package com.stripe.android.stripeconnect;

import android.os.Parcelable
import org.json.JSONObject

abstract class Appearance: Parcelable {
    open val colorPrimary: Int? = null
    open val colorText: Int? = null
    open val colorBackground: Int? = null
    open val buttonPrimaryColorBackground: Int? = null
    open val buttonPrimaryColorBorder: Int? = null
    open val buttonPrimaryColorText: Int? = null
    open val buttonSecondaryColorBackground: Int? = null
    open val buttonSecondaryColorBorder: Int? = null
    open val buttonSecondaryColorText: Int? = null
    open val badgeNeutralColorBackground: Int? = null
    open val badgeNeutralColorText: Int? = null
    open val badgeSuccessColorBackground: Int? = null
    open val badgeSuccessColorText: Int? = null
    open val badgeSuccessColorBorder: Int? = null
    open val badgeWarningColorBackground: Int? = null
    open val badgeWarningColorText: Int? = null
    open val badgeWarningColorBorder: Int? = null
    open val badgeDangerColorBackground: Int? = null
    open val badgeDangerColorText: Int? = null
    open val badgeDangerColorBorder: Int? = null
    open val borderRadius: Int? = null
    open val spacingUnit: Int? = null
    open val horizontalPadding: Int? = null
    open val fontFamily: String? = null

    @OptIn(ExperimentalStdlibApi::class)
    fun asJsonString(): String {
        val json = JSONObject()

        colorPrimary?.let { json.put("colorPrimary", "#${it.toHexString().takeLast(6)}") }
        colorText?.let { json.put("colorText", "#${it.toHexString().takeLast(6)}") }
        colorBackground?.let { json.put("colorBackground", "#${it.toHexString().takeLast(6)}") }
        buttonPrimaryColorBackground?.let { json.put("buttonPrimaryColorBackground", "#${it.toHexString().takeLast(6)}") }
        buttonPrimaryColorBorder?.let { json.put("buttonPrimaryColorBorder", "#${it.toHexString().takeLast(6)}") }
        buttonPrimaryColorText?.let { json.put("buttonPrimaryColorText", "#${it.toHexString().takeLast(6)}") }
        buttonSecondaryColorBackground?.let { json.put("buttonSecondaryColorBackground", "#${it.toHexString().takeLast(6)}") }
        buttonSecondaryColorBorder?.let { json.put("buttonSecondaryColorBorder", "#${it.toHexString().takeLast(6)}") }
        buttonSecondaryColorText?.let { json.put("buttonSecondaryColorText", "#${it.toHexString().takeLast(6)}") }
        badgeNeutralColorBackground?.let { json.put("badgeNeutralColorBackground", "#${it.toHexString().takeLast(6)}") }
        badgeNeutralColorText?.let { json.put("badgeNeutralColorText", "#${it.toHexString().takeLast(6)}") }
        badgeSuccessColorBackground?.let { json.put("badgeSuccessColorBackground", "#${it.toHexString().takeLast(6)}") }
        badgeSuccessColorText?.let { json.put("badgeSuccessColorText", "#${it.toHexString().takeLast(6)}") }
        badgeSuccessColorBorder?.let { json.put("badgeSuccessColorBorder", "#${it.toHexString().takeLast(6)}") }
        badgeWarningColorBackground?.let { json.put("badgeWarningColorBackground", "#${it.toHexString().takeLast(6)}") }
        badgeWarningColorText?.let { json.put("badgeWarningColorText", "#${it.toHexString().takeLast(6)}") }
        badgeWarningColorBorder?.let { json.put("badgeWarningColorBorder", "#${it.toHexString().takeLast(6)}") }
        badgeDangerColorBackground?.let { json.put("badgeDangerColorBackground", "#${it.toHexString().takeLast(6)}") }
        badgeDangerColorText?.let { json.put("badgeDangerColorText", "#${it.toHexString().takeLast(6)}") }
        badgeDangerColorBorder?.let { json.put("badgeDangerColorBorder", "#${it.toHexString().takeLast(6)}") }
        borderRadius?.let { json.put("borderRadius", it) }
        spacingUnit?.let { json.put("spacingUnit", it) }
        horizontalPadding?.let { json.put("horizontalPadding", it) }
        fontFamily?.let { json.put("fontFamily", it) }

        return json.toString()
    }
}
