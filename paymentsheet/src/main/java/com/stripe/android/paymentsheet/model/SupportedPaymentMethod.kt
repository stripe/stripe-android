package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.forms.AfterpayClearpayRequirement
import com.stripe.android.paymentsheet.forms.BancontactRequirement
import com.stripe.android.paymentsheet.forms.CardRequirement
import com.stripe.android.paymentsheet.forms.EpsRequirement
import com.stripe.android.paymentsheet.forms.GiropayRequirement
import com.stripe.android.paymentsheet.forms.IdealRequirement
import com.stripe.android.paymentsheet.forms.P24Requirement
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.forms.SepaDebitRequirement
import com.stripe.android.paymentsheet.forms.SofortRequirement
import com.stripe.android.paymentsheet.forms.afterpayClearpayForm
import com.stripe.android.paymentsheet.forms.afterpayClearpayParamKey
import com.stripe.android.paymentsheet.forms.bancontactForm
import com.stripe.android.paymentsheet.forms.bancontactParamKey
import com.stripe.android.paymentsheet.forms.epsForm
import com.stripe.android.paymentsheet.forms.epsParamKey
import com.stripe.android.paymentsheet.forms.giropayForm
import com.stripe.android.paymentsheet.forms.giropayParamKey
import com.stripe.android.paymentsheet.forms.idealForm
import com.stripe.android.paymentsheet.forms.idealParamKey
import com.stripe.android.paymentsheet.forms.p24Form
import com.stripe.android.paymentsheet.forms.p24ParamKey
import com.stripe.android.paymentsheet.forms.sepaDebitForm
import com.stripe.android.paymentsheet.forms.sepaDebitParamKey
import com.stripe.android.paymentsheet.forms.sofortForm
import com.stripe.android.paymentsheet.forms.sofortParamKey

/**
 * Enum defining all payment method types for which Payment Sheet can collect payment data.
 *
 * FormSpec is optionally null only because Card is not converted to the compose model.
 */
internal enum class SupportedPaymentMethod(
    val type: PaymentMethod.Type,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int,
    val requirement: PaymentMethodRequirements,
    val paramKey: MutableMap<String, Any?>,
    val formSpec: LayoutSpec?,
) {
    Card(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        CardRequirement,
        mutableMapOf(),
        LayoutSpec.create(
            SaveForFutureUseSpec(emptyList())
        )
    ),

    Bancontact(
        PaymentMethod.Type.Bancontact,
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        BancontactRequirement,
        bancontactParamKey,
        bancontactForm
    ),

    Sofort(
        PaymentMethod.Type.Sofort,
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        SofortRequirement,
        sofortParamKey,
        sofortForm
    ),

    Ideal(
        PaymentMethod.Type.Ideal,
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        IdealRequirement,
        idealParamKey,
        idealForm
    ),

    SepaDebit(
        PaymentMethod.Type.SepaDebit,
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        SepaDebitRequirement,
        sepaDebitParamKey,
        sepaDebitForm
    ),

    Eps(
        PaymentMethod.Type.Eps,
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        EpsRequirement,
        epsParamKey,
        epsForm
    ),

    P24(
        PaymentMethod.Type.P24,
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        P24Requirement,
        p24ParamKey,
        p24Form
    ),

    Giropay(
        PaymentMethod.Type.Giropay,
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        GiropayRequirement,
        giropayParamKey,
        giropayForm
    ),
    AfterpayClearpay(
        PaymentMethod.Type.AfterpayClearpay,
        R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
        R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
        AfterpayClearpayRequirement,
        afterpayClearpayParamKey,
        afterpayClearpayForm
    );

    override fun toString(): String {
        return type.code
    }

    companion object {
        fun fromCode(code: String?) =
            values().firstOrNull { it.type.code == code }
    }
}
