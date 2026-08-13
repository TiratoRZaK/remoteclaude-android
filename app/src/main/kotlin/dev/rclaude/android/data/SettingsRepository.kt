package dev.rclaude.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.rclaude.protocol.ConnectionLink
import dev.rclaude.protocol.ServerAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Хранилище подключения. */
val Context.connectionDataStore: DataStore<Preferences> by preferencesDataStore(name = "connection")

/** Сохранённое подключение: адрес сервера и токен доступа. */
data class StoredConnection(val address: ServerAddress, val token: String)

/** Адрес сервера и токен в DataStore. */
class SettingsRepository(private val store: DataStore<Preferences>) {

    /** Поток сохранённого подключения; `null` — подключение ещё не настроено. */
    val connection: Flow<StoredConnection?> = store.data.map(::read)

    /** Идентификатор выбранного оформления; `null` — оформление не выбирали. */
    val styleId: Flow<String?> = store.data.map { preferences -> preferences[STYLE] }

    /** Текущее сохранённое подключение. */
    suspend fun current(): StoredConnection? = read(store.data.first())

    /** Запоминает выбранное оформление. */
    suspend fun saveStyle(id: String) {
        store.edit { preferences -> preferences[STYLE] = id }
    }

    /** Запоминает адрес и токен. */
    suspend fun save(address: ServerAddress, token: String) {
        store.edit { preferences ->
            preferences[ADDRESS] = address.httpBase
            preferences[TOKEN] = token
        }
    }

    private fun read(preferences: Preferences): StoredConnection? {
        val address = preferences[ADDRESS]
            ?.let { ConnectionLink.parse(it).getOrNull()?.address }
            ?: return null
        val token = preferences[TOKEN]?.takeIf { it.isNotBlank() } ?: return null
        return StoredConnection(address, token)
    }

    private companion object {
        val ADDRESS = stringPreferencesKey("address")
        val TOKEN = stringPreferencesKey("token")
        val STYLE = stringPreferencesKey("style")
    }
}
