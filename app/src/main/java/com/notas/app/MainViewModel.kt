package com.notas.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notas.app.backup.BackupManager
import com.notas.app.data.AppDb
import com.notas.app.data.Category
import com.notas.app.data.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDb.get(app)
    private val noteDao = db.noteDao()
    private val catDao = db.categoryDao()

    val notes: StateFlow<List<Note>> =
        noteDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> =
        catDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Categoria selecionada no filtro (null = todas) */
    val filter = MutableStateFlow<Long?>(null)
    val query = MutableStateFlow("")

    init {
        // Limpa notas vazias que ficaram para trás (ex.: app fechado no meio da criação)
        viewModelScope.launch { noteDao.deleteBlank() }
    }

    suspend fun createBlank(): Long = noteDao.insert(Note(categoryId = filter.value))

    /** Cria uma nota a partir de texto (foto/print lido, ou texto compartilhado de outro app). */
    suspend fun createFromText(text: String): Long {
        val clean = text.trim()
        val lines = clean.lines()
        val first = lines.first().trim()
        val hasTitle = lines.size > 1 && first.length <= 60
        val note = Note(
            title = if (hasTitle) first else "",
            content = if (hasTitle) lines.drop(1).joinToString("\n").trim() else clean,
            categoryId = filter.value
        )
        return noteDao.insert(note)
    }

    suspend fun getNote(id: Long): Note? = noteDao.get(id)

    fun update(note: Note) {
        viewModelScope.launch { noteDao.update(note.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun delete(ids: Collection<Long>) {
        viewModelScope.launch { noteDao.deleteByIds(ids.toList()) }
    }

    fun move(ids: Collection<Long>, categoryId: Long?) {
        viewModelScope.launch { noteDao.moveToCategory(ids.toList(), categoryId) }
    }

    fun addCategory(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch {
            catDao.insert(Category(name = n, colorIndex = categories.value.size % 8))
        }
    }

    fun renameCategory(c: Category, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch { catDao.update(c.copy(name = n)) }
    }

    fun recolor(c: Category) {
        viewModelScope.launch { catDao.update(c.copy(colorIndex = (c.colorIndex + 1) % 8)) }
    }

    fun deleteCategory(c: Category) {
        viewModelScope.launch {
            noteDao.clearCategory(c.id)
            catDao.delete(c)
            if (filter.value == c.id) filter.value = null
        }
    }

    suspend fun restore(json: String): Int = BackupManager.restore(getApplication(), json)
}
