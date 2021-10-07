package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.forms.AfterpayClearpayRequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.BancontactRequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.CardRequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.EpsRequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.GiropayRequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.IdealRequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.P24RequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.RequirementEvaluator
import com.stripe.android.paymentsheet.forms.SepaDebitRequirementStaticEvaluator
import com.stripe.android.paymentsheet.forms.SofortRequirementStaticEvaluator
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
    val requirements: RequirementEvaluator,
    val paramKey: MutableMap<String, Any?>,
    val formSpec: LayoutSpec?,
) {
    Card(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        CardRequirementStaticEvaluator,
        mutableMapOf(),
        LayoutSpec.create(
            SaveForFutureUseSpec(emptyList())
        )
    ),

    Bancontact(
        PaymentMethod.Type.Bancontact,
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        BancontactRequirementStaticEvaluator,
        bancontactParamKey,
        bancontactForm
    ),

    Sofort(
        PaymentMethod.Type.Sofort,
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        SofortRequirementStaticEvaluator,
        sofortParamKey,
        sofortForm
    ),

    Ideal(
        PaymentMethod.Type.Ideal,
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        IdealRequirementStaticEvaluator,
        idealParamKey,
        idealForm
    ),

    SepaDebit(
        PaymentMethod.Type.SepaDebit,
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        SepaDebitRequirementStaticEvaluator,
        sepaDebitParamKey,
        sepaDebitForm
    ),

    Eps(
        PaymentMethod.Type.Eps,
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        EpsRequirementStaticEvaluator,
        epsParamKey,
        epsForm
    ),

    P24(
        PaymentMethod.Type.P24,
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        P24RequirementStaticEvaluator,
        p24ParamKey,
        p24Form
    ),

    Giropay(
        PaymentMethod.Type.Giropay,
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        GiropayRequirementStaticEvaluator,
        giropayParamKey,
        giropayForm
    ),
    AfterpayClearpay(
        PaymentMethod.Type.AfterpayClearpay,
        R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
        R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
        AfterpayClearpayRequirementStaticEvaluator,
        afterpayClearpayParamKey,
        afterpayClearpayForm
    );

    override fun toString(): String {
        return type.code
    }

    companion object {
        fun fromCode(code: String?) =
            values().firstOrNull { it.type.code == code }

        /**
         * Defines all types of saved payment method supported on Payment Sheet.
         *
         * These are fetched from the
         * [PaymentMethods API endpoint](https://stripe.com/docs/api/payment_methods/list) for
         * returning customers.
         */
        val supportedSavedPaymentMethods = setOf("card")
    }
}

