package com.stripe.android.payments.samsungpay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet
import com.stripe.android.model.CardBrand

internal abstract class BaseSamsungPayActivity : AppCompatActivity() {

    protected fun startSamsungPay(
        config: Config,
        currencyCode: String,
        amount: Long,
        orderNumber: String,
    ) {
        val paymentManager = PaymentManager(
            applicationContext,
            SamsungFactory.buildPartnerInfo(config.serviceId)
        )

        val listener = object : PaymentManager.CustomSheetTransactionInfoListener {
            override fun onCardInfoUpdated(selectedCardInfo: CardInfo, customSheet: CustomSheet) {
                paymentManager.updateSheet(customSheet)
            }

            override fun onSuccess(
                response: CustomSheetPaymentInfo,
                paymentCredential: String,
                extraPaymentData: Bundle,
            ) {
                onSamsungPaySuccess(response, paymentCredential, extraPaymentData)
            }

            override fun onFailure(errorCode: Int, errorData: Bundle?) {
                onSamsungPayFailure(errorCode, errorData)
            }
        }

        val sheetPaymentInfo = buildPaymentInfo(config, currencyCode, amount, orderNumber)
        paymentManager.startInAppPayWithCustomSheet(sheetPaymentInfo, listener)
    }

    protected abstract fun onSamsungPaySuccess(
        response: CustomSheetPaymentInfo,
        paymentCredential: String,
        extraPaymentData: Bundle,
    )

    protected abstract fun onSamsungPayFailure(errorCode: Int, errorData: Bundle?)

    private fun buildPaymentInfo(
        config: Config,
        currencyCode: String,
        amount: Long,
        orderNumber: String,
    ): CustomSheetPaymentInfo {
        val brandList = ArrayList(
            ALL_SAMSUNG_PAY_BRANDS.filter { (cardBrand, _) ->
                config.cardBrandFilter.isAccepted(cardBrand)
            }.map { (_, spaySdkBrand) -> spaySdkBrand }
        )
        val customSheet = CustomSheet()
        customSheet.addControl(buildAmountControl(currencyCode, amount))

        return CustomSheetPaymentInfo.Builder()
            .setMerchantId(config.merchantId)
            .setMerchantName(config.merchantName)
            .setOrderNumber(orderNumber.sanitizeOrderNumber())
            .setAddressInPaymentSheet(CustomSheetPaymentInfo.AddressInPaymentSheet.DO_NOT_SHOW)
            .setAllowedCardBrands(brandList)
            .setCardHolderNameEnabled(true)
            .setRecurringEnabled(false)
            .setCustomSheet(customSheet)
            .setExtraPaymentInfo(Bundle())
            .build()
    }

    private fun buildAmountControl(currencyCode: String, amount: Long): AmountBoxControl {
        val amountDouble = amount / 100.0
        val amountBoxControl = AmountBoxControl("amount_control_id", currencyCode.uppercase())
        amountBoxControl.addItem("product_item_id", "Total", amountDouble, "")
        amountBoxControl.setAmountTotal(amountDouble, AmountConstants.FORMAT_TOTAL_PRICE_ONLY)
        return amountBoxControl
    }

    private fun String.sanitizeOrderNumber(): String =
        replace(Regex("[^A-Za-z0-9-]"), "-")

    private companion object {
        val ALL_SAMSUNG_PAY_BRANDS = listOf(
            CardBrand.Visa to SpaySdk.Brand.VISA,
            CardBrand.MasterCard to SpaySdk.Brand.MASTERCARD,
            CardBrand.AmericanExpress to SpaySdk.Brand.AMERICANEXPRESS,
            CardBrand.Discover to SpaySdk.Brand.DISCOVER,
        )
    }
}
