package com.stripe.android.paymentsheet.forms

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import com.stripe.android.paymentsheet.forms.generated.FormElementSpecV1 as FormElementSpec
import com.stripe.android.paymentsheet.forms.generated.PaymentMethodFormSpecV1 as PaymentMethodFormSpec
import com.stripe.android.paymentsheet.forms.generated.SelectorIconV1 as SelectorIcon

internal fun List<PaymentMethodFormSpec>.toLpmSpecJson(): String {
    return JsonArray(
        filterNot { formSpec ->
            formSpec.fields.none(FormElementSpec::isDeclarative)
        }.map { it.toJson() }
    ).toString()
}

private fun PaymentMethodFormSpec.toJson(): JsonObject = buildJsonObject {
    put("type", type)
    putJsonArray("fields") {
        fields.filter(FormElementSpec::isDeclarative).forEach { add(it.toJson()) }
    }
    selectorIcon?.let { put("selector_icon", it.toJson()) }
}

private fun FormElementSpec.isDeclarative(): Boolean {
    return type != "native_component" && type != "mandate_text"
}

private fun FormElementSpec.toJson(): JsonObject = buildJsonObject {
    put("type", type)
    apiPath?.let { value ->
        putJsonObject("api_path") {
            put("v1", value)
        }
    }
    translationId?.let { put("translation_id", it) }
    if (items.isNotEmpty()) {
        putJsonArray("items") {
            items.forEach { option ->
                add(
                    buildJsonObject {
                        put("display_text", option.displayText)
                        option.apiValue?.let { put("api_value", it) }
                    }
                )
            }
        }
    }
    allowedCountryCodes?.let { countries ->
        put(
            "allowed_country_codes",
            buildJsonArray {
                countries.forEach { add(JsonPrimitive(it)) }
            }
        )
    }
    placeholderFor?.let { put("for", it) }
    component?.let { put("component", it) }
    subtitle?.let { put("subtitle", it) }
    disableBillingDetailCollection?.let { put("disable_billing_detail_collection", it) }
    textKey?.let { put("text_key", it) }
    setupFutureUsageRequired?.let { put("setup_future_usage_required", it) }
}

private fun SelectorIcon.toJson(): JsonObject = buildJsonObject {
    put("light_theme_png", lightThemePng)
    darkThemePng?.let { put("dark_theme_png", it) }
}
