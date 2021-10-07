package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.forms.AfterpayClearpayRequirementEvaluator
import com.stripe.android.paymentsheet.forms.BancontactRequirementEvaluator
import com.stripe.android.paymentsheet.forms.CardRequirementEvaluator
import com.stripe.android.paymentsheet.forms.EpsRequirementEvaluator
import com.stripe.android.paymentsheet.forms.GiropayRequirementEvaluator
import com.stripe.android.paymentsheet.forms.IdealRequirementEvaluator
import com.stripe.android.paymentsheet.forms.P24RequirementEvaluator
import com.stripe.android.paymentsheet.forms.RequirementEvaluator
import com.stripe.android.paymentsheet.forms.SepaDebitRequirementEvaluator
import com.stripe.android.paymentsheet.forms.SofortRequirementEvaluator
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
    val requirementEvaluator: RequirementEvaluator,
    val paramKey: MutableMap<String, Any?>,
    val formSpec: LayoutSpec?,
) {
    Card(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        CardRequirementEvaluator,
        mutableMapOf(),
        LayoutSpec.create(
            SaveForFutureUseSpec(emptyList())
        )
    ),

    Bancontact(
        PaymentMethod.Type.Bancontact,
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        BancontactRequirementEvaluator,
        bancontactParamKey,
        bancontactForm
    ),

    Sofort(
        PaymentMethod.Type.Sofort,
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        SofortRequirementEvaluator,
        sofortParamKey,
        sofortForm
    ),

    Ideal(
        PaymentMethod.Type.Ideal,
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        IdealRequirementEvaluator,
        idealParamKey,
        idealForm
    ),

    SepaDebit(
        PaymentMethod.Type.SepaDebit,
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        SepaDebitRequirementEvaluator,
        sepaDebitParamKey,
        sepaDebitForm
    ),

    Eps(
        PaymentMethod.Type.Eps,
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        EpsRequirementEvaluator,
        epsParamKey,
        epsForm
    ),

    P24(
        PaymentMethod.Type.P24,
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        P24RequirementEvaluator,
        p24ParamKey,
        p24Form
    ),

    Giropay(
        PaymentMethod.Type.Giropay,
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        GiropayRequirementEvaluator,
        giropayParamKey,
        giropayForm
    ),
    AfterpayClearpay(
        PaymentMethod.Type.AfterpayClearpay,
        R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
        R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
        AfterpayClearpayRequirementEvaluator,
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
