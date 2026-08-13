package dev.rclaude.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rclaude.android.ui.RemoteClaudeApp
import dev.rclaude.android.ui.theme.AppStyle
import dev.rclaude.android.ui.theme.RemoteClaudeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RemoteClaudeApplication).container
        setContent {
            val styleId by container.settings.styleId.collectAsStateWithLifecycle(initialValue = null)
            RemoteClaudeTheme(style = AppStyle.fromId(styleId)) {
                RemoteClaudeApp(container)
            }
        }
    }
}
