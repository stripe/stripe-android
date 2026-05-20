package com.stripe.android.payments.samsungpay

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val TAG = "SamsungPayViewModel"

internal class SamsungPayViewModel : ViewModel() {

    private val _samsungPayResult = MutableSharedFlow<SamsungPayResult>(replay = 1)
    val samsungPayResult: Flow<SamsungPayResult> = _samsungPayResult.asSharedFlow()

    fun startPayment(context: Context) {
        val paymentManager = PaymentManager(
            context.applicationContext,
            SamsungFactory.buildPartnerInfo()
        )

        val transactionInfoListener: PaymentManager.CustomSheetTransactionInfoListener =
            object : PaymentManager.CustomSheetTransactionInfoListener {
                override fun onCardInfoUpdated(
                    selectedCardInfo: CardInfo,
                    customSheet: CustomSheet
                ) {
                    Log.d(TAG, "transactionInfoListener.onCardInfoUpdated: card changed")
                    Log.d(TAG, "  brand=${selectedCardInfo.brand}")
                    Log.d(TAG, "  cardId=${selectedCardInfo.cardId}")
                    logBundleContents("onCardInfoUpdated.cardMetaData", selectedCardInfo.cardMetaData)
                    paymentManager.updateSheet(customSheet)
                    Log.d(TAG, "transactionInfoListener.onCardInfoUpdated: updateSheet() called")
                }

                override fun onSuccess(
                    response: CustomSheetPaymentInfo,
                    paymentCredential: String,
                    extraPaymentData: Bundle
                ) {
                    Log.d(TAG, "transactionInfoListener.onSuccess: payment succeeded")
                    Log.d(TAG, "  merchantName=${response.merchantName}")
                    Log.d(TAG, "  merchantId=${response.merchantId}")
                    Log.d(TAG, "  orderNumber=${response.orderNumber}")
                    Log.d(TAG, "  paymentProtocol=${response.paymentProtocol}")
                    Log.d(TAG, "  merchantCountryCode=${response.merchantCountryCode}")
                    Log.d(TAG, "  paymentCardBrand=${response.paymentCardBrand}")
                    Log.d(TAG, "  paymentCardLast4DPAN=${response.paymentCardLast4DPAN}")
                    Log.d(TAG, "  paymentCardLast4FPAN=${response.paymentCardLast4FPAN}")
                    Log.d(TAG, "  paymentCurrencyCode=${response.paymentCurrencyCode}")
                    Log.d(TAG, "  isRecurring=${response.isRecurring}")
                    Log.d(TAG, "  paymentCredential length=${paymentCredential.length}")
                    Log.d(TAG, "  paymentCredential=$paymentCredential")
                    logBundleContents("onSuccess.extraPaymentInfo", response.extraPaymentInfo)
                    logBundleContents("onSuccess.extraPaymentData", extraPaymentData)
                    viewModelScope.launch {
                        _samsungPayResult.emit(SamsungPayResult.Success)
                    }
                }

                override fun onFailure(errorCode: Int, errorData: Bundle?) {
                    Log.e(TAG, "transactionInfoListener.onFailure: errorCode=$errorCode")
                    logBundleContents("onFailure.errorData", errorData)
                    viewModelScope.launch {
                        _samsungPayResult.emit(SamsungPayResult.Failure(Throwable("samsung pay failed")))
                    }
                }
            }

        val sheetPaymentInfo = makeTransactionDetailsWithSheet()

        paymentManager.startInAppPayWithCustomSheet(
            sheetPaymentInfo,
            transactionInfoListener
        )
    }


    /*
 * Make user's transaction details.
 * The merchant app should send PaymentInfo to Samsung Pay via the applicable Samsung Pay SDK API method for the operation
 * being invoked.
 * Upon successful user authentication, Samsung Pay returns the "Payment Info" structure and the result string.
 * The result string is forwarded to the PG for transaction completion and will vary based on the requirements of the PG used.
 * The code example below illustrates how to populate payment information in each field of the PaymentInfo class.
 */
    private fun makeTransactionDetailsWithSheet(): CustomSheetPaymentInfo {
        val brandList = brandList

        val extraPaymentInfo = Bundle()
        val customSheet = CustomSheet()

        customSheet.addControl(makeAmountControl())
        return CustomSheetPaymentInfo.Builder()
            .setMerchantId("123456")
            .setMerchantName("Sample Merchant")
            .setOrderNumber("AMZ007MAR")
            // If you want to enter address, please refer to the javaDoc :
            // reference/com/samsung/android/sdk/samsungpay/v2/payment/sheet/AddressControl.html
            .setAddressInPaymentSheet(CustomSheetPaymentInfo.AddressInPaymentSheet.DO_NOT_SHOW)
            .setAllowedCardBrands(brandList)
            .setCardHolderNameEnabled(true)
            .setRecurringEnabled(false)
            .setCustomSheet(customSheet)
            .setExtraPaymentInfo(extraPaymentInfo)
            .build()
    }

    private fun makeAmountControl(): AmountBoxControl {
//        val amountBoxControl = AmountBoxControl(AMOUNT_CONTROL_ID, "USD")
        val amountBoxControl = AmountBoxControl("amount_control_id", "USD")
//        amountBoxControl.addItem(PRODUCT_ITEM_ID, "Item", 1199.00, "")
        amountBoxControl.addItem("product_item_id", "Item", .20, "")
//        amountBoxControl.addItem(PRODUCT_TAX_ID, "Tax", 5.0, "")
        amountBoxControl.addItem("product_tax_id", "Tax", .30, "")
//        amountBoxControl.addItem(PRODUCT_SHIPPING_ID, "Shipping", 1.0, "")
        amountBoxControl.addItem("product_shipping_id", "Shipping", .10, "")
        amountBoxControl.setAmountTotal(.60, AmountConstants.FORMAT_TOTAL_PRICE_ONLY)
        return amountBoxControl
    }

    private fun logBundleContents(tag: String, bundle: Bundle?) {
        if (bundle == null) {
            Log.d(TAG, "[$tag] Bundle is null")
            return
        }
        if (bundle.isEmpty) {
            Log.d(TAG, "[$tag] Bundle is empty")
            return
        }
        Log.d(TAG, "[$tag] Bundle contents (${bundle.size()} entries):")
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            Log.d(
                TAG,
                "[$tag]   key=\"$key\" value=\"$value\" type=${value?.javaClass?.simpleName ?: "null"}"
            )
        }
    }

    private val brandList: ArrayList<SpaySdk.Brand>
        get() {
            val brandList = ArrayList<SpaySdk.Brand>()
            brandList.add(SpaySdk.Brand.VISA)
            brandList.add(SpaySdk.Brand.MASTERCARD)
            brandList.add(SpaySdk.Brand.AMERICANEXPRESS)
            brandList.add(SpaySdk.Brand.DISCOVER)

            return brandList
        }

    internal class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SamsungPayViewModel() as T
        }
    }
}