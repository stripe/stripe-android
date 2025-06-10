package com.stripe.android.shoppay.webview

import org.json.JSONObject

// --- Existing PaymentConfirmationData (ensure BillingDetails is fully mapped) ---
data class PaymentConfirmationData(
    val paymentMethodType: String?,
    val nonce: String?,
    val shippingAddress: ShippingAddress?,
    val billingDetails: BillingDetails?
) {
    data class ShippingAddress(
        val name: String?,
        val address: Address?
    )

    data class BillingDetails(
        val name: String?,
        val email: String?,
        val phone: String?,
        val address: Address?
    )

    data class Address(
        val line1: String?,
        val line2: String?,
        val city: String?,
        val state: String?,
        val postalCode: String?
    )
}

// --- Sealed Classes for different Stripe Parent Events ---
sealed interface StripeParentEvent {
    val eventType: String // e.g., "confirm", "change", "element-loader-ui-callback"
    val sourceFrameId: String?
    val controllerAppFrameId: String?
}

data class ConfirmEvent(
    override val eventType: String = "confirm",
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val paymentDetails: PaymentConfirmationData?
) : StripeParentEvent

data class ElementChangeEvent(
    override val eventType: String = "change",
    override val sourceFrameId: String?, // Helps identify which element
    override val controllerAppFrameId: String?,
    val value: JSONObject?, // Structure varies by element (e.g., email, address, payment value)
    val empty: Boolean?,
    val complete: Boolean?,
    val elementMode: String? = null, // e.g., for address element
    val collapsed: Boolean? = null, // e.g., for payment element
    val availablePaymentMethods: JSONObject? = null // For express checkout ready
) : StripeParentEvent

data class ElementLoaderUiCallbackEvent(
    override val eventType: String = "element-loader-ui-callback",
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val componentName: String?,
    val loaderUiNodes: JSONObject? // Raw JSON for loader UI structure
) : StripeParentEvent

data class FrameTitleEvent(
    override val eventType: String = "title",
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val title: String?
) : StripeParentEvent

data class ElementReadyEvent(
    override val eventType: String = "ready",
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val data: JSONObject? // Can be empty {} or contain specific data like availablePaymentMethods
) : StripeParentEvent

data class ElementClickEvent(
    override val eventType: String = "click",
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val nonce: String?,
    val paymentMethodType: String?
    // Other fields like overlayString, fontValues etc. can be added if needed
) : StripeParentEvent

data class SetStylesEvent(
    override val eventType: String = "set_styles",
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val styles: JSONObject? // Contains style attributes like height, transition
) : StripeParentEvent

data class LoadEvent(
    override val eventType: String = "load", // Typically for iframes like hcaptcha or google maps
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val data: JSONObject? // Usually empty
) : StripeParentEvent

data class UpdatePaymentMethodEvent( // For update-apple-pay, update-google-pay
    override val eventType: String, // "update-apple-pay" or "update-google-pay"
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val merchantDetails: JSONObject?,
    val country: String?,
    val currency: String?,
    val total: JSONObject?,
    val blockedCardBrands: List<String>?,
    val capabilityEnabledCardNetworks: List<String>?
) : StripeParentEvent

data class SetupEvent( // For setup-us-bank-account, setup-stripe-google-maps-autocomplete
    override val eventType: String,
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val data: JSONObject? // Usually empty
) : StripeParentEvent

data class DismissOverlayEvent(
    override val eventType: String = "dismiss-overlay",
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val data: JSONObject? // Usually empty
) : StripeParentEvent


// Catch-all for events not specifically handled or for simpler structures
data class GenericStripeParentEvent(
    override val eventType: String,
    override val sourceFrameId: String?,
    override val controllerAppFrameId: String?,
    val payloadData: JSONObject? // The "data" field from the payload object
) : StripeParentEvent
