package com.stripe.android.identity.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class DefaultIdentityIOTest {
    @Test
    fun `createTestModeFileToUpload creates document front placeholder`() = runScenario(
        image = TestModeImage.DOCUMENT_FRONT,
        assetPath = "stripe_identity_test_mode/document_front.png"
    ) {
        assertThat(file.name).isEqualTo("stripe_identity_test_mode_document_front.png")
        assertThat(file.readBytes()).isEqualTo(assetBytes)
    }

    @Test
    fun `createTestModeFileToUpload creates document back placeholder`() = runScenario(
        image = TestModeImage.DOCUMENT_BACK,
        assetPath = "stripe_identity_test_mode/document_back.png"
    ) {
        assertThat(file.name).isEqualTo("stripe_identity_test_mode_document_back.png")
        assertThat(file.readBytes()).isEqualTo(assetBytes)
    }

    @Test
    fun `createTestModeFileToUpload creates selfie placeholder`() = runScenario(
        image = TestModeImage.SELFIE,
        assetPath = "stripe_identity_test_mode/selfie.png"
    ) {
        assertThat(file.name).isEqualTo("stripe_identity_test_mode_selfie.png")
        assertThat(file.readBytes()).isEqualTo(assetBytes)
    }

    private fun runScenario(
        image: TestModeImage,
        assetPath: String,
        block: Scenario.() -> Unit
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetBytes = context.assets.open(assetPath).use { it.readBytes() }
        val file = DefaultIdentityIO(context).createTestModeFileToUpload(image)

        Scenario(
            file = file,
            assetBytes = assetBytes
        ).apply(block)
    }

    private data class Scenario(
        val file: File,
        val assetBytes: ByteArray
    )
}
