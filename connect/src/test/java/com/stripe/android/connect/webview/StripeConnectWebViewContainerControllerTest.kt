package com.stripe.android.connect.webview

import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.EmbeddedComponentManager.Configuration
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(PrivateBetaConnectSDK::class)
class StripeConnectWebViewContainerControllerTest {
    private val view: StripeConnectWebViewContainerInternal = mock()
    private val embeddedComponentManager = EmbeddedComponentManager(
        configuration = Configuration(publishableKey = "pk_test_123"),
        fetchClientSecretCallback = { },
    )
    private val embeddedComponent: StripeEmbeddedComponent = StripeEmbeddedComponent.PAYOUTS

    private lateinit var controller: StripeConnectWebViewContainerController

    @Before
    fun setup() {
        controller = StripeConnectWebViewContainerController(
            view = view,
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = embeddedComponent,
        )
    }

    @Test
    fun `should load URL when view is attached`() {
        controller.onViewAttached()
        verify(view).loadUrl(
            "https://connect-js.stripe.com/v1.0/android_webview.html#component=payouts&publicKey=pk_test_123"
        )
    }
}
