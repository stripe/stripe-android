package com.stripe.android.shoppay.bridge

import com.stripe.android.paymentsheet.PaymentSheet
import org.json.JSONObject

internal fun PaymentSheet.ShopPayConfiguration.LineItem.toJSON(): JSONObject {
    return JSONObject().apply {
        put("name", name)
        put("amount", amount)
    }
}

internal fun PaymentSheet.ShopPayConfiguration.ShippingRate.toJSON(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("displayName", displayName)
        deliveryEstimate?.let { estimate ->
            put("deliveryEstimate", estimate.toJSON())
        }
    }
}

internal fun PaymentSheet.ShopPayConfiguration.DeliveryEstimate.toJSON(): Any {
    return when (this) {
        is PaymentSheet.ShopPayConfiguration.DeliveryEstimate.Text -> {
            value
        }
        is PaymentSheet.ShopPayConfiguration.DeliveryEstimate.Range -> {
            JSONObject().apply {
                put("minimum", minimum.toJSON())
                put("maximum", maximum.toJSON())
            }
        }
    }
}

internal fun PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.toJSON(): JSONObject {
    return JSONObject().apply {
        put("unit", unit.name.lowercase())
        put("value", value)
    }
}
