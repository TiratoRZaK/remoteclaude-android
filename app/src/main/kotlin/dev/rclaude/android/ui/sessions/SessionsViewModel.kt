package dev.rclaude.android.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rclaude.android.data.SettingsRepository
import dev.rclaude.protocol.SessionInfo
import dev.rclaude.protocol.net.RemoteClaudeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние экрана списка сессий. */
data class SessionsUiState(
    val loading: Boolean = true,
    val server: String = "",
    val sessions: List<SessionInfo> = emptyList(),
    val note: String? = null,
    val error: String? = null,
)

/** Список живых сессий сервера. */
class SessionsViewModel(
    private val api: RemoteClaudeApi,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Перечитывает список сессий. */
    fun refresh() {
        viewModelScope.launch {
            val stored = settings.current()
            if (stored == null) {
                _state.value = SessionsUiState(
                    loading = false,
                    error = "Подключение не настроено — вставь ссылку из rclaude qr.",
                )
                return@launch
            }
            _state.update { it.copy(loading = true, error = null, server = stored.address.httpBase) }
            runCatching { api.sessions(stored.address, stored.token) }
                .onSuccess { sessions ->
                    _state.update { current ->
                        current.copy(
                            loading = false,
                            sessions = sessions,
                            note = if (sessions.isEmpty()) {
                                "Живых сессий нет. Запусти rclaude во вкладке IDE."
                            } else {
                                null
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, error = error.message ?: "не удалось получить список сессий")
                    }
                }
        }
    }
}
