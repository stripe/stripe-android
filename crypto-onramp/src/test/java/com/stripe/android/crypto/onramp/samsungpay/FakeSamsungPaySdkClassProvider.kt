package com.stripe.android.crypto.onramp.samsungpay

internal val fakeSamsungPaySdkClassProvider = SamsungPayClassProvider(::fakeSamsungPaySdkClass)

internal fun fakeSamsungPaySdkClass(className: String): Class<*> {
    return fakeSamsungPaySdkClasses[className] ?: throw ClassNotFoundException(className)
}

private val fakeSamsungPaySdkClasses: Map<String, Class<*>> = mapOf(
    SamsungPaySdkClassNames.PARTNER_INFO to PartnerInfo::class.java,
    SamsungPaySdkClassNames.SPAY_SDK to SpaySdk::class.java,
    SamsungPaySdkClassNames.SERVICE_TYPE to SpaySdk.ServiceType::class.java,
    SamsungPaySdkClassNames.SDK_API_LEVEL to SpaySdk.SdkApiLevel::class.java,
    SamsungPaySdkClassNames.BRAND to SpaySdk.Brand::class.java,
    SamsungPaySdkClassNames.SAMSUNG_PAY to SamsungPay::class.java,
    SamsungPaySdkClassNames.STATUS_LISTENER to StatusListener::class.java,
    SamsungPaySdkClassNames.PAYMENT_MANAGER to PaymentManager::class.java,
    SamsungPaySdkClassNames.CUSTOM_SHEET_LISTENER to
        PaymentManager.CustomSheetTransactionInfoListener::class.java,
    SamsungPaySdkClassNames.CUSTOM_SHEET_PAYMENT_INFO to CustomSheetPaymentInfo::class.java,
    SamsungPaySdkClassNames.CUSTOM_SHEET_PAYMENT_INFO_BUILDER to CustomSheetPaymentInfo.Builder::class.java,
    SamsungPaySdkClassNames.ADDRESS_IN_PAYMENT_SHEET to
        CustomSheetPaymentInfo.AddressInPaymentSheet::class.java,
    SamsungPaySdkClassNames.CUSTOM_SHEET to CustomSheet::class.java,
    SamsungPaySdkClassNames.SHEET_CONTROL to SheetControl::class.java,
    SamsungPaySdkClassNames.AMOUNT_BOX_CONTROL to AmountBoxControl::class.java,
    SamsungPaySdkClassNames.AMOUNT_CONSTANTS to AmountConstants::class.java,
)
