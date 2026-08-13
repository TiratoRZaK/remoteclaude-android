package dev.rclaude.android.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppStyleTest {

    @Test
    fun `оформление находится по сохранённому идентификатору`() {
        assertEquals(AppStyle.SYNTHWAVE, AppStyle.fromId("synthwave"))
        assertEquals(AppStyle.CHRONICLE, AppStyle.fromId("chronicle"))
    }

    @Test
    fun `неизвестный и пустой идентификатор дают оформление по умолчанию`() {
        assertEquals(AppStyle.DEFAULT, AppStyle.fromId(null))
        assertEquals(AppStyle.DEFAULT, AppStyle.fromId(""))
        assertEquals(AppStyle.DEFAULT, AppStyle.fromId("удалённый-стиль"))
    }

    @Test
    fun `идентификаторы уникальны, названия заполнены`() {
        val ids = AppStyle.entries.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(AppStyle.entries.all { it.title.isNotBlank() && it.tagline.isNotBlank() })
        assertTrue(AppStyle.entries.all { it.id.matches(Regex("[a-z-]+")) })
    }

    @Test
    fun `оформлений несколько и они перечислены в галерее`() {
        assertTrue(AppStyle.entries.size >= 5)
    }
}
