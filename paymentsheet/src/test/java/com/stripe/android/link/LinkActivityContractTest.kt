package com.stripe.android.link

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkActivityContractTest {

    private val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkEnabled,
        isEnabled = false
    )

    @Test
    fun `LinkActivityContract creates intent with WebLinkActivityContract when native link disabled`() {
        featureFlagTestRule.setEnabled(false)

        val context: Context = ApplicationProvider.getApplicationContext()
        val expectedIntent = Intent()
        val webLinkActivityContract = mock<WebLinkActivityContract>()
        whenever(webLinkActivityContract.createIntent(context, args))
            .thenReturn(expectedIntent)

        val contract = linkActivityContract(webLinkActivityContract = webLinkActivityContract)

        val actualIntent = contract.createIntent(context, args)

        verify(webLinkActivityContract).createIntent(eq(context), eq(args))
        assertThat(expectedIntent).isEqualTo(actualIntent)
    }

    @Test
    fun `LinkActivityContract parses result with WebLinkActivityContract when native link disabled`() {
        featureFlagTestRule.setEnabled(false)

        val context: Context = ApplicationProvider.getApplicationContext()
        val expectedIntent = Intent()
        val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)
        val webLinkActivityContract = mock<WebLinkActivityContract>()

        whenever(webLinkActivityContract.createIntent(context, args))
            .thenReturn(expectedIntent)

        val contract = linkActivityContract(webLinkActivityContract = webLinkActivityContract)

        val intent = contract.createIntent(context, args)
        intent.data = Uri.EMPTY

        contract.parseResult(0, intent)

        verify(webLinkActivityContract).parseResult(0, intent)
    }

    @Test
    fun `LinkActivityContract creates intent with with NativeLinkActivityContract when native link is enabled`() {
        featureFlagTestRule.setEnabled(true)

        val context: Context = ApplicationProvider.getApplicationContext()
        val expectedIntent = Intent()
        val nativeLinkActivityContract = mock<NativeLinkActivityContract>()
        whenever(nativeLinkActivityContract.createIntent(context, args))
            .thenReturn(expectedIntent)

        val contract = linkActivityContract(nativeLinkActivityContract = nativeLinkActivityContract)

        val actualIntent = contract.createIntent(context, args)

        verify(nativeLinkActivityContract).createIntent(context, args)
        assertThat(expectedIntent).isEqualTo(actualIntent)
    }

    @Test
    fun `LinkActivityContract parses result with NativeLinkActivityContract when native link is enabled`() {
        featureFlagTestRule.setEnabled(true)

        val context: Context = ApplicationProvider.getApplicationContext()
        val expectedIntent = Intent()
        val expectedResult = LinkActivityResult.PaymentMethodObtained(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val nativeLinkActivityContract = mock<NativeLinkActivityContract>()

        whenever(nativeLinkActivityContract.createIntent(context, args))
            .thenReturn(expectedIntent)
        whenever(nativeLinkActivityContract.parseResult(0, expectedIntent))
            .thenReturn(expectedResult)

        val contract = linkActivityContract(nativeLinkActivityContract = nativeLinkActivityContract)

        val intent = contract.createIntent(context, args)
        contract.parseResult(0, intent)

        verify(nativeLinkActivityContract).parseResult(0, intent)
    }

    private fun linkActivityContract(
        webLinkActivityContract: WebLinkActivityContract = mock(),
        nativeLinkActivityContract: NativeLinkActivityContract = mock()
    ): LinkActivityContract {
        return LinkActivityContract(
            nativeLinkActivityContract = nativeLinkActivityContract,
            webLinkActivityContract = webLinkActivityContract
        )
    }
}
