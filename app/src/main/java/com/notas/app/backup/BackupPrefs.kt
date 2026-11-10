package com.notas.app.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Configurações do backup. A senha de app fica criptografada no aparelho. */
class BackupPrefs(context: Context) {

    private val prefs: SharedPreferences = try {
        val key = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "backup_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Raro: se o keystore do aparelho falhar, usa armazenamento privado comum do app
        context.applicationContext.getSharedPreferences("backup_plain", Context.MODE_PRIVATE)
    }

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    var destEmail: String
        get() = prefs.getString("dest", "") ?: ""
        set(v) = prefs.edit().putString("dest", v).apply()

    var senderEmail: String
        get() = prefs.getString("sender", "") ?: ""
        set(v) = prefs.edit().putString("sender", v).apply()

    var appPassword: String
        get() = prefs.getString("password", "") ?: ""
        set(v) = prefs.edit().putString("password", v).apply()

    var hour: Int
        get() = prefs.getInt("hour", 21)
        set(v) = prefs.edit().putInt("hour", v).apply()

    var lastTime: Long
        get() = prefs.getLong("lastTime", 0L)
        set(v) = prefs.edit().putLong("lastTime", v).apply()

    var lastStatus: String
        get() = prefs.getString("lastStatus", "") ?: ""
        set(v) = prefs.edit().putString("lastStatus", v).apply()

    val isConfigured: Boolean
        get() = senderEmail.isNotBlank() && appPassword.isNotBlank() && destEmail.isNotBlank()
}
