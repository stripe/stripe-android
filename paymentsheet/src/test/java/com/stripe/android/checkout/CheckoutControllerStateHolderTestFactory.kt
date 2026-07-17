package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentBuilder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import javax.inject.Provider

/**
 * Builds a [CheckoutControllerStateHolder] for tests that only exercise its state/selection
 * behavior. The [EmbeddedContentBuilder] is built lazily (only when `embeddedContent` /
 * `walletButtonsContent` is first collected), so the default throwing provider is never invoked
 * unless a test opts into content. Tests that assert on content should pass a real builder.
 */
internal fun createTestCheckoutControllerStateHolder(
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    errorReporter: ErrorReporter = FakeErrorReporter(),
    coroutineScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
    contentBuilder: Provider<EmbeddedContentBuilder> = Provider {
        error("EmbeddedContentBuilder not configured for this test")
    },
): CheckoutControllerStateHolder = CheckoutControllerStateHolder(
    savedStateHandle = savedStateHandle,
    errorReporter = errorReporter,
    coroutineScope = coroutineScope,
    contentBuilder = contentBuilder,
)
