package com.stripe.android.link

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
    fun `LinkActivityContract creates intent with URL with native link disabled`() {
        featureFlagTestRule.setEnabled(false)

        val expectedIntent = Intent()
        val webLinkActivityContract = mock<WebLinkActivityContract>()
        whenever(webLinkActivityContract.createIntent(ApplicationProvider.getApplicationContext(), args))
            .thenReturn(expectedIntent)

        val contract = linkActivityContract(webLinkActivityContract = webLinkActivityContract)

        val actualIntent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)

        assertThat(expectedIntent).isEqualTo(actualIntent)
    }

    @Test
    fun `LinkActivityContract parses result with webLinkActivityContract`() {
        featureFlagTestRule.setEnabled(false)

        val expectedIntent = Intent()
        val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)
        val webLinkActivityContract = mock<WebLinkActivityContract>()
        whenever(webLinkActivityContract.createIntent(ApplicationProvider.getApplicationContext(), args))
            .thenReturn(expectedIntent)

        val contract = linkActivityContract(webLinkActivityContract = webLinkActivityContract)

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        intent.data = Uri.EMPTY

        contract.parseResult(0, intent)

        verify(webLinkActivityContract).parseResult(0, intent)
    }

    @Test
    fun `LinkActivityContract creates intent with with NativeLinkArgs when native link is enabled`() {
        featureFlagTestRule.setEnabled(true)

        val expectedIntent = Intent()
        val nativeLinkActivityContract = mock<NativeLinkActivityContract>()
        whenever(nativeLinkActivityContract.createIntent(ApplicationProvider.getApplicationContext(), args))
            .thenReturn(expectedIntent)

        val contract = linkActivityContract(nativeLinkActivityContract = nativeLinkActivityContract)

        val actualIntent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)

        assertThat(expectedIntent).isEqualTo(actualIntent)
    }

    @Test
    fun `LinkActivityContract parses result with nativeLinkActivityContract`() {
        featureFlagTestRule.setEnabled(true)

        val nativeLinkActivityContract = mock<NativeLinkActivityContract>()
        whenever(nativeLinkActivityContract.createIntent(ApplicationProvider.getApplicationContext(), args))
            .thenReturn(Intent())

        val contract = linkActivityContract(nativeLinkActivityContract = nativeLinkActivityContract)

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
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
