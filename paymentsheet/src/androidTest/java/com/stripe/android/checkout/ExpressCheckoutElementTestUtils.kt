package com.stripe.android.checkout

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile

internal fun NetworkRule.enqueueLinkAccountLookup() {
    enqueue(
        method("POST"),
        path("/v1/consumers/sessions/lookup"),
    ) { response ->
        response.testBodyFromFile("consumer-session-lookup-success.json")
    }
}

internal fun enqueueSuccessfulGooglePayPayment(paymentMethod: PaymentMethod) {
    intending(hasComponent(GOOGLE_PAY_ACTIVITY_NAME)).respondWith(
        Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            Intent().putExtra(
                "extra_result",
                GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod),
            ),
        )
    )
}

internal fun assertGooglePayCalled() {
    intended(hasComponent(GOOGLE_PAY_ACTIVITY_NAME))
}

internal fun enqueueSuccessfulNativeLinkPayment() {
    intending(hasComponent(LinkActivity::class.java.name)).respondWith(
        Instrumentation.ActivityResult(
            LinkActivity.RESULT_COMPLETE,
            Intent().putExtra(
                LinkActivityContract.EXTRA_RESULT,
                LinkActivityResult.Completed(LinkAccountUpdate.None),
            ),
        )
    )
}

internal fun assertNativeLinkCalled() {
    intended(hasComponent(LinkActivity::class.java.name))
}

private const val GOOGLE_PAY_ACTIVITY_NAME =
        "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"