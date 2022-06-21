package com.stripe.android.stripecardscan.framework

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoaderTest {
    private val testContext = InstrumentationRegistry.getInstrumentation().context

    @Test
    @SmallTest
    fun loadData_fromResource_success() = runBlocking {
        val fetchedData = FetchedResource(
            modelClass = "sample_class",
            modelFrameworkVersion = 2049,
            modelVersion = "sample_resource",
            modelHash = "0dcf3e387c68dfea8dd72a183f1f765478ebaa4d8544cfc09a16e87a795d8ccf",
            modelHashAlgorithm = "SHA-256",
            assetFileName = "sample_resource.tflite"
        )

        val byteBuffer = Loader(testContext).loadData(fetchedData)
        assertNotNull(byteBuffer)
        assertEquals(14, byteBuffer.limit(), "File is not expected size")
        byteBuffer.rewind()

        // ensure not all bytes are zero
        var encounteredNonZeroByte = false
        while (!encounteredNonZeroByte) {
            encounteredNonZeroByte = byteBuffer.get().toInt() != 0
        }
        assertTrue(encounteredNonZeroByte, "All bytes were zero")

        // ensure bytes are correct
        byteBuffer.rewind()
        assertEquals('A', byteBuffer.get().toInt().toChar())
        assertEquals('B', byteBuffer.get().toInt().toChar())
        assertEquals('C', byteBuffer.get().toInt().toChar())
        assertEquals('1', byteBuffer.get().toInt().toChar())
    }

    @Test
    @SmallTest
    fun loadData_fromFile_success() = runBlocking {
        val sampleFile = File(testContext.cacheDir, "sample_file")
        if (sampleFile.exists()) {
            sampleFile.delete()
        }

        sampleFile.createNewFile()
        sampleFile.writeText("ABC123")

        val fetchedData = FetchedFile(
            modelClass = "sample_class",
            modelFrameworkVersion = 2049,
            modelVersion = "sample_file",
            modelHash = "133351546614bfadfa68bb66c22a06265972b02791e4ac545ad900f20fe1a796",
            modelHashAlgorithm = "SHA-256",
            file = sampleFile
        )

        val byteBuffer = Loader(testContext).loadData(fetchedData)
        assertNotNull(byteBuffer)
        assertEquals(6, byteBuffer.limit(), "File is not expected size")
        byteBuffer.rewind()

        // ensure not all bytes are zero
        var encounteredNonZeroByte = false
        while (!encounteredNonZeroByte) {
            encounteredNonZeroByte = byteBuffer.get().toInt() != 0
        }
        assertTrue(encounteredNonZeroByte, "All bytes were zero")

        // ensure bytes are correct
        byteBuffer.rewind()
        assertEquals('A', byteBuffer.get().toInt().toChar())
        assertEquals('B', byteBuffer.get().toInt().toChar())
        assertEquals('C', byteBuffer.get().toInt().toChar())
        assertEquals('1', byteBuffer.get().toInt().toChar())
    }
}
