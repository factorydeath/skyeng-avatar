package ru.skypro.avatar.student.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionPersistence(private val context: Context) {
    private var prefs: SharedPreferences? = null
    var recovered: Boolean = false
        private set
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun initialize() {
        if (prefs != null) return
        for (name in listOf("avatar_cookies", "avatar_session")) {
            val legacy = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            if (legacy.all.isNotEmpty()) {
                recovered = true
                if (!legacy.edit().clear().commit()) throw IOException("Session migration failed")
            }
        }
        val opened = try {
            open().also { it.all }
        } catch (_: GeneralSecurityException) {
            recover()
        } catch (_: IOException) {
            recover()
        } catch (_: SecurityException) {
            recover()
        }
        try {
            json.decodeFromString<List<StoredCookie>>(opened.getString("cookies", "[]") ?: "[]")
            opened.getBoolean("loggedIn", false)
        } catch (_: SerializationException) {
            recovered = true
            commit(opened.edit().clear())
        } catch (_: ClassCastException) {
            recovered = true
            commit(opened.edit().clear())
        }
        prefs = opened
    }

    private fun open(): SharedPreferences {
        val key = MasterKey.Builder(context, KEY_ALIAS).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context, FILE_NAME, key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun recover(): SharedPreferences {
        recovered = true
        if (!context.deleteSharedPreferences(FILE_NAME)) throw IOException("Session recovery failed")
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(KEY_ALIAS) }
        return open().also { it.all }
    }

    @Synchronized
    fun readCookies(): List<StoredCookie> = json.decodeFromString(requireNotNull(prefs).getString("cookies", "[]") ?: "[]")

    @Synchronized
    fun writeCookies(cookies: List<StoredCookie>) {
        commit(requireNotNull(prefs).edit().putString("cookies", json.encodeToString(cookies)))
    }

    @Synchronized
    fun loggedIn(): Boolean = requireNotNull(prefs).getBoolean("loggedIn", false)

    @Synchronized
    fun markLoggedIn() {
        commit(requireNotNull(prefs).edit().putBoolean("loggedIn", true))
    }

    @Synchronized
    fun clear() {
        commit(requireNotNull(prefs).edit().clear())
    }

    private fun commit(editor: SharedPreferences.Editor) {
        if (!editor.commit()) throw IOException("Session storage write failed")
    }

    private companion object {
        const val FILE_NAME = "avatar_session_v2"
        const val KEY_ALIAS = "avatar_session_key_v2"
    }
}
