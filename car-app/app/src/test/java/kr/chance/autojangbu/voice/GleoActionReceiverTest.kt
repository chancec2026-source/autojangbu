package kr.chance.autojangbu.voice

import kr.chance.autojangbu.data.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GleoActionReceiverTest {

    @Test
    fun `구분은 영어든 한국어든 알아듣는다`() {
        assertEquals(Category.BUSINESS, GleoActionReceiver.parseCategory("business"))
        assertEquals(Category.BUSINESS, GleoActionReceiver.parseCategory("업무"))
        assertEquals(Category.BUSINESS, GleoActionReceiver.parseCategory("거래처 미팅"))
        assertEquals(Category.COMMUTE, GleoActionReceiver.parseCategory("Commute"))
        assertEquals(Category.COMMUTE, GleoActionReceiver.parseCategory("퇴근길"))
        assertEquals(Category.PERSONAL, GleoActionReceiver.parseCategory("개인 일정"))
        assertNull(GleoActionReceiver.parseCategory("음"))
        assertNull(GleoActionReceiver.parseCategory(""))
    }

    @Test
    fun `파라미터는 actionParameters 안이든 바로든 읽는다`() {
        val wrapped = """{"packageName":"kr.chance.autojangbu","action":"CLASSIFY_LAST_TRIP","actionParameters":{"category":"business","note":"○○현장"}}"""
        assertEquals("○○현장", GleoActionReceiver.parseParams(wrapped).getString("note"))
        assertEquals("business", GleoActionReceiver.parseParams("""{"category":"business"}""").getString("category"))
        assertEquals(0, GleoActionReceiver.parseParams(null).length())
        assertEquals(0, GleoActionReceiver.parseParams("깨진 값").length())
    }
}
