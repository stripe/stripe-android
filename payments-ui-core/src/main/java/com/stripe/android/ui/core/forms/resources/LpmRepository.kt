package com.stripe.android.ui.core.forms.resources

import android.content.res.Resources
import android.os.Looper
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentMethod
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
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import java.io.InputStream
import java.lang.RuntimeException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is responsible for loading the LPM UI Specification for all LPMs, and returning
 * a particular requested LPM.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LpmRepository @Inject constructor(
    resources: Resources?
) {
    private val lpmSerializer: LpmSerializer = LpmSerializer()

    private lateinit var codeToSupportedPaymentMethod: Map<String, SupportedPaymentMethod>

    fun values() = codeToSupportedPaymentMethod.values

    fun fromCode(code: String?) = code?.let { paymentMethodCode ->
        codeToSupportedPaymentMethod[paymentMethodCode]
    }

    init {
        initialize(
            resources?.assets?.open("lpms.json")
        )
    }

    @VisibleForTesting
    fun initialize(inputStream: InputStream?) {
        val parsedSupportedPaymentMethod = parseLpms(inputStream)
            ?.filter { exposedPaymentMethods.contains(it.type) }
            ?.mapNotNull { convertToSupportedPaymentMethod(it) }

        // By mapNotNull we will not accept any LPMs that are not known by the platform.
        codeToSupportedPaymentMethod =
            parsedSupportedPaymentMethod?.associateBy { it.type.code } ?: emptyMap()
    }

    private fun parseLpms(inputStream: InputStream?) =
        getJsonStringFromInputStream(inputStream)?.let { string ->
            lpmSerializer.deserializeList(string)
        }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }

    private fun convertToSupportedPaymentMethod(sharedDataSpec: SharedDataSpec) =
        when (sharedDataSpec.type) {
            PaymentMethod.Type.Card.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Card,
                R.string.stripe_paymentsheet_payment_method_card,
                R.drawable.stripe_ic_paymentsheet_pm_card,
                true,
                CardRequirement,
                if (sharedDataSpec.fields.isEmpty() || sharedDataSpec.fields == listOf(EmptyFormSpec)) {
                    HardcodedCard.formSpec
                } else {
                    LayoutSpec(sharedDataSpec.fields)
                }
            )
            PaymentMethod.Type.Bancontact.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Bancontact,
                R.string.stripe_paymentsheet_payment_method_bancontact,
                R.drawable.stripe_ic_paymentsheet_pm_bancontact,
                false,
                BancontactRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Sofort.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Sofort,
                R.string.stripe_paymentsheet_payment_method_sofort,
                R.drawable.stripe_ic_paymentsheet_pm_klarna,
                false,
                SofortRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Ideal.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Ideal,
                R.string.stripe_paymentsheet_payment_method_ideal,
                R.drawable.stripe_ic_paymentsheet_pm_ideal,
                false,
                IdealRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.SepaDebit.code -> SupportedPaymentMethod(
                PaymentMethod.Type.SepaDebit,
                R.string.stripe_paymentsheet_payment_method_sepa_debit,
                R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
                false,
                SepaDebitRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Eps.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Eps,
                R.string.stripe_paymentsheet_payment_method_eps,
                R.drawable.stripe_ic_paymentsheet_pm_eps,
                false,
                EpsRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.P24.code -> SupportedPaymentMethod(
                PaymentMethod.Type.P24,
                R.string.stripe_paymentsheet_payment_method_p24,
                R.drawable.stripe_ic_paymentsheet_pm_p24,
                false,
                P24Requirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Giropay.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Giropay,
                R.string.stripe_paymentsheet_payment_method_giropay,
                R.drawable.stripe_ic_paymentsheet_pm_giropay,
                false,
                GiropayRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.AfterpayClearpay.code -> SupportedPaymentMethod(
                PaymentMethod.Type.AfterpayClearpay,
                R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
                R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
                false,
                AfterpayClearpayRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Klarna.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Klarna,
                R.string.stripe_paymentsheet_payment_method_klarna,
                R.drawable.stripe_ic_paymentsheet_pm_klarna,
                false,
                KlarnaRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.PayPal.code -> SupportedPaymentMethod(
                PaymentMethod.Type.PayPal,
                R.string.stripe_paymentsheet_payment_method_paypal,
                R.drawable.stripe_ic_paymentsheet_pm_paypal,
                false,
                PaypalRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Affirm.code -> SupportedPaymentMethod(
                PaymentMethod.Type.Affirm,
                R.string.stripe_paymentsheet_payment_method_affirm,
                R.drawable.stripe_ic_paymentsheet_pm_affirm,
                false,
                AffirmRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.AuBecsDebit.code -> SupportedPaymentMethod(
                PaymentMethod.Type.AuBecsDebit,
                R.string.stripe_paymentsheet_payment_method_au_becs_debit,
                R.drawable.stripe_ic_paymentsheet_pm_bank,
                true,
                AuBecsDebitRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.USBankAccount.code -> SupportedPaymentMethod(
                PaymentMethod.Type.USBankAccount,
                R.string.stripe_paymentsheet_payment_method_us_bank_account,
                R.drawable.stripe_ic_paymentsheet_pm_bank,
                true,
                USBankAccountRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            else -> null
        }

    /**
     * Enum defining all payment method types for which Payment Sheet can collect
     * payment data.
     *
     * FormSpec is optionally null only because Card is not converted to the
     * compose model.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class SupportedPaymentMethod(
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

        /**
         * Returns true if the payment method supports confirming from a saved
         * payment method of this type.  See [PaymentMethodRequirements] for
         * description of the values
         */
        fun supportsCustomerSavedPM() = requirement.confirmPMFromCustomer == true
    }

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val HardcodedCard = SupportedPaymentMethod(
            PaymentMethod.Type.Card,
            R.string.stripe_paymentsheet_payment_method_card,
            R.drawable.stripe_ic_paymentsheet_pm_card,
            true,
            CardRequirement,
            LayoutSpec(listOf(CardDetailsSectionSpec(), CardBillingSpec(), SaveForFutureUseSpec()))
        )

        /**
         * This is a list of the payment methods that we are allowing in the release
         */
        @VisibleForTesting
        internal val exposedPaymentMethods by lazy {
            listOf(
                PaymentMethod.Type.Card.code,
                PaymentMethod.Type.Bancontact.code,
                PaymentMethod.Type.Sofort.code,
                PaymentMethod.Type.Ideal.code,
                PaymentMethod.Type.SepaDebit.code,
                PaymentMethod.Type.Eps.code,
                PaymentMethod.Type.Giropay.code,
                PaymentMethod.Type.P24.code,
                PaymentMethod.Type.Klarna.code,
                PaymentMethod.Type.PayPal.code,
                PaymentMethod.Type.AfterpayClearpay.code,
                PaymentMethod.Type.USBankAccount.code,
                PaymentMethod.Type.Affirm.code,
                PaymentMethod.Type.AuBecsDebit.code
            )
        }
    }
}
