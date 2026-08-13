package dev.rclaude.android.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rclaude.android.data.SettingsRepository
import dev.rclaude.protocol.ConnectionLink
import dev.rclaude.protocol.net.RemoteClaudeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние экрана подключения. */
data class ConnectUiState(
    val link: String = "",
    val token: String = "",
    val checking: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val saved: Boolean = false,
) {
    /** Кнопка сохранения доступна, когда есть и адрес, и токен. */
    val canSave: Boolean get() = link.isNotBlank() && token.isNotBlank() && !checking
}

/** Ввод ссылки подключения, проверка сервера и сохранение адреса с токеном. */
class ConnectViewModel(
    private val api: RemoteClaudeApi,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectUiState())
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = settings.current() ?: return@launch
            _state.update { it.copy(link = stored.address.httpBase, token = stored.token) }
        }
    }

    /** Ссылка изменилась: токен из фрагмента подставляется сам. */
    fun onLinkChanged(text: String) {
        val token = ConnectionLink.parse(text).getOrNull()?.token
        _state.update { current ->
            current.copy(link = text, token = token ?: current.token, message = null)
        }
    }

    fun onTokenChanged(text: String) {
        _state.update { it.copy(token = text, message = null) }
    }

    /** Результат сканирования QR — та же ссылка, что вводится руками. */
    fun onScanned(text: String) {
        onLinkChanged(text)
    }

    /** Проверяет `GET /api/health`. */
    fun check() {
        val parsed = ConnectionLink.parse(_state.value.link)
        val address = parsed.getOrNull()?.address ?: run {
            showMessage(parsed.exceptionOrNull()?.message ?: "ссылку не разобрать", isError = true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = null) }
            val result = runCatching { api.health(address) }
            _state.update { current ->
                result.fold(
                    onSuccess = { health ->
                        current.copy(
                            checking = false,
                            message = "Сервер на связи, версия ${health.version ?: "неизвестна"}",
                            messageIsError = false,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            checking = false,
                            message = error.message ?: "сервер не ответил",
                            messageIsError = true,
                        )
                    },
                )
            }
        }
    }

    /** Сохраняет подключение; при успехе поднимает флаг [ConnectUiState.saved]. */
    fun save() {
        val parsed = ConnectionLink.parse(_state.value.link)
        val address = parsed.getOrNull()?.address ?: run {
            showMessage(parsed.exceptionOrNull()?.message ?: "ссылку не разобрать", isError = true)
            return
        }
        val token = _state.value.token.trim()
        if (token.isEmpty()) {
            showMessage("нет токена: отсканируй QR из rclaude qr или впиши токен", isError = true)
            return
        }
        viewModelScope.launch {
            settings.save(address, token)
            _state.update { it.copy(saved = true, message = null) }
        }
    }

    /** Экран увёл пользователя дальше — флаг сохранения снимается. */
    fun onSavedHandled() {
        _state.update { it.copy(saved = false) }
    }

    private fun showMessage(text: String, isError: Boolean) {
        _state.update { it.copy(message = text, messageIsError = isError) }
    }
}
