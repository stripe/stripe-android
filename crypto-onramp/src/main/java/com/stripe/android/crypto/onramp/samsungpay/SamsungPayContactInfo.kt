package com.stripe.android.crypto.onramp.samsungpay

import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.platformPayKycInfo
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import java.util.Locale

/** Samsung's shipping contact is used only for prefill, never as verified card billing details. */
internal class SamsungPayContactInfo(private val reflection: SamsungPayReflection) {
    fun buildControl(onSheetUpdated: (Any) -> Unit): Any {
        val control = reflection.newInstance(
            SamsungPaySdkClassNames.ADDRESS_CONTROL,
            String::class.java to CONTACT_CONTROL_ID,
            reflection.loadClass(SamsungPaySdkClassNames.SHEET_ITEM_TYPE) to
                reflection.enumConstant(SamsungPaySdkClassNames.SHEET_ITEM_TYPE, "SHIPPING_ADDRESS"),
        )
        val options = listOf(
            "DISPLAY_OPTION_ADDRESSEE", "DISPLAY_OPTION_ADDRESS", "DISPLAY_OPTION_PHONE_NUMBER", "DISPLAY_OPTION_EMAIL",
        ).fold(0) { value, field -> value or reflection.staticInt(SamsungPaySdkClassNames.ADDRESS_CONSTANTS, field) }
        reflection.invoke(control, "setDisplayOption", Int::class.javaPrimitiveType!! to options)
        val listenerClass = reflection.loadClass(SamsungPaySdkClassNames.SHEET_UPDATED_LISTENER)
        val listener = reflection.createProxy(listenerClass) { proxy, method, arguments ->
            if (method.name == "onResult") {
                arguments?.getOrNull(1)?.let(onSheetUpdated)
                null
            } else {
                reflection.handleProxyObjectMethod(proxy, method, arguments)
            }
        }
        reflection.invoke(control, "setSheetUpdatedListener", listenerClass to listener)
        return control
    }

    fun read(paymentInfo: Any): KycInfo? {
        val contact = reflection.invoke(paymentInfo, "getPaymentShippingAddress") ?: return null
        val country = contact.string("getCountryCode")?.uppercase(Locale.ROOT)?.let { code ->
            // Samsung may return ISO 3166 alpha-3; phone normalization and Link expect alpha-2.
            Locale.getISOCountries().firstOrNull {
                it == code || Locale.Builder().setRegion(it).build().isO3Country == code
            }
        }
        val details = PaymentMethod.BillingDetails(
            name = contact.string("getAddressee"),
            email = contact.string("getEmail"),
            phone = (reflection.invoke(contact, "getPhoneNumber") as? String)?.takeIf { it.isNotBlank() },
            address = Address(
                line1 = contact.string("getAddressLine1"),
                line2 = contact.string("getAddressLine2"),
                city = contact.string("getCity"),
                state = contact.string("getState"),
                country = country,
                postalCode = contact.string("getPostalCode"),
            ),
        )
        // Reuse the wallet name splitting and E.164/raw-phone contract without changing the PaymentMethod.
        return details.platformPayKycInfo()
    }

    private fun Any.string(method: String): String? =
        (reflection.invoke(this, method) as? String)?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val CONTACT_CONTROL_ID = "stripe_samsung_pay_contact"
    }
}
