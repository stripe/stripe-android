package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.stripe.android.GooglePayJsonFactory.TransactionInfo.CheckoutOption
import com.stripe.android.GooglePayJsonFactory.TransactionInfo.TotalPriceStatus
import dev.drewhamilton.poko.Poko
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GooglePayDynamicUpdateCallbacks : BasePaymentDataCallbacks() {
    private var key: String? = null
    private var handlers = mutableMapOf<String, GooglePayDynamicUpdateHandler>()

    fun setUpdateReceiver(key: String) {
        this.key = key
    }

    fun register(key: String, handler: GooglePayDynamicUpdateHandler) {
        handlers[key] = handler
    }

    fun unregister(key: String) {
        handlers.remove(key)
    }

    fun register(
        key: String,
        handler: GooglePayDynamicUpdateHandler,
        lifecycleOwner: LifecycleOwner,
    ) {
        register(key, handler)

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    unregister(key)
                }
            }
        )
    }

    override fun onPaymentDataChanged(
        request: IntermediatePaymentData?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate?>
    ) {
        val request = GooglePayReceivedUpdate.fromJson(request?.toJson())

        key?.let {
            handlers[key]?.handle(request) { response ->
                onCompleteListener.complete(PaymentDataRequestUpdate.fromJson(response.toJson()))
            }
        }
    }
}

@Poko
class GooglePayReceivedUpdate(
    val address: Address?,
    val shippingOption: ShippingOptionData?,
) {
    @Poko
    class Address(
        val countryCode: String,
        val state: String,
        val city: String,
        val postalCode: String,
    )

    @Poko
    class ShippingOptionData(
        val id: String
    )

    internal companion object {
        fun fromJson(json: String?): GooglePayReceivedUpdate {
            if (json == null) {
                return GooglePayReceivedUpdate(address = null, shippingOption = null)
            }

            val updateJsonObject = JSONObject(json)

            val shippingAddressObject = updateJsonObject
                .optJSONObject("shippingAddress")
                ?.let { shippingObject ->
                    Address(
                        countryCode = shippingObject.getString("countryCode"),
                        state = shippingObject.getString("administrativeArea"),
                        city = shippingObject.getString("locality"),
                        postalCode = shippingObject.getString("postalCode"),
                    )
                }

            val shippingOptionData = updateJsonObject
                .optJSONObject("shippingOptionData")
                ?.let { shippingOption ->
                    ShippingOptionData(
                        id = shippingOption.getString("id"),
                    )
                }

            return GooglePayReceivedUpdate(
                address = shippingAddressObject,
                shippingOption = shippingOptionData,
            )
        }
    }
}

@Poko
class GooglePayUpdate(
    val transactionInfo: TransactionInfo?,
    val shippingOptionParameters: ShippingOptionParameters?,
) {
    @Poko
    class TransactionInfo(
        val currencyCode: String,
        val totalPriceStatus: TotalPriceStatus,
        val totalPrice: String,
        val checkoutOption: CheckoutOption,
    )

    @Poko
    class ShippingOptionParameters(
        val selectedOptionId: String?,
        val options: List<SelectionOption>,
    ) {
        @Poko
        class SelectionOption(
            val id: String,
            val label: String,
            val description: String?,
        )
    }

    internal fun toJson(): String {
        val updateJsonObject = JSONObject()

        transactionInfo?.run {
            updateJsonObject.put(
                "newTransactionInfo",
                JSONObject().apply {
                    put("currencyCode", currencyCode)
                    put("totalPrice", totalPrice)
                    put("totalPriceStatus", totalPriceStatus.code)
                    put("checkoutOption", checkoutOption.code)
                }
            )
        }

        shippingOptionParameters?.run {
            updateJsonObject.put(
                "newShippingOptionParameters",
                JSONObject().apply {
                    selectedOptionId?.let {
                        put("defaultSelectedOptionId", it)
                    }

                    put(
                        "shippingOptions",
                        JSONArray().apply {
                            shippingOptionParameters.options.forEach { option ->
                                put(
                                    JSONObject().apply {
                                        put("id", option.id)
                                        put("label", option.label)

                                        option.description?.let {
                                            put("description", it)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }

        return updateJsonObject.toString()
    }
}

interface GooglePayDynamicUpdateHandler {
    fun handle(
        receivedUpdate: GooglePayReceivedUpdate,
        onUpdate: (update: GooglePayUpdate) -> Unit
    )
}
