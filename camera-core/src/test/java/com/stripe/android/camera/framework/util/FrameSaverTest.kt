package com.stripe.android.camera.framework.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class FrameSaverTest {

    private class TestFrameSaver : FrameSaver<String, Int, Int>() {
        override fun getMaxSavedFrames(savedFrameIdentifier: String): Int = 3

        override fun getSaveFrameIdentifier(frame: Int, metaData: Int): String? = when (metaData) {
            1 -> "one"
            2 -> "two"
            else -> "else"
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun saveFrames() = runTest {
        val frameSaver = TestFrameSaver()

        frameSaver.saveFrame(1, 1)
        frameSaver.saveFrame(1, 1)
        frameSaver.saveFrame(1, 1)
        frameSaver.saveFrame(1, 1)
        frameSaver.saveFrame(2, 2)
        frameSaver.saveFrame(3, 3)
        frameSaver.saveFrame(4, 4)

        assertEquals(
            listOf(1, 1, 1),
            frameSaver.getSavedFrames()["one"]?.toList()
        )

        assertEquals(
            listOf(2),
            frameSaver.getSavedFrames()["two"]?.toList()
        )

        assertEquals(
            listOf(4, 3),
            frameSaver.getSavedFrames()["else"]?.toList()
        )
    }

    @Test
    @ExperimentalCoroutinesApi
    fun doesNotLeakInternalMap() = runTest {
        val frameSaver = TestFrameSaver()

        frameSaver.saveFrame(1, 1)

        val savedFrames = frameSaver.getSavedFrames()

        frameSaver.reset()

        assertEquals(1, savedFrames["one"]?.first)
    }
}
