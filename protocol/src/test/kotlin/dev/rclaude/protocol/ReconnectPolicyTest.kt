package dev.rclaude.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class ReconnectPolicyTest {

    @Test
    fun `пауза удваивается до потолка`() {
        val policy = ReconnectPolicy()

        val delays = List(7) { policy.nextDelayMs() }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 15_000L, 15_000L), delays)
    }

    @Test
    fun `удачное подключение возвращает паузу к началу`() {
        val policy = ReconnectPolicy()
        repeat(4) { policy.nextDelayMs() }

        policy.reset()

        assertEquals(1_000L, policy.currentDelayMs)
        assertEquals(1_000L, policy.nextDelayMs())
    }

    @Test
    fun `параметры паузы настраиваются`() {
        val policy = ReconnectPolicy(initialDelayMs = 200, maxDelayMs = 1_000, factor = 3)

        assertEquals(listOf(200L, 600L, 1_000L), List(3) { policy.nextDelayMs() })
    }
}
