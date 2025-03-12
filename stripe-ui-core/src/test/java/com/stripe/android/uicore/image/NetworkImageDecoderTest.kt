package com.stripe.android.uicore.image

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class NetworkImageDecoderTest {
    private val mockWebServer = MockWebServer()

    @Before
    @Throws(IOException::class)
    fun setUp() {
        mockWebServer.start()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `when image is png, content type should be Png`() = testContentType(
        filename = "example.png",
        expectedContentType = LoadedImage.ContentType.Png,
    )

    @Test
    fun `when image is jpg, content type should be Jpeg`() = testContentType(
        filename = "example.jpg",
        expectedContentType = LoadedImage.ContentType.Jpeg,
    )

    @Test
    fun `when image is gif, content type should be Unknown`() = testContentType(
        filename = "example.gif",
        expectedContentType = LoadedImage.ContentType.Unknown(value = "image/gif"),
    )

    private fun testContentType(
        filename: String,
        expectedContentType: LoadedImage.ContentType
    ) = runTest {
        enqueueImage(filename)

        val decoder = NetworkImageDecoder()

        val image = decoder.decode(mockWebServer.url("image").toUrl())

        assertThat(image).isNotNull()

        val nonNullImage = requireNotNull(image)

        assertThat(nonNullImage.contentType).isEqualTo(expectedContentType)
    }

    private fun enqueueImage(filename: String) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .readImage(imageName = filename)
        )
    }

    private fun MockResponse.readImage(imageName: String) = apply {
        val inputStream = MockResponse::class.java.classLoader!!.getResourceAsStream(imageName)
        val buffer = Buffer()
        buffer.readFrom(inputStream)
        setBody(buffer)
    }
}
