package com.stripe.android.checkout

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.test.espresso.intent.Intents.getIntents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
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
    enqueueGooglePayPaymentResult(
        GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod)
    )
}

internal fun enqueueSuccessfulGooglePayPayment(
    paymentMethod: PaymentMethod,
    shippingInformation: ShippingInformation,
) {
    enqueueGooglePayPaymentResult(
        GooglePayPaymentMethodLauncher.Result.Completed(
            paymentMethod = paymentMethod,
            shippingInformation = shippingInformation,
        )
    )
}

internal fun enqueueFailedGooglePayPayment(error: Throwable) {
    enqueueGooglePayPaymentResult(
        GooglePayPaymentMethodLauncher.Result.Failed(
            error = error,
            errorCode = GooglePayPaymentMethodLauncher.INTERNAL_ERROR,
        )
    )
}

private fun enqueueGooglePayPaymentResult(result: GooglePayPaymentMethodLauncher.Result) {
    intending(hasComponent(GOOGLE_PAY_ACTIVITY_NAME)).respondWith(
        Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            Intent().putExtra(
                "extra_result",
                result,
            ),
        )
    )
}

internal fun assertGooglePayCalled() {
    intended(hasComponent(GOOGLE_PAY_ACTIVITY_NAME))
}

internal fun assertGooglePayCalledWithShippingAddressParameters(
    expected: GooglePayJsonFactory.ShippingAddressParameters,
) {
    assertGooglePayCalled()

    val intent = getIntents().single { hasComponent(GOOGLE_PAY_ACTIVITY_NAME).matches(it) }
    val args = intent.extras?.let {
        BundleCompat.getParcelable(it, "extra_args", Parcelable::class.java)
    }
    val shippingAddressParameters = requireNotNull(args).javaClass
        .getDeclaredField("shippingAddressParameters")
        .apply { isAccessible = true }
        .get(args)
    assertThat(shippingAddressParameters).isEqualTo(expected)
}

internal fun enqueueSuccessfulNativeLinkPayment() {
    enqueueNativeLinkPaymentResult(
        LinkActivityResult.Completed(LinkAccountUpdate.None)
    )
}

internal fun enqueueFailedNativeLinkPayment(error: Throwable) {
    enqueueNativeLinkPaymentResult(
        LinkActivityResult.Failed(
            error = error,
            linkAccountUpdate = LinkAccountUpdate.None,
        )
    )
}

private fun enqueueNativeLinkPaymentResult(result: LinkActivityResult) {
    intending(hasComponent(LinkActivity::class.java.name)).respondWith(
        Instrumentation.ActivityResult(
            LinkActivity.RESULT_COMPLETE,
            Intent().putExtra(
                LinkActivityContract.EXTRA_RESULT,
                result,
            ),
        )
    )
}

internal fun assertNativeLinkCalled() {
    intended(hasComponent(LinkActivity::class.java.name))
}

private const val GOOGLE_PAY_ACTIVITY_NAME =
        "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"
