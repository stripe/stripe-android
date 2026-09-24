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
import com.stripe.android.link.NativeLinkArgs
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentMethodFactory
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject

internal fun NetworkRule.enqueueLinkAccountLookup() {
    enqueue(
        method("POST"),
        path("/v1/consumers/sessions/lookup"),
    ) { response ->
        response.testBodyFromFile("consumer-session-lookup-success.json")
    }
}

internal fun enqueueSuccessfulGooglePayPayment(
    paymentMethod: PaymentMethod,
    shippingInformation: ShippingInformation? = null,
) {
    enqueueGooglePayPaymentResult(
        GooglePayPaymentMethodLauncher.Result.Completed(
            paymentMethod = paymentMethod,
            shippingInformation = shippingInformation,
        )
    )
}

internal fun createCheckoutInitResponseWithRequiredShippingAddress(response: MockResponse) {
    response.testBodyFromFile("checkout-session-init.json") { json ->
        json.put("customer_email", "checkout@example.com")
        json.put("account_settings", JSONObject().put("country", "US"))
        json.put(
            "shipping_address_collection",
            JSONObject().put("allowed_countries", JSONArray(listOf("US", "CA")))
        )
    }
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

    val shippingAddressParameters = googlePayLauncherArg("shippingAddressParameters")
    assertThat(shippingAddressParameters).isEqualTo(expected)
}

internal fun assertGooglePayCalledWithRequiredBillingAddress() {
    assertGooglePayCalled()

    val config = googlePayLauncherArg("config") as GooglePayPaymentMethodLauncher.Config
    assertThat(config.billingAddressConfig).isEqualTo(
        GooglePayPaymentMethodLauncher.BillingAddressConfig(
            isRequired = true,
            format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
            isPhoneNumberRequired = false,
        )
    )
}

internal fun enqueueSuccessfulNativeLinkPayment() {
    enqueueNativeLinkPaymentResult(
        LinkActivityResult.Completed(LinkAccountUpdate.None)
    )
}

internal fun enqueueNativeLinkPaymentMethod(paymentMethod: PaymentMethod) {
    enqueueNativeLinkPaymentResult(
        LinkActivityResult.PaymentMethodObtained(paymentMethod)
    )
}

internal fun createPaymentMethodWithBillingAddress(): PaymentMethod {
    return PaymentMethodFactory.card(
        last4 = "4242",
        id = "pm_1234",
        billingDetails = PaymentMethod.BillingDetails(
            address = Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 3",
                postalCode = "94103",
                state = "CA",
            ),
        ),
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

internal fun assertNativeLinkCalledWithRequiredBillingAddress() {
    assertNativeLinkCalled()

    val intent = getIntents().single { hasComponent(LinkActivity::class.java.name).matches(it) }
    val args = intent.extras?.let {
        BundleCompat.getParcelable(it, LinkActivity.EXTRA_ARGS, NativeLinkArgs::class.java)
    }
    assertThat(args?.configuration?.billingDetailsCollectionConfiguration).isEqualTo(
        PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )
    )
}

private fun googlePayLauncherArg(name: String): Any? {
    val intent = getIntents().single { hasComponent(GOOGLE_PAY_ACTIVITY_NAME).matches(it) }
    val args = intent.extras?.let {
        BundleCompat.getParcelable(it, "extra_args", Parcelable::class.java)
    }
    return requireNotNull(args).javaClass
        .getDeclaredField(name)
        .apply { isAccessible = true }
        .get(args)
}

private const val GOOGLE_PAY_ACTIVITY_NAME =
        "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"
