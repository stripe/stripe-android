package com.stripe.android.link

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
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

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkEnabled,
        isEnabled = false
    )

    @Test
    fun `creates intent with WebLinkActivityContract when native link disabled`() {
        featureFlagTestRule.setEnabled(false)

        val (webLinkActivityContract, expectedIntent) = mockWebLinkContract()

        val contract = linkActivityContract(webLinkActivityContract = webLinkActivityContract)

        val actualIntent = contract.createIntent(context, args)

        verify(webLinkActivityContract).createIntent(eq(context), eq(args))
        assertThat(expectedIntent).isEqualTo(actualIntent)
    }

    @Test
    fun `parses result with WebLinkActivityContract when result code is not LinkActivity_RESULT_COMPLETE`() {
        val (webLinkActivityContract, _) = mockWebLinkContract()
        val resultIntent = Intent().apply {
            data = Uri.parse("https://stripe.com")
        }

        val contract = linkActivityContract(webLinkActivityContract = webLinkActivityContract)

        contract.parseResult(0, resultIntent)

        verify(webLinkActivityContract).parseResult(0, resultIntent)
    }

    @Test
    fun `LinkActivityContract creates intent with with NativeLinkActivityContract when native link is enabled`() {
        featureFlagTestRule.setEnabled(true)

        val (nativeLinkActivityContract, expectedIntent) = mockNativeLinkContract()

        val contract = linkActivityContract(nativeLinkActivityContract = nativeLinkActivityContract)

        val actualIntent = contract.createIntent(context, args)

        verify(nativeLinkActivityContract).createIntent(context, args)
        assertThat(expectedIntent).isEqualTo(actualIntent)
    }

    @Test
    fun `parses result with NativeLinkActivityContract when result code is LinkActivity_RESULT_COMPLETE`() {
        val resultIntent = Intent()
        val (nativeLinkActivityContract, _) = mockNativeLinkContract()

        val contract = linkActivityContract(nativeLinkActivityContract = nativeLinkActivityContract)

        contract.parseResult(LinkActivity.RESULT_COMPLETE, resultIntent)

        verify(nativeLinkActivityContract).parseResult(eq(LinkActivity.RESULT_COMPLETE), eq(resultIntent))
    }

    private fun mockWebLinkContract(): Pair<WebLinkActivityContract, Intent> {
        val expectedIntent = Intent()
        val webLinkActivityContract = mock<WebLinkActivityContract>()
        whenever(webLinkActivityContract.createIntent(context, args))
            .thenReturn(expectedIntent)
        return webLinkActivityContract to expectedIntent
    }

    private fun mockNativeLinkContract(): Pair<NativeLinkActivityContract, Intent> {
        val expectedIntent = Intent()
        val nativeLinkActivityContract = mock<NativeLinkActivityContract>()
        whenever(nativeLinkActivityContract.createIntent(context, args))
            .thenReturn(expectedIntent)
        return nativeLinkActivityContract to expectedIntent
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
