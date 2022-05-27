package com.stripe.android.ui.core.forms.resources

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.AffirmRequirement
import com.stripe.android.paymentsheet.forms.AfterpayClearpayRequirement
import com.stripe.android.paymentsheet.forms.AuBecsDebitRequirement
import com.stripe.android.paymentsheet.forms.BancontactRequirement
import com.stripe.android.paymentsheet.forms.CardRequirement
import com.stripe.android.paymentsheet.forms.EpsRequirement
import com.stripe.android.paymentsheet.forms.GiropayRequirement
import com.stripe.android.paymentsheet.forms.IdealRequirement
import com.stripe.android.paymentsheet.forms.KlarnaRequirement
import com.stripe.android.paymentsheet.forms.P24Requirement
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.forms.PaypalRequirement
import com.stripe.android.paymentsheet.forms.SepaDebitRequirement
import com.stripe.android.paymentsheet.forms.SofortRequirement
import com.stripe.android.paymentsheet.forms.USBankAccountRequirement
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.forms.AffirmForm
import com.stripe.android.ui.core.forms.AfterpayClearpayForm
import com.stripe.android.ui.core.forms.AuBecsDebitForm
import com.stripe.android.ui.core.forms.BancontactForm
import com.stripe.android.ui.core.forms.CardForm
import com.stripe.android.ui.core.forms.EpsForm
import com.stripe.android.ui.core.forms.GiropayForm
import com.stripe.android.ui.core.forms.IdealForm
import com.stripe.android.ui.core.forms.KlarnaForm
import com.stripe.android.ui.core.forms.P24Form
import com.stripe.android.ui.core.forms.PaypalForm
import com.stripe.android.ui.core.forms.SepaDebitForm
import com.stripe.android.ui.core.forms.SofortForm
import com.stripe.android.ui.core.forms.USBankAccountForm
import javax.inject.Singleton

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LpmRepository {
    fun values() = exposedPaymentMethods

    /**
     * This is a list of the payment methods that we are allowing in the release
     */
    @VisibleForTesting
    internal val exposedPaymentMethods by lazy {
        listOf(
            SupportedPaymentMethod.Card,
            SupportedPaymentMethod.Bancontact,
            SupportedPaymentMethod.Sofort,
            SupportedPaymentMethod.Ideal,
            SupportedPaymentMethod.SepaDebit,
            SupportedPaymentMethod.Eps,
            SupportedPaymentMethod.Giropay,
            SupportedPaymentMethod.P24,
            SupportedPaymentMethod.Klarna,
            SupportedPaymentMethod.PayPal,
            SupportedPaymentMethod.AfterpayClearpay,
            SupportedPaymentMethod.USBankAccount,
            SupportedPaymentMethod.Affirm,
            SupportedPaymentMethod.AuBecsDebit
        )
    }

    private val codeToForm = exposedPaymentMethods.associate {
        it.type.code to it
    }

    fun fromCode(code: PaymentMethodCode?) = code?.let {
        codeToForm[code]
    }

    /**
     * Enum defining all payment method types for which Payment Sheet can collect
     * payment data.
     *
     * FormSpec is optionally null only because Card is not converted to the
     * compose model.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    open class SupportedPaymentMethod(
        /**
         * This describes the PaymentMethod Type as described
         * https://stripe.com/docs/api/payment_intents/create#create_payment_intent-payment_method_types
         */
        val type: PaymentMethod.Type,

        /** This describes the name that appears under the selector. */
        @StringRes val displayNameResource: Int,

        /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
        @DrawableRes val iconResource: Int,

        /** Indicates if the lpm icon in the selector is a single color and should be tinted
         * on selection.
         */
        val tintIconOnSelection: Boolean,

        /**
         * This describes the requirements of the LPM including if it is supported with
         * PaymentIntents w/ or w/out SetupFutureUsage set, SetupIntent, or on-session when attached
         * to the customer object.
         */
        val requirement: PaymentMethodRequirements,

        /**
         * This describes how the UI should look.
         */
        val formSpec: LayoutSpec,
    ) {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Card : SupportedPaymentMethod(
            PaymentMethod.Type.Card,
            R.string.stripe_paymentsheet_payment_method_card,
            R.drawable.stripe_ic_paymentsheet_pm_card,
            true,
            CardRequirement,
            CardForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Bancontact : SupportedPaymentMethod(
            PaymentMethod.Type.Bancontact,
            R.string.stripe_paymentsheet_payment_method_bancontact,
            R.drawable.stripe_ic_paymentsheet_pm_bancontact,
            false,
            BancontactRequirement,
            BancontactForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Sofort : SupportedPaymentMethod(
            PaymentMethod.Type.Sofort,
            R.string.stripe_paymentsheet_payment_method_sofort,
            R.drawable.stripe_ic_paymentsheet_pm_klarna,
            false,
            SofortRequirement,
            SofortForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Ideal : SupportedPaymentMethod(
            PaymentMethod.Type.Ideal,
            R.string.stripe_paymentsheet_payment_method_ideal,
            R.drawable.stripe_ic_paymentsheet_pm_ideal,
            false,
            IdealRequirement,
            IdealForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object SepaDebit : SupportedPaymentMethod(
            PaymentMethod.Type.SepaDebit,
            R.string.stripe_paymentsheet_payment_method_sepa_debit,
            R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
            false,
            SepaDebitRequirement,
            SepaDebitForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Eps : SupportedPaymentMethod(
            PaymentMethod.Type.Eps,
            R.string.stripe_paymentsheet_payment_method_eps,
            R.drawable.stripe_ic_paymentsheet_pm_eps,
            false,
            EpsRequirement,
            EpsForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object P24 : SupportedPaymentMethod(
            PaymentMethod.Type.P24,
            R.string.stripe_paymentsheet_payment_method_p24,
            R.drawable.stripe_ic_paymentsheet_pm_p24,
            false,
            P24Requirement,
            P24Form
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Giropay : SupportedPaymentMethod(
            PaymentMethod.Type.Giropay,
            R.string.stripe_paymentsheet_payment_method_giropay,
            R.drawable.stripe_ic_paymentsheet_pm_giropay,
            false,
            GiropayRequirement,
            GiropayForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object AfterpayClearpay : SupportedPaymentMethod(
            PaymentMethod.Type.AfterpayClearpay,
            R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
            R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
            false,
            AfterpayClearpayRequirement,
            AfterpayClearpayForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Klarna : SupportedPaymentMethod(
            PaymentMethod.Type.Klarna,
            R.string.stripe_paymentsheet_payment_method_klarna,
            R.drawable.stripe_ic_paymentsheet_pm_klarna,
            false,
            KlarnaRequirement,
            KlarnaForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object PayPal : SupportedPaymentMethod(
            PaymentMethod.Type.PayPal,
            R.string.stripe_paymentsheet_payment_method_paypal,
            R.drawable.stripe_ic_paymentsheet_pm_paypal,
            false,
            PaypalRequirement,
            PaypalForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object Affirm : SupportedPaymentMethod(
            PaymentMethod.Type.Affirm,
            R.string.stripe_paymentsheet_payment_method_affirm,
            R.drawable.stripe_ic_paymentsheet_pm_affirm,
            false,
            AffirmRequirement,
            AffirmForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object AuBecsDebit : SupportedPaymentMethod(
            PaymentMethod.Type.AuBecsDebit,
            R.string.stripe_paymentsheet_payment_method_au_becs_debit,
            R.drawable.stripe_ic_paymentsheet_pm_bank,
            true,
            AuBecsDebitRequirement,
            AuBecsDebitForm
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        object USBankAccount : SupportedPaymentMethod(
            PaymentMethod.Type.USBankAccount,
            R.string.stripe_paymentsheet_payment_method_us_bank_account,
            R.drawable.stripe_ic_paymentsheet_pm_bank,
            true,
            USBankAccountRequirement,
            USBankAccountForm
        )

        /**
         * Returns true if the payment method supports confirming from a saved
         * payment method of this type.  See [PaymentMethodRequirements] for
         * description of the values
         */
        fun supportsCustomerSavedPM() = requirement.confirmPMFromCustomer == true

        override fun toString(): String {
            return type.code
        }
    }
}
