package dev.rclaude.android

import android.app.Application
import dev.rclaude.android.di.AppContainer

/** Точка сборки зависимостей приложения. */
class RemoteClaudeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
