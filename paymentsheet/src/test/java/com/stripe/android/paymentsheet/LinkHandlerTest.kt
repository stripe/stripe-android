package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.LinkState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    private val configuration = LinkPaymentLauncher.Configuration(
        stripeIntent = mock(),
        merchantName = "Merchant, Inc",
        customerName = "Name",
        customerEmail = "customer@email.com",
        customerPhone = "1234567890",
        customerBillingCountryCode = "US",
        shippingValues = null,
    )

    @Test
    fun `foo`(): Unit = runBlocking {
        val linkLauncher = mock<LinkPaymentLauncher>()
        val eventReporter = mock<EventReporter>()
        val handler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentSelectionRepositoryProvider = { throw AssertionError("Unexpected") }
        )

        handler.setupLink(this, LinkState(configuration, LinkState.LoginState.LoggedIn))
        assertThat(handler.isLinkEnabled.asFlow().first()).isTrue()
        assertThat(handler.activeLinkSession.asFlow().first()).isTrue()
        assertThat(handler.linkConfiguration.asFlow().first()).isEqualTo(configuration)

        verify(linkLauncher).present(configuration, null)
        verifyNoMoreInteractions(linkLauncher)
        verifyNoInteractions(eventReporter)
    }
    // TODO(linkextraction): Add more tests with setup link
}
