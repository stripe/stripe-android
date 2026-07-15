package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Tracks the [CheckoutController]s built during a test and calls [CheckoutController.destroy] on each
 * when the test finishes.
 *
 * Every `CheckoutController.Builder.build()` creates a controller that owns a `viewModelScope` with a
 * perpetual collector (started in [CheckoutConfirmationStateHolder]). Only [CheckoutController.destroy]
 * cancels that scope, so an untracked controller leaks its scope and DI graph across tests.
 *
 * Register controllers via [track].
 */
@OptIn(CheckoutSessionPreview::class)
internal class CheckoutControllerTestRule : TestWatcher() {
    private val controllers = mutableListOf<CheckoutController>()

    fun track(controller: CheckoutController): CheckoutController =
        controller.also { controllers.add(it) }

    override fun finished(description: Description) {
        controllers.forEach { it.destroy() }
        super.finished(description)
    }
}
