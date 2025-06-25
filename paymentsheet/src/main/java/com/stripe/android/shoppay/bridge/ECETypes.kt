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
            put("name", name)
            put("amount", amount)
        }
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
            name?.let { put("name", it) }
            email?.let { put("email", it) }
            phone?.let { put("phone", it) }
            address?.let { put("address", it.toJson()) }
        }
    }
}

@Parcelize
internal data class ECEShippingAddressData(
    val name: String?,
    val address: ECEFullAddress?
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            name?.let { put("name", it) }
            address?.let { put("address", it.toJson()) }
        }
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
            line1?.let { put("line1", it) }
            line2?.let { put("line2", it) }
            city?.let { put("city", it) }
            state?.let { put("state", it) }
            postalCode?.let { put("postal_code", it) }
            country?.let { put("country", it) }
        }
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
            addressLine?.let { put("addressLine", JSONArray(it)) }
            city?.let { put("city", it) }
            state?.let { put("state", it) }
            postalCode?.let { put("postalCode", it) }
            country?.let { put("country", it) }
            phone?.let { put("phone", it) }
            organization?.let { put("organization", it) }
        }
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
            put("id", id)
            put("amount", amount)
            put("displayName", displayName)
            deliveryEstimate?.let {
                when (it) {
                    is ECEDeliveryEstimate.Text -> put("deliveryEstimate", it.value)
                    is ECEDeliveryEstimate.Range -> put("deliveryEstimate", it.value.toJson())
                }
            }
        }
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
            maximum?.let { put("maximum", it.toJson()) }
            minimum?.let { put("minimum", it.toJson()) }
        }
    }
}

@Parcelize
internal data class ECEDeliveryEstimateUnit(
    val unit: DeliveryTimeUnit,
    val value: Int
) : JsonSerializer, Parcelable {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("unit", unit.name.lowercase())
            put("value", value)
        }
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
                put("externalSourceId", externalSourceId)
            }
        }
    }

    override fun toJson(): JSONObject {
        return JSONObject().apply {
            shopPay?.let { put("shopPay", it.toJson()) }
        }
    }
}
