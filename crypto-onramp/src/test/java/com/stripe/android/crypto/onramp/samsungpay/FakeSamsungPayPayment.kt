package com.stripe.android.crypto.onramp.samsungpay

import android.content.Context
import android.os.Bundle

class CardInfo

class CustomSheetPaymentInfo private constructor(
    val merchantId: String?,
    val merchantName: String?,
    val orderNumber: String?,
    val addressInPaymentSheet: AddressInPaymentSheet?,
    val allowedCardBrands: List<SpaySdk.Brand>,
    val isCardHolderNameEnabled: Boolean,
    val isRecurring: Boolean,
    val customSheet: CustomSheet?,
    val extraPaymentInfo: Bundle?,
) {
    enum class AddressInPaymentSheet {
        DO_NOT_SHOW,
    }

    class Builder {
        private var merchantId: String? = null
        private var merchantName: String? = null
        private var orderNumber: String? = null
        private var addressInPaymentSheet: AddressInPaymentSheet? = null
        private var allowedCardBrands: List<SpaySdk.Brand> = emptyList()
        private var isCardHolderNameEnabled: Boolean = false
        private var isRecurring: Boolean = false
        private var customSheet: CustomSheet? = null
        private var extraPaymentInfo: Bundle? = null

        fun setMerchantId(value: String) = apply { merchantId = value }
        fun setMerchantName(value: String) = apply { merchantName = value }
        fun setOrderNumber(value: String) = apply { orderNumber = value }
        fun setAddressInPaymentSheet(value: AddressInPaymentSheet) = apply { addressInPaymentSheet = value }
        fun setAllowedCardBrands(value: List<SpaySdk.Brand>) = apply { allowedCardBrands = value }
        fun setCardHolderNameEnabled(value: Boolean) = apply { isCardHolderNameEnabled = value }
        fun setRecurringEnabled(value: Boolean) = apply { isRecurring = value }
        fun setCustomSheet(value: CustomSheet) = apply { customSheet = value }
        fun setExtraPaymentInfo(value: Bundle) = apply { extraPaymentInfo = value }

        fun build(): CustomSheetPaymentInfo {
            return CustomSheetPaymentInfo(
                merchantId = merchantId,
                merchantName = merchantName,
                orderNumber = orderNumber,
                addressInPaymentSheet = addressInPaymentSheet,
                allowedCardBrands = allowedCardBrands,
                isCardHolderNameEnabled = isCardHolderNameEnabled,
                isRecurring = isRecurring,
                customSheet = customSheet,
                extraPaymentInfo = extraPaymentInfo,
            )
        }
    }
}

class PaymentManager(
    context: Context,
    partnerInfo: PartnerInfo,
) : SpaySdk() {
    init {
        check(context.applicationContext != null)
        FakeSamsungPaySdkState.paymentPartnerInfo = partnerInfo
    }

    fun startInAppPayWithCustomSheet(
        paymentInfo: CustomSheetPaymentInfo,
        listener: CustomSheetTransactionInfoListener,
    ) {
        FakeSamsungPaySdkState.paymentInfo = paymentInfo
        FakeSamsungPaySdkState.paymentListener = listener
    }

    fun updateSheet(customSheet: CustomSheet) {
        FakeSamsungPaySdkState.updatedSheet = customSheet
    }

    interface CustomSheetTransactionInfoListener {
        fun onCardInfoUpdated(selectedCardInfo: CardInfo, customSheet: CustomSheet)

        fun onSuccess(
            response: CustomSheetPaymentInfo,
            paymentCredential: String,
            extraPaymentData: Bundle,
        )

        fun onFailure(errorCode: Int, errorData: Bundle?)
    }
}
