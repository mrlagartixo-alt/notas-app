package com.notas.app.backup

import android.content.Context
import androidx.room.withTransaction
import com.notas.app.data.AppDb
import com.notas.app.data.Category
import com.notas.app.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.mail.AuthenticationFailedException

object BackupManager {

    /** Monta o backup e manda por e-mail. Atualiza o "último status" nas configurações. */
    suspend fun sendBackup(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val prefs = BackupPrefs(context)
        val result = runCatching {
            require(prefs.isConfigured) { "Preencha os e-mails e a senha de app primeiro." }
            val db = AppDb.get(context)
            val notes = db.noteDao().getAll()
            val cats = db.categoryDao().getAll()

            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val human = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date())

            Mailer.send(
                user = prefs.senderEmail,
                password = prefs.appPassword,
                to = prefs.destEmail,
                subject = "Backup Anota · ${notes.size} notas · $human",
                body = "Backup automático do Anota.\n\n" +
                    "${notes.size} notas e ${cats.size} categorias.\n\n" +
                    "• O arquivo .json serve para restaurar no app (Ajustes → Restaurar).\n" +
                    "• O arquivo .txt é a versão para ler em qualquer lugar.\n",
                attachments = listOf(
                    Mailer.Attachment("anota-backup-$stamp.json", "application/json", buildJson(notes, cats).toByteArray(Charsets.UTF_8)),
                    Mailer.Attachment("anota-notas-$stamp.txt", "text/plain; charset=UTF-8", buildText(notes, cats).toByteArray(Charsets.UTF_8))
                )
            )
        }
        prefs.lastTime = System.currentTimeMillis()
        prefs.lastStatus = if (result.isSuccess) "ok" else friendlyError(result.exceptionOrNull()!!)
        result
    }

    fun friendlyError(e: Throwable): String {
        val chain = generateSequence(e) { it.cause }.toList()
        return when {
            chain.any { it is AuthenticationFailedException } ->
                "O Gmail recusou o login. Confira o e-mail remetente e use uma SENHA DE APP (não a senha normal da conta)."
            chain.any { it is IOException } ->
                "Sem conexão com a internet (ou o Gmail não respondeu). Tente de novo."
            else -> e.message ?: e.javaClass.simpleName
        }
    }

    fun buildJson(notes: List<Note>, cats: List<Category>): String {
        val catName = cats.associate { it.id to it.name }
        val root = JSONObject()
        root.put("app", "anota")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val catArr = JSONArray()
        for (c in cats) {
            catArr.put(JSONObject().put("name", c.name).put("colorIndex", c.colorIndex))
        }
        root.put("categories", catArr)

        val noteArr = JSONArray()
        for (n in notes) {
            val catLabel: Any = n.categoryId?.let { catName[it] } ?: JSONObject.NULL
            noteArr.put(
                JSONObject()
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("category", catLabel)
                    .put("pinned", n.pinned)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
            )
        }
        root.put("notes", noteArr)
        return root.toString(2)
    }

    fun buildText(notes: List<Note>, cats: List<Category>): String {
        val catName = cats.associate { it.id to it.name }
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        return buildString {
            appendLine("ANOTA — ${notes.size} notas")
            appendLine("Exportado em ${fmt.format(Date())}")
            appendLine()
            for (n in notes) {
                appendLine("════════════════════════════")
                if (n.title.isNotBlank()) appendLine(n.title.trim())
                val cat = n.categoryId?.let { catName[it] }
                appendLine(listOfNotNull(cat, fmt.format(Date(n.updatedAt))).joinToString(" · "))
                appendLine()
                appendLine(n.content.trim())
                appendLine()
            }
        }
    }

    /** Importa um backup .json. Não duplica notas que já existem. Devolve quantas foram adicionadas. */
    suspend fun restore(context: Context, json: String): Int {
        val root = JSONObject(json)
        require(root.optString("app") == "anota") { "Este arquivo não é um backup do Anota." }
        val db = AppDb.get(context)
        return db.withTransaction {
            val catDao = db.categoryDao()
            val noteDao = db.noteDao()

            val idByName = catDao.getAll().associate { it.name.lowercase() to it.id }.toMutableMap()
            val cats = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until cats.length()) {
                val o = cats.getJSONObject(i)
                val name = o.getString("name")
                if (name.lowercase() !in idByName) {
                    idByName[name.lowercase()] = catDao.insert(Category(name = name, colorIndex = o.optInt("colorIndex", 0)))
                }
            }

            val existing = noteDao.getAll().map { "${it.title}|${it.content}|${it.createdAt}" }.toMutableSet()
            var added = 0
            val arr = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val title = o.optString("title", "")
                val content = o.optString("content", "")
                val created = o.optLong("createdAt", System.currentTimeMillis())
                val key = "$title|$content|$created"
                if (key in existing) continue
                val catId = if (o.isNull("category")) null else idByName[o.getString("category").lowercase()]
                noteDao.insert(
                    Note(
                        title = title,
                        content = content,
                        categoryId = catId,
                        pinned = o.optBoolean("pinned", false),
                        createdAt = created,
                        updatedAt = o.optLong("updatedAt", created)
                    )
                )
                existing.add(key)
                added++
            }
            added
        }
    }
}
