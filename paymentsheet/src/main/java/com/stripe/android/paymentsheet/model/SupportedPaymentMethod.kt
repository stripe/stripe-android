package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.LayoutFormDescriptor
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.forms.AfterpayClearpayForm
import com.stripe.android.paymentsheet.forms.AfterpayClearpayParamKey
import com.stripe.android.paymentsheet.forms.AfterpayClearpayRequirement
import com.stripe.android.paymentsheet.forms.BancontactForm
import com.stripe.android.paymentsheet.forms.BancontactParamKey
import com.stripe.android.paymentsheet.forms.BancontactRequirement
import com.stripe.android.paymentsheet.forms.CardRequirement
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.paymentsheet.forms.EpsForm
import com.stripe.android.paymentsheet.forms.EpsParamKey
import com.stripe.android.paymentsheet.forms.EpsRequirement
import com.stripe.android.paymentsheet.forms.GiropayForm
import com.stripe.android.paymentsheet.forms.GiropayParamKey
import com.stripe.android.paymentsheet.forms.GiropayRequirement
import com.stripe.android.paymentsheet.forms.IdealForm
import com.stripe.android.paymentsheet.forms.IdealParamKey
import com.stripe.android.paymentsheet.forms.IdealRequirement
import com.stripe.android.paymentsheet.forms.KlarnaForm
import com.stripe.android.paymentsheet.forms.KlarnaParamKey
import com.stripe.android.paymentsheet.forms.KlarnaRequirement
import com.stripe.android.paymentsheet.forms.P24Form
import com.stripe.android.paymentsheet.forms.P24ParamKey
import com.stripe.android.paymentsheet.forms.P24Requirement
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.forms.Requirement
import com.stripe.android.paymentsheet.forms.SepaDebitForm
import com.stripe.android.paymentsheet.forms.SepaDebitParamKey
import com.stripe.android.paymentsheet.forms.SepaDebitRequirement
import com.stripe.android.paymentsheet.forms.SofortForm
import com.stripe.android.paymentsheet.forms.SofortParamKey
import com.stripe.android.paymentsheet.forms.SofortRequirement

/**
 * Enum defining all payment method types for which Payment Sheet can collect
 * payment data.
 *
 * FormSpec is optionally null only because Card is not converted to the
 * compose model.
 */
internal enum class SupportedPaymentMethod(
    val type: PaymentMethod.Type,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int,
    private val requirement: PaymentMethodRequirements,
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
        BancontactParamKey,
        BancontactForm
    ),

    Sofort(
        PaymentMethod.Type.Sofort,
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        SofortRequirement,
        SofortParamKey,
        SofortForm
    ),

    Ideal(
        PaymentMethod.Type.Ideal,
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        IdealRequirement,
        IdealParamKey,
        IdealForm
    ),

    SepaDebit(
        PaymentMethod.Type.SepaDebit,
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        SepaDebitRequirement,
        SepaDebitParamKey,
        SepaDebitForm
    ),

    Eps(
        PaymentMethod.Type.Eps,
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        EpsRequirement,
        EpsParamKey,
        EpsForm
    ),

    P24(
        PaymentMethod.Type.P24,
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        P24Requirement,
        P24ParamKey,
        P24Form
    ),

    Giropay(
        PaymentMethod.Type.Giropay,
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        GiropayRequirement,
        GiropayParamKey,
        GiropayForm
    ),
    AfterpayClearpay(
        PaymentMethod.Type.AfterpayClearpay,
        R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
        R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
        AfterpayClearpayRequirement,
        AfterpayClearpayParamKey,
        AfterpayClearpayForm
    ),
    Klarna(
        PaymentMethod.Type.Klarna,
        R.string.stripe_paymentsheet_payment_method_klarna,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        KlarnaRequirement,
        KlarnaParamKey,
        KlarnaForm
    );

    /**
     * This will get the form layout for the supported method that matches the top pick for the
     * payment method.  It should be known that this payment method has a form
     * that matches the capabilities already.
     */
    fun getPMAddForm(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = requireNotNull(getSpecWithFullfilledRequirements(stripeIntent, config))

    /**
     * This function will determine if there is a valid for the payment method
     * given the [PaymentSheet.Configuration] and the [StripeIntent]
     */
    internal fun getSpecWithFullfilledRequirements(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ): LayoutFormDescriptor? {
        val formSpec = formSpec
        val oneTimeUse = LayoutFormDescriptor(
            formSpec,
            showCheckbox = false,
            showCheckboxControlledFields = false
        )
        val merchantRequestedSave = LayoutFormDescriptor(
            formSpec,
            showCheckbox = false,
            showCheckboxControlledFields = true
        )
        val userSelectableSave = LayoutFormDescriptor(
            formSpec,
            showCheckbox = true,
            showCheckboxControlledFields = true
        )

        if (!stripeIntent.paymentMethodTypes.contains(type.code)) {
            return null
        }

        return when (stripeIntent) {
            is PaymentIntent -> {
                if ((stripeIntent.isSetupFutureUsageSet())) {
                    if (supportsPaymentIntentSfuSet(config)
                    ) {
                        merchantRequestedSave
                    } else {
                        null
                    }
                } else {
                    when {
                        supportsPaymentIntentSfuSettable(
                            stripeIntent,
                            config
                        )
                        -> userSelectableSave
                        supportsPaymentIntentSfuNotSettable(
                            config
                        ) -> oneTimeUse
                        else -> null
                    }
                }
            }
            is SetupIntent -> {
                when {
                    supportsSetupIntent(
                        config
                    ) -> merchantRequestedSave
                    else -> null
                }
            }
        }
    }

    /**
     * Returns true if the payment method supports confirming from a saved
     * payment method of this type.  See [PaymentMethodRequirements] for
     * description of the values
     */
    fun supportsCustomerSavedPM() = requirement.confirmPMFromCustomer == true

    /**
     * This checks to see if the payment method is supported from a SetupIntent.
     */
    private fun supportsSetupIntent(
        config: PaymentSheet.Configuration?
    ) = requirement.confirmPMFromCustomer == true &&
        checkRequirements(requirement.siRequirements, config)

    /**
     * This checks if there is support using this payment method when SFU
     * is already set in the PaymentIntent.  If SFU is set it must be possible
     * to confirm this payment method from a PM ID attached to a customer for
     * a consistent user experience.
     */
    private fun supportsPaymentIntentSfuSet(
        config: PaymentSheet.Configuration?
    ) = requirement.confirmPMFromCustomer == true &&
        checkRequirements(requirement.siRequirements, config) &&
        checkRequirements(requirement.piRequirements, config)

    /**
     * This detects if there is support with using this PM with the PI
     * where SFU is not settable by the user.
     */
    private fun supportsPaymentIntentSfuNotSettable(
        config: PaymentSheet.Configuration?
    ) = checkRequirements(requirement.piRequirements, config)

    /**
     * This checks to see if this PM is supported with the given
     * payment intent and configuration.
     *
     * The customer ID is required to be passed in the configuration
     * (the sdk cannot know if the PI has a customer ID associated with it),
     * so that we can guarantee to the user that the PM will be associated
     * with their customer object AND accessible when opening PaymentSheet
     * and seeing the saved PMs associate with their customer object.
     */
    private fun supportsPaymentIntentSfuSettable(
        stripeIntent: PaymentIntent,
        config: PaymentSheet.Configuration?
    ) = allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes) &&
        config?.customer != null &&
        requirement.confirmPMFromCustomer == true &&
        checkRequirements(requirement.piRequirements, config) &&
        checkRequirements(requirement.siRequirements, config)

    /**
     * Currently the only required requirement in the SDK is Delayed PMs
     */
    private fun checkRequirements(
        requirements: Set<Requirement>?,
        config: PaymentSheet.Configuration?
    ) =
        requirements?.map { requirement ->
            when (requirement) {
                Delayed -> config?.allowsDelayedPaymentMethods == true
            }
        }?.contains(false) == false

    /**
     * This checks that all PMs in the Intent have support for reuse.
     *
     * Currently a PaymentIntent can have multiple PaymentMethods allowed for confirm.
     * Some of those PaymentMethods may support setup_future_usage = off_session,
     * some might not. If a merchant creates a PaymentIntent with setup_future_usage
     * set to null, the user should be able to select if they want to save it (thus
     * setting setup_future_usage to off_session on confirm).  The problem is that
     * if all the PaymentMethods in the PaymentIntent do not support off_session
     * payments, the server will fail the confirmation.
     *
     * TODO: Fix when there is support on the server
     */
    private fun allHaveKnownReuseSupport(paymentMethodsInIntent: List<String?>): Boolean {
        // The following PaymentMethods are know to work when
        // PaymentIntent.setup_future_usage = on/off session
        // This list is different from the PaymentMethod.Type.isReusable
        // It is expected that this check will be removed soon
        val knownReusable = setOf(
            PaymentMethod.Type.Alipay.code,
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.SepaDebit.code,
            PaymentMethod.Type.AuBecsDebit.code,
            PaymentMethod.Type.Bancontact.code,
            PaymentMethod.Type.Sofort.code,
            PaymentMethod.Type.BacsDebit.code,
            PaymentMethod.Type.Ideal.code
        )
        return paymentMethodsInIntent.filterNot { knownReusable.contains(it) }.isEmpty()
    }

    override fun toString(): String {
        return type.code
    }

    companion object {
        /**
         * This is a list of the payment methods that we are allowing in the release
         */
        @VisibleForTesting
        internal val exposedPaymentMethods = listOf(
            Card, Bancontact, Sofort, Ideal, SepaDebit, Klarna
        )

        /**
         * This will use only those payment methods that are allowed in the release
         */
        fun fromCode(code: String?) =
            exposedPaymentMethods.firstOrNull { it.type.code == code }

        /**
         * Get the LPMS that are supported when used as a Customer Saved LPM given
         * the intent.
         */
        internal fun getSupportedSavedCustomerPMs(
            stripeIntent: StripeIntent?,
            config: PaymentSheet.Configuration?
        ) = stripeIntent?.paymentMethodTypes?.mapNotNull {
            fromCode(it)
        }?.filter { paymentMethod ->
            paymentMethod.supportsCustomerSavedPM() &&
                paymentMethod.getSpecWithFullfilledRequirements(stripeIntent, config) != null
        } ?: emptyList()

        /**
         * This will return a list of payment methods that have a supported form given
         * the [PaymentSheet.Configuration] and [StripeIntent].
         */
        internal fun getPMsToAdd(
            stripeIntent: StripeIntent?,
            config: PaymentSheet.Configuration?
        ) = stripeIntent?.paymentMethodTypes?.mapNotNull {
            fromCode(it)
        }?.filter { supportedPaymentMethod ->
            supportedPaymentMethod.getSpecWithFullfilledRequirements(
                stripeIntent,
                config
            ) != null
        }?.filterNot { supportedPaymentMethod ->
            stripeIntent.isLiveMode &&
                stripeIntent.unactivatedPaymentMethods.contains(supportedPaymentMethod.type.code)
        } ?: emptyList()
    }
}
