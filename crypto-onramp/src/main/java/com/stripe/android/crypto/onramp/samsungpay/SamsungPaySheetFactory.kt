package com.stripe.android.crypto.onramp.samsungpay

import android.os.Bundle
import com.stripe.android.crypto.onramp.exception.SamsungPayException.Reason
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.model.CardBrand
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

internal class SamsungPaySheetFactory(
    private val reflection: SamsungPayReflection,
    private val configuration: OnrampConfiguration.SamsungPayConfig,
    private val merchantDisplayName: String,
) {
    fun buildPartnerInfo(): Any {
        val serviceType = reflection
            .enumConstant(SamsungPaySdkClassNames.SERVICE_TYPE, "INAPP_PAYMENT")
            .toString()
        val sdkApiLevelValue = reflection.enumConstant(SamsungPaySdkClassNames.SDK_API_LEVEL, "LEVEL_2_22")
        val sdkApiLevel = reflection.invoke(sdkApiLevelValue, "getLevel") as String
        val data = Bundle().apply {
            putString(
                reflection.staticString(SamsungPaySdkClassNames.SPAY_SDK, "PARTNER_SERVICE_TYPE"),
                serviceType,
            )
            putString(
                reflection.staticString(SamsungPaySdkClassNames.SPAY_SDK, "PARTNER_SDK_API_LEVEL"),
                sdkApiLevel,
            )
        }

        return reflection.newInstance(
            SamsungPaySdkClassNames.PARTNER_INFO,
            String::class.java to configuration.serviceId,
            Bundle::class.java to data,
        )
    }

    fun buildPaymentInfo(presentation: SamsungPayPresentation): Any {
        val customSheet = reflection.newInstance(SamsungPaySdkClassNames.CUSTOM_SHEET)
        reflection.invoke(
            customSheet,
            "addControl",
            reflection.loadClass(SamsungPaySdkClassNames.SHEET_CONTROL) to buildAmountControl(presentation),
        )

        val builder = reflection.newInstance(SamsungPaySdkClassNames.CUSTOM_SHEET_PAYMENT_INFO_BUILDER)
        configuration.merchantId?.let { merchantId ->
            reflection.invoke(builder, "setMerchantId", String::class.java to merchantId)
        }
        reflection.invoke(builder, "setMerchantName", String::class.java to merchantName())
        reflection.invoke(
            builder,
            "setOrderNumber",
            String::class.java to sanitizeOrderNumber(presentation.orderNumber),
        )
        reflection.invoke(
            builder,
            "setAddressInPaymentSheet",
            reflection.loadClass(SamsungPaySdkClassNames.ADDRESS_IN_PAYMENT_SHEET) to
                reflection.enumConstant(SamsungPaySdkClassNames.ADDRESS_IN_PAYMENT_SHEET, "DO_NOT_SHOW"),
        )
        reflection.invoke(builder, "setAllowedCardBrands", List::class.java to allowedCardBrands())
        reflection.invoke(
            builder,
            "setCardHolderNameEnabled",
            Boolean::class.javaPrimitiveType!! to true,
        )
        reflection.invoke(builder, "setRecurringEnabled", Boolean::class.javaPrimitiveType!! to false)
        reflection.invoke(
            builder,
            "setCustomSheet",
            reflection.loadClass(SamsungPaySdkClassNames.CUSTOM_SHEET) to customSheet,
        )
        reflection.invoke(builder, "setExtraPaymentInfo", Bundle::class.java to Bundle())
        return reflection.invoke(builder, "build")
            ?: failInvalidConfiguration("Samsung Pay failed to build payment information.", null)
    }

    fun validateConfiguration() {
        requireValid(configuration.serviceId.isNotBlank(), "Samsung Pay service ID must not be blank.")
        requireValid(configuration.merchantId?.isNotBlank() != false, "Samsung Pay merchant ID must not be blank.")
        requireValid(merchantName().isNotBlank(), "Samsung Pay merchant name must not be blank.")
        requireValid(configuration.allowedCardBrands.isNotEmpty(), "Samsung Pay must allow at least one card brand.")
    }

    private fun buildAmountControl(presentation: SamsungPayPresentation): Any {
        requireValid(presentation.amount > 0, "Samsung Pay amount must be greater than zero.")
        val currencyCode = presentation.currencyCode.uppercase(Locale.ROOT)
        val currency = runCatching { Currency.getInstance(currencyCode) }.getOrElse { error ->
            failInvalidConfiguration("Samsung Pay currency code is invalid.", error)
        }
        val fractionDigits = currency.defaultFractionDigits
        requireValid(fractionDigits >= 0, "Samsung Pay currency must have a defined fraction digit count.")
        val amount = BigDecimal.valueOf(presentation.amount)
            .movePointLeft(fractionDigits)
            .toDouble()
        val amountControl = reflection.newInstance(
            SamsungPaySdkClassNames.AMOUNT_BOX_CONTROL,
            String::class.java to AMOUNT_CONTROL_ID,
            String::class.java to currencyCode,
        )
        reflection.invoke(
            amountControl,
            "addItem",
            String::class.java to PRODUCT_ITEM_ID,
            String::class.java to TOTAL_TITLE,
            Double::class.javaPrimitiveType!! to amount,
            String::class.java to "",
        )
        reflection.invoke(
            amountControl,
            "setAmountTotal",
            Double::class.javaPrimitiveType!! to amount,
            String::class.java to reflection.staticString(
                SamsungPaySdkClassNames.AMOUNT_CONSTANTS,
                "FORMAT_TOTAL_PRICE_ONLY",
            ),
        )
        return amountControl
    }

    private fun allowedCardBrands(): List<Any> {
        return configuration.allowedCardBrands.map { cardBrand ->
            val samsungName = when (cardBrand) {
                CardBrand.Visa -> "VISA"
                CardBrand.MasterCard -> "MASTERCARD"
                CardBrand.AmericanExpress -> "AMERICANEXPRESS"
                CardBrand.Discover -> "DISCOVER"
                else -> failInvalidConfiguration(
                    "Card brand $cardBrand is not supported by Samsung Pay onramp.",
                    null,
                )
            }
            reflection.enumConstant(SamsungPaySdkClassNames.BRAND, samsungName)
        }
    }

    private fun merchantName(): String = configuration.merchantName ?: merchantDisplayName

    private fun sanitizeOrderNumber(orderNumber: String): String {
        val sanitized = orderNumber.replace(ORDER_NUMBER_REGEX, "-")
        requireValid(
            sanitized.any(Char::isLetterOrDigit),
            "Samsung Pay order number must contain at least one alphanumeric character.",
        )
        return sanitized
    }

    private fun requireValid(
        condition: Boolean,
        message: String,
    ) {
        if (!condition) {
            failInvalidConfiguration(message, null)
        }
    }

    private fun failInvalidConfiguration(
        message: String,
        cause: Throwable?,
    ): Nothing {
        throw SamsungPayException(
            message = message,
            cause = cause,
            errorCode = null,
            reason = Reason.InvalidConfiguration,
        )
    }

    private companion object {
        val ORDER_NUMBER_REGEX = Regex("[^A-Za-z0-9-]")

        const val AMOUNT_CONTROL_ID = "stripe_samsung_pay_amount"
        const val PRODUCT_ITEM_ID = "stripe_samsung_pay_total"
        const val TOTAL_TITLE = "Total"
    }
}
