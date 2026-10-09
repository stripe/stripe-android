package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Build
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object CheckoutSessionParamsFactory {
    fun create(
        settings: CheckoutPlaygroundSettings.Snapshot,
        customerId: String?,
    ): JsonObject {
        val session = CheckoutPlaygroundDefinitions.session
        val paymentMethodSave = settings[session.paymentMethodSave]
        val paymentMethodRemove = settings[session.paymentMethodRemove]
        val automaticTax = settings[session.automaticTax]
        val shippingAddressCollection = settings[session.shippingAddressCollection]
        val billingAddressCollection = settings[session.billingAddressCollection]
        val invoiceCreation = settings[session.invoiceCreation]
        val email = resolvedEmail(settings)
        val currency = settings[session.currency].value

        return buildJsonObject {
            put("ui_mode", "mobile_elements")
            put(
                "metadata",
                buildJsonObject {
                    put("sdk", "stripe-android")
                    put("sdk_version", StripeSdkVersion.VERSION_NAME)
                    put("integration", "checkout-playground")
                    put("android_version", Build.VERSION.RELEASE)
                    put("android_api_level", Build.VERSION.SDK_INT.toString())
                },
            )
            put("currency", currency)
            putCart(currency)
            putAllowedPaymentMethodTypes(settings)
            putInvoiceCreation(invoiceCreation)
            putTaxAndAddresses(automaticTax, billingAddressCollection, shippingAddressCollection)
            putCustomer(
                customerId,
                email,
                paymentMethodSave,
                automaticTax,
                billingAddressCollection,
                shippingAddressCollection,
            )
            putSavedPaymentMethodOptions(customerId, paymentMethodSave, paymentMethodRemove)
        }
    }

    fun resolvedEmail(settings: CheckoutPlaygroundSettings.Snapshot): String? {
        val session = CheckoutPlaygroundDefinitions.session
        return settings[session.adaptivePricingCountry]
            .testEmail
            ?: settings[session.customerEmail].trim().ifEmpty { null }
    }

    private fun JsonObjectBuilder.putCart(currency: String) {
        put(
            "items",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", "one_time_price")
                        put(
                            "one_time_price",
                            buildJsonObject {
                                put("items", JsonArray(LINE_ITEMS.map { it.toJson(currency) }))
                            },
                        )
                    },
                )
            },
        )
    }

    private fun JsonObjectBuilder.putAllowedPaymentMethodTypes(settings: CheckoutPlaygroundSettings.Snapshot) {
        val session = CheckoutPlaygroundDefinitions.session
        if (!settings[session.automaticPaymentMethods]) {
            put(
                "allowed_payment_method_types",
                JsonArray(settings[session.allowedPaymentMethodTypes].sorted().map(::JsonPrimitive)),
            )
        }
    }

    private fun JsonObjectBuilder.putInvoiceCreation(enabled: Boolean) {
        if (enabled) {
            put(
                "invoice_creation",
                buildJsonObject {
                    put("enabled", true)
                    put(
                        "invoice_data",
                        buildJsonObject {
                            put("description", "Checkout live post-purchase invoice")
                            put("metadata", buildJsonObject { put("suite", "checkout_live_billing_invoice") })
                        },
                    )
                },
            )
        }
    }

    private fun JsonObjectBuilder.putTaxAndAddresses(
        automaticTax: Boolean,
        billingAddressCollection: Boolean,
        shippingAddressCollection: Boolean,
    ) {
        if (automaticTax) put("automatic_tax", buildJsonObject { put("enabled", true) })
        if (billingAddressCollection) put("billing_address_collection", "required")
        if (shippingAddressCollection) {
            put(
                "shipping_address_collection",
                buildJsonObject {
                    put("allowed_countries", JsonArray(ALLOWED_SHIPPING_COUNTRIES.map(::JsonPrimitive)))
                },
            )
        }
    }

    private fun JsonObjectBuilder.putCustomer(
        customerId: String?,
        email: String?,
        paymentMethodSave: Boolean,
        automaticTax: Boolean,
        billingAddressCollection: Boolean,
        shippingAddressCollection: Boolean,
    ) {
        if (customerId == null) {
            email?.let { put("customer_email", it) }
            if (paymentMethodSave) put("customer_creation", "always")
            return
        }

        put("customer", customerId)
        if (automaticTax) {
            val customerUpdate = buildJsonObject {
                if (billingAddressCollection) put("address", "auto")
                if (shippingAddressCollection) put("shipping", "auto")
            }
            if (customerUpdate.isNotEmpty()) put("customer_update", customerUpdate)
        }
    }

    private fun JsonObjectBuilder.putSavedPaymentMethodOptions(
        customerId: String?,
        paymentMethodSave: Boolean,
        paymentMethodRemove: Boolean,
    ) {
        if (customerId != null || paymentMethodSave) {
            put(
                "saved_payment_method_options",
                buildJsonObject {
                    put("payment_method_save", if (paymentMethodSave) "enabled" else "disabled")
                    put("payment_method_remove", if (paymentMethodRemove) "enabled" else "disabled")
                },
            )
        }
    }

    private fun LineItem.toJson(currency: String): JsonObject = buildJsonObject {
        put(
            "price_data",
            buildJsonObject {
                put("currency", currency)
                put(
                    "product_data",
                    buildJsonObject {
                        put("name", name)
                        put("tax_code", "txcd_99999999")
                    },
                )
                put("unit_amount", unitAmount)
                put("tax_behavior", "exclusive")
            },
        )
        put("quantity", quantity)
    }

    private data class LineItem(
        val name: String,
        val unitAmount: Int,
        val quantity: Int,
    )

    private val LINE_ITEMS = listOf(
        LineItem("Classic T-Shirt", CLASSIC_T_SHIRT_UNIT_AMOUNT, CLASSIC_T_SHIRT_QUANTITY),
        LineItem("Zip-Up Hoodie", ZIP_UP_HOODIE_UNIT_AMOUNT, ZIP_UP_HOODIE_QUANTITY),
    )

    private val ALLOWED_SHIPPING_COUNTRIES = listOf("US", "CA", "IE", "GB")

    private const val CLASSIC_T_SHIRT_UNIT_AMOUNT = 3500
    private const val CLASSIC_T_SHIRT_QUANTITY = 2
    private const val ZIP_UP_HOODIE_UNIT_AMOUNT = 5000
    private const val ZIP_UP_HOODIE_QUANTITY = 1
}
