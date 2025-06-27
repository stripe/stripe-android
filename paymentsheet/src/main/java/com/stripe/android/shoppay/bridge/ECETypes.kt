package com.stripe.android.shoppay.bridge

import android.os.Parcelable
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

@Parcelize
internal data class ECELineItem(
    val name: String,
    val amount: Int
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put(FIELD_NAME, name)
            put(FIELD_AMOUNT, amount)
        }
    }

    private companion object {
        private const val FIELD_NAME = "name"
        private const val FIELD_AMOUNT = "amount"
    }
}

@Parcelize
internal data class ECEBillingDetails(
    val name: String?,
    val email: String?,
    val phone: String?,
    val address: ECEFullAddress?
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            name?.let { put(FIELD_NAME, it) }
            email?.let { put(FIELD_EMAIL, it) }
            phone?.let { put(FIELD_PHONE, it) }
            address?.let { put(FIELD_ADDRESS, it.toJson()) }
        }
    }

    private companion object {
        private const val FIELD_NAME = "name"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_ADDRESS = "address"
    }
}

@Parcelize
internal data class ECEShippingAddressData(
    val name: String?,
    val address: ECEFullAddress?
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            name?.let { put(FIELD_NAME, it) }
            address?.let { put(FIELD_ADDRESS, it.toJson()) }
        }
    }

    private companion object {
        private const val FIELD_NAME = "name"
        private const val FIELD_ADDRESS = "address"
    }
}

@Parcelize
internal data class ECEFullAddress(
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            line1?.let { put(FIELD_LINE1, it) }
            line2?.let { put(FIELD_LINE2, it) }
            city?.let { put(FIELD_CITY, it) }
            state?.let { put(FIELD_STATE, it) }
            postalCode?.let { put(FIELD_POSTAL_CODE, it) }
            country?.let { put(FIELD_COUNTRY, it) }
        }
    }

    private companion object {
        private const val FIELD_LINE1 = "line1"
        private const val FIELD_LINE2 = "line2"
        private const val FIELD_CITY = "city"
        private const val FIELD_STATE = "state"
        private const val FIELD_POSTAL_CODE = "postal_code"
        private const val FIELD_COUNTRY = "country"
    }
}

@Parcelize
internal data class ECEPartialAddress(
    val addressLine: List<String>?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val phone: String?,
    val organization: String?
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            addressLine?.let { put(FIELD_ADDRESS_LINE, JSONArray(it)) }
            city?.let { put(FIELD_CITY, it) }
            state?.let { put(FIELD_STATE, it) }
            postalCode?.let { put(FIELD_POSTAL_CODE, it) }
            country?.let { put(FIELD_COUNTRY, it) }
            phone?.let { put(FIELD_PHONE, it) }
            organization?.let { put(FIELD_ORGANIZATION, it) }
        }
    }

    private companion object {
        private const val FIELD_ADDRESS_LINE = "addressLine"
        private const val FIELD_CITY = "city"
        private const val FIELD_STATE = "state"
        private const val FIELD_POSTAL_CODE = "postalCode"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_ORGANIZATION = "organization"
    }
}

@Parcelize
internal data class ECEShippingRate(
    val id: String,
    val amount: Int,
    val displayName: String,
    val deliveryEstimate: ECEDeliveryEstimate? = null
) : JsonSerializer, StripeModel {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put(FIELD_ID, id)
            put(FIELD_AMOUNT, amount)
            put(FIELD_DISPLAY_NAME, displayName)
            deliveryEstimate?.let {
                when (it) {
                    is ECEDeliveryEstimate.Text -> put(FIELD_DELIVERY_ESTIMATE, it.value)
                    is ECEDeliveryEstimate.Range -> put(FIELD_DELIVERY_ESTIMATE, it.value.toJson())
                }
            }
        }
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_DELIVERY_ESTIMATE = "deliveryEstimate"
    }
}

@Parcelize
internal sealed class ECEDeliveryEstimate : Parcelable {
    @Parcelize
    data class Text(val value: String) : ECEDeliveryEstimate()

    @Parcelize
    data class Range(val value: ECEStructuredDeliveryEstimate) : ECEDeliveryEstimate(), JsonSerializer {
        override fun toJson(): JSONObject {
            return value.toJson()
        }
    }
}

@Parcelize
internal data class ECEStructuredDeliveryEstimate(
    val maximum: ECEDeliveryEstimateUnit? = null,
    val minimum: ECEDeliveryEstimateUnit? = null
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            maximum?.let { put(FIELD_MAXIMUM, it.toJson()) }
            minimum?.let { put(FIELD_MINIMUM, it.toJson()) }
        }
    }

    private companion object {
        private const val FIELD_MAXIMUM = "maximum"
        private const val FIELD_MINIMUM = "minimum"
    }
}

@Parcelize
internal data class ECEDeliveryEstimateUnit(
    val unit: DeliveryTimeUnit,
    val value: Int
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put(FIELD_UNIT, unit.name.lowercase())
            put(FIELD_VALUE, value)
        }
    }

    private companion object {
        private const val FIELD_UNIT = "unit"
        private const val FIELD_VALUE = "value"
    }
}

@Parcelize
internal enum class DeliveryTimeUnit : Parcelable {
    HOUR,
    DAY,
    BUSINESS_DAY,
    WEEK,
    MONTH
}

@Parcelize
internal data class ECEPaymentMethodOptions(
    val shopPay: ShopPay?
) : JsonSerializer, Parcelable {
    @Parcelize
    data class ShopPay(
        val externalSourceId: String
    ) : JsonSerializer, Parcelable {
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put(FIELD_EXTERNAL_SOURCE_ID, externalSourceId)
            }
        }

        private companion object {
            private const val FIELD_EXTERNAL_SOURCE_ID = "externalSourceId"
        }
    }

    override fun toJson(): JSONObject {
        return JSONObject().apply {
            shopPay?.let { put(FIELD_SHOP_PAY, it.toJson()) }
        }
    }

    private companion object {
        private const val FIELD_SHOP_PAY = "shopPay"
    }
}
