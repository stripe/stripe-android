package com.stripe.android.shoppay.bridge

import com.stripe.android.elements.payment.ShopPayConfiguration.DeliveryEstimate
import com.stripe.android.elements.payment.ShopPayConfiguration.LineItem
import com.stripe.android.elements.payment.ShopPayConfiguration.ShippingRate
import org.json.JSONObject

internal fun LineItem.toJSON(): JSONObject {
    return JSONObject().apply {
        put("name", name)
        put("amount", amount)
    }
}

internal fun ShippingRate.toJSON(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("displayName", displayName)
        deliveryEstimate?.let { estimate ->
            put("deliveryEstimate", estimate.toJSON())
        }
    }
}

internal fun DeliveryEstimate.toJSON(): Any {
    return when (this) {
        is DeliveryEstimate.Text -> {
            value
        }
        is DeliveryEstimate.Range -> {
            JSONObject().apply {
                putOpt("minimum", minimum?.toJSON())
                putOpt("maximum", maximum?.toJSON())
            }
        }
    }
}

internal fun DeliveryEstimate.DeliveryEstimateUnit.toJSON(): JSONObject {
    return JSONObject().apply {
        put("unit", unit.name.lowercase())
        put("value", value)
    }
}

// Mapper functions to convert ShopPayConfiguration types to ECE types

internal fun LineItem.toECELineItem(): ECELineItem {
    return ECELineItem(
        name = name,
        amount = amount
    )
}

internal fun ShippingRate.toECEShippingRate(): ECEShippingRate {
    return ECEShippingRate(
        id = id,
        amount = amount,
        displayName = displayName,
        deliveryEstimate = deliveryEstimate?.toECEDeliveryEstimate()
    )
}

internal fun DeliveryEstimate.toECEDeliveryEstimate(): ECEDeliveryEstimate {
    return when (this) {
        is DeliveryEstimate.Text -> {
            ECEDeliveryEstimate.Text(value = value)
        }
        is DeliveryEstimate.Range -> {
            ECEDeliveryEstimate.Range(
                value = ECEStructuredDeliveryEstimate(
                    maximum = maximum?.toECEDeliveryEstimateUnit(),
                    minimum = minimum?.toECEDeliveryEstimateUnit()
                )
            )
        }
    }
}

internal fun DeliveryEstimate.DeliveryEstimateUnit.toECEDeliveryEstimateUnit(): ECEDeliveryEstimateUnit {
    return ECEDeliveryEstimateUnit(
        unit = unit.toECEDeliveryTimeUnit(),
        value = value
    )
}

internal fun DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.toECEDeliveryTimeUnit(): DeliveryTimeUnit {
    return when (this) {
        DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.HOUR -> DeliveryTimeUnit.HOUR
        DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.DAY -> DeliveryTimeUnit.DAY
        DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.BUSINESS_DAY -> DeliveryTimeUnit.BUSINESS_DAY
        DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.WEEK -> DeliveryTimeUnit.WEEK
        DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.MONTH -> DeliveryTimeUnit.MONTH
    }
}
