package dev.rclaude.android.di

import android.content.Context
import dev.rclaude.android.data.SettingsRepository
import dev.rclaude.android.data.connectionDataStore
import dev.rclaude.protocol.net.OkHttpSessionSocketFactory
import dev.rclaude.protocol.net.RemoteClaudeApi
import dev.rclaude.protocol.net.SessionSocketFactory

/** Зависимости приложения: один HTTP-клиент, один репозиторий настроек. */
class AppContainer(context: Context) {

    private val httpClient = RemoteClaudeApi.defaultClient()

    val api: RemoteClaudeApi = RemoteClaudeApi(httpClient)

    val socketFactory: SessionSocketFactory =
        OkHttpSessionSocketFactory(RemoteClaudeApi.socketClient(httpClient))

    val settings: SettingsRepository = SettingsRepository(context.connectionDataStore)
}
