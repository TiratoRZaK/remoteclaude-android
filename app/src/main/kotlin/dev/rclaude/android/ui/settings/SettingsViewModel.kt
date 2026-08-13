package dev.rclaude.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rclaude.android.data.SettingsRepository
import dev.rclaude.android.ui.theme.AppStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Состояние экрана настроек. */
data class SettingsUiState(
    val server: String? = null,
    val style: AppStyle = AppStyle.DEFAULT,
)

/** Настройки: текущее подключение и выбор оформления. */
class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(settings.connection, settings.styleId) { connection, styleId ->
            SettingsUiState(
                server = connection?.address?.httpBase,
                style = AppStyle.fromId(styleId),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(),
        )

    /** Запоминает выбранное оформление — тема меняется сразу. */
    fun choose(style: AppStyle) {
        viewModelScope.launch { settings.saveStyle(style.id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
