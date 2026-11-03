package com.stripe.android.stripecardscan.payment.ml

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.framework.FetchedResource
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
internal class SSDOcrModelSelectionTest {
    @Test
    fun `default configuration loads original bundled artifact`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val model = CardScanConfiguration(elementsSessionId = null).ssdOcrModel
        val resource = SSDOcrModelManager(model).fetchModel(context, true) as FetchedResource

        assertThat(model).isEqualTo(SSDOcrModel.Original)
        assertThat(resource.assetFileName).isEqualTo("darknite_1_1_1_16.tflite")
        assertThat(model.numberOfPriors).isEqualTo(3420)
        assertArtifact(context, resource, 1659924)
    }

    @Test
    fun `compact configuration loads compact bundled artifact`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val model = CardScanConfiguration(elementsSessionId = null, useCompactSsdOcr = true).ssdOcrModel
        val resource = SSDOcrModelManager(model).fetchModel(context, true) as FetchedResource

        assertThat(model).isEqualTo(SSDOcrModel.Compact)
        assertThat(resource.assetFileName).isEqualTo("embossed_fine_w8.tflite")
        assertThat(model.numberOfPriors).isEqualTo(2736)
        assertArtifact(context, resource, 235816)
    }

    @Test
    fun `sequential scans retain independent model assignments`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = SSDOcrModelManager(SSDOcrModel.Original)
        val compact = SSDOcrModelManager(SSDOcrModel.Compact)
        val first = original.fetchModel(context, true) as FetchedResource
        val second = compact.fetchModel(context, true) as FetchedResource
        val third = original.fetchModel(context, true) as FetchedResource

        assertThat(first.assetFileName).isEqualTo("darknite_1_1_1_16.tflite")
        assertThat(second.assetFileName).isEqualTo("embossed_fine_w8.tflite")
        assertThat(third).isEqualTo(first)
    }

    private fun assertArtifact(context: Context, resource: FetchedResource, expectedSize: Int) {
        val bytes = context.assets.open(requireNotNull(resource.assetFileName)).use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertThat(bytes.size).isEqualTo(expectedSize)
        assertThat(hash).isEqualTo(resource.modelHash)
    }
}
