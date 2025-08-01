package com.stripe.android.lpmfoundations.luxe

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.ui.IconHelper
import com.stripe.android.paymentsheet.verticalmode.DisplayablePaymentMethod
import com.stripe.android.ui.core.elements.SharedDataSpec

internal data class SupportedPaymentMethod(
    /**
     * This describes the PaymentMethod Type as described
     * https://stripe.com/docs/api/payment_intents/create#create_payment_intent-payment_method_types
     */
    val code: PaymentMethodCode,

    val syntheticCode: String = code,

    /** This describes the name that appears under the selector. */
    val displayName: ResolvableString,

    /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
    @DrawableRes val iconResource: Int,

    /** This describes the image in the LPM selector when the luminance of `Colors.component` is less than 0.5. */
    @DrawableRes val iconResourceNight: Int?,

    /** An optional light theme icon url if it's supported. */
    val lightThemeIconUrl: String?,

    /** An optional dark theme icon url if it's supported. */
    val darkThemeIconUrl: String?,

    /** Indicates if the lpm icon in the selector requires tinting. */
    val iconRequiresTinting: Boolean,

    /** The subtitle, or marketing copy for an LPM. */
    val subtitle: ResolvableString? = null,

    /** This describes the outlined version in the LPM selector. */
    @DrawableRes val outlinedIconResource: Int? = null,
) {
    constructor(
        paymentMethodDefinition: PaymentMethodDefinition,
        sharedDataSpec: SharedDataSpec? = null,
        @StringRes displayNameResource: Int,
        @DrawableRes iconResource: Int,
        @DrawableRes iconResourceNight: Int?,
        iconRequiresTinting: Boolean = false,
        subtitle: ResolvableString? = null,
        @DrawableRes outlinedIconResource: Int? = null,
    ) : this(
        code = paymentMethodDefinition.type.code,
        displayName = displayNameResource.resolvableString,
        iconResource = iconResource,
        iconResourceNight = iconResourceNight,
        lightThemeIconUrl = sharedDataSpec?.selectorIcon?.lightThemePng,
        darkThemeIconUrl = sharedDataSpec?.selectorIcon?.darkThemePng,
        iconRequiresTinting = iconRequiresTinting,
        subtitle = subtitle,
        outlinedIconResource = outlinedIconResource,
    )

    constructor(
        code: PaymentMethodCode,
        syntheticCode: PaymentMethodCode = code,
        @StringRes displayNameResource: Int,
        @DrawableRes iconResource: Int,
        @DrawableRes iconResourceNight: Int?,
        iconRequiresTinting: Boolean = false,
        lightThemeIconUrl: String?,
        darkThemeIconUrl: String?,
        subtitle: ResolvableString? = null,
        @DrawableRes outlinedIconResource: Int? = null,
    ) : this(
        code = code,
        syntheticCode = syntheticCode,
        displayName = displayNameResource.resolvableString,
        iconResource = iconResource,
        iconResourceNight = iconResourceNight,
        lightThemeIconUrl = lightThemeIconUrl,
        darkThemeIconUrl = darkThemeIconUrl,
        iconRequiresTinting = iconRequiresTinting,
        subtitle = subtitle,
        outlinedIconResource = outlinedIconResource,
    )

    fun asFormHeaderInformation(
        incentive: PaymentMethodIncentive?,
    ): FormHeaderInformation {
        return FormHeaderInformation(
            displayName = displayName,
            shouldShowIcon = true,
            iconResource = iconResource,
            iconResourceNight = iconResourceNight,
            outlinedIconResource = outlinedIconResource,
            lightThemeIconUrl = lightThemeIconUrl,
            darkThemeIconUrl = darkThemeIconUrl,
            iconRequiresTinting = iconRequiresTinting,
            promoBadge = incentive?.displayText,
        )
    }

    fun asDisplayablePaymentMethod(
        customerSavedPaymentMethods: List<PaymentMethod>,
        incentive: PaymentMethodIncentive?,
        onClick: () -> Unit,
    ): DisplayablePaymentMethod {
        fun isTypeAndHasCustomerSavedPaymentMethodsOfType(type: PaymentMethod.Type): Boolean {
            return customerSavedPaymentMethods.any { it.type == type } && code == type.code
        }

        val displayName = if (isTypeAndHasCustomerSavedPaymentMethodsOfType(PaymentMethod.Type.Card)) {
            R.string.stripe_paymentsheet_new_card.resolvableString
        } else {
            displayName
        }

        return DisplayablePaymentMethod(
            code = code,
            syntheticCode = syntheticCode,
            displayName = displayName,
            iconResource = iconResource,
            iconResourceNight = iconResourceNight,
            outlinedIconResource = outlinedIconResource,
            lightThemeIconUrl = lightThemeIconUrl,
            darkThemeIconUrl = darkThemeIconUrl,
            iconRequiresTinting = iconRequiresTinting,
            subtitle = subtitle,
            promoBadge = incentive?.displayText,
            onClick = onClick,
        )
    }

    @Composable
    @DrawableRes
    fun icon() = IconHelper.icon(
        iconRes = iconResource,
        iconResNight = iconResourceNight,
        outlinedIconResource = outlinedIconResource
    )

    @Composable
    fun iconUrl() = IconHelper.iconUrl(
        lightThemeIconUrl = lightThemeIconUrl,
        darkThemeIconUrl = darkThemeIconUrl
    )
}
