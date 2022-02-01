package com.stripe.android.stripe3ds2.views

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter

internal enum class Brand(
    private val directoryServerName: String?,
    @DrawableRes internal val drawableResId: Int,
    @StringRes internal val nameResId: Int?,
    internal val shouldStretch: Boolean = false
) {
    Visa(
        "visa",
        R.drawable.stripe_3ds2_ic_visa,
        R.string.stripe_3ds2_brand_visa
    ),
    Mastercard(
        "mastercard",
        R.drawable.stripe_3ds2_ic_mastercard,
        R.string.stripe_3ds2_brand_mastercard
    ),
    Amex(
        "american_express",
        R.drawable.stripe_3ds2_ic_amex,
        R.string.stripe_3ds2_brand_amex
    ),
    Discover(
        "discover",
        R.drawable.stripe_3ds2_ic_discover,
        R.string.stripe_3ds2_brand_discover
    ),
    CartesBancaires(
        "cartes_bancaires",
        R.drawable.stripe_3ds2_ic_cartesbancaires,
        R.string.stripe_3ds2_brand_cartesbancaires,
        shouldStretch = true
    ),
    UnionPay(
        "unionpay",
        R.drawable.stripe_3ds2_ic_unionpay,
        R.string.stripe_3ds2_brand_unionpay
    ),
    Unknown(
        "unknown",
        R.drawable.stripe_3ds2_ic_unknown,
        null
    );

    internal companion object {
        internal fun lookup(
            directoryServerName: String,
            errorReporter: ErrorReporter
        ): Brand {
            val brand = values()
                .firstOrNull {
                    it.directoryServerName.equals(directoryServerName.trim(), ignoreCase = true)
                }

            return when {
                brand != null -> Result.success(brand)
                else -> {
                    val supportedNames = values().map { it.directoryServerName }
                    Result.failure(
                        SDKRuntimeException(
                            "Directory server name '$directoryServerName' is not supported. Must be one of $supportedNames."
                        )
                    )
                }
            }.onFailure {
                errorReporter.reportError(it)
            }.getOrDefault(Unknown)
        }
    }
}
