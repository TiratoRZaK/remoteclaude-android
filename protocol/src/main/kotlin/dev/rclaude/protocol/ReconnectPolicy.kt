package dev.rclaude.protocol

/**
 * Нарастающая пауза переподключения: первая пауза [initialDelayMs], каждая следующая
 * умножается на [factor] до потолка [maxDelayMs]. Удачное подключение вызывает [reset].
 */
class ReconnectPolicy(
    private val initialDelayMs: Long = 1_000,
    private val maxDelayMs: Long = 15_000,
    private val factor: Int = 2,
) {
    init {
        require(initialDelayMs > 0) { "начальная пауза должна быть положительной" }
        require(maxDelayMs >= initialDelayMs) { "потолок паузы меньше начальной паузы" }
        require(factor >= 1) { "множитель паузы должен быть не меньше единицы" }
    }

    /** Пауза, которую вернёт следующий [nextDelayMs]. */
    var currentDelayMs: Long = initialDelayMs
        private set

    /** Отдаёт текущую паузу и увеличивает следующую. */
    fun nextDelayMs(): Long {
        val delay = currentDelayMs
        currentDelayMs = minOf(delay * factor, maxDelayMs)
        return delay
    }

    /** Возврат к начальной паузе после удачного подключения. */
    fun reset() {
        currentDelayMs = initialDelayMs
    }
}
