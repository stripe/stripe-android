package com.stripe.android.ui.core.forms.resources

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.LuxePostConfirmActionRepository
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.forms.AffirmRequirement
import com.stripe.android.paymentsheet.forms.AfterpayClearpayRequirement
import com.stripe.android.paymentsheet.forms.AuBecsDebitRequirement
import com.stripe.android.paymentsheet.forms.BancontactRequirement
import com.stripe.android.paymentsheet.forms.CardRequirement
import com.stripe.android.paymentsheet.forms.CashAppPayRequirement
import com.stripe.android.paymentsheet.forms.EpsRequirement
import com.stripe.android.paymentsheet.forms.GiropayRequirement
import com.stripe.android.paymentsheet.forms.IdealRequirement
import com.stripe.android.paymentsheet.forms.KlarnaRequirement
import com.stripe.android.paymentsheet.forms.MandateRequirement
import com.stripe.android.paymentsheet.forms.MobilePayRequirement
import com.stripe.android.paymentsheet.forms.P24Requirement
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.forms.PaypalRequirement
import com.stripe.android.paymentsheet.forms.RevolutPayRequirement
import com.stripe.android.paymentsheet.forms.SepaDebitRequirement
import com.stripe.android.paymentsheet.forms.SofortRequirement
import com.stripe.android.paymentsheet.forms.USBankAccountRequirement
import com.stripe.android.paymentsheet.forms.UpiRequirement
import com.stripe.android.paymentsheet.forms.ZipRequirement
import com.stripe.android.ui.core.BuildConfig
import com.stripe.android.ui.core.CardBillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.ContactInformationSpec
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.ui.core.elements.transform
import java.io.InputStream

/**
 * This class is responsible for loading the LPM UI Specification for all LPMs, and returning
 * a particular requested LPM.
 *
 * This is not injected as a singleton because when the activity is killed
 * the FormViewModel and SheetViewModel don't share the Dagger graph and the
 * repository is not a singleton.  Additionally every time you create a new
 * form view model a new repository is created and thus needs to be initialized.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LpmRepository constructor(
    private val arguments: LpmRepositoryArguments,
    private val lpmInitialFormData: LpmInitialFormData = LpmInitialFormData.Instance,
    private val lpmPostConfirmData: LuxePostConfirmActionRepository = LuxePostConfirmActionRepository.Instance
) {
    private val lpmSerializer = LpmSerializer()

    var serverSpecLoadingState: ServerSpecState = ServerSpecState.Uninitialized

    val supportedPaymentMethodTypes: List<String> by lazy {
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
            PaymentMethod.Type.RevolutPay.code,
            PaymentMethod.Type.MobilePay.code,
            PaymentMethod.Type.Zip.code,
            PaymentMethod.Type.AuBecsDebit.code,
            PaymentMethod.Type.Upi.code,
            PaymentMethod.Type.CashAppPay.code,
        )
    }

    fun fromCode(code: PaymentMethodCode?) = lpmInitialFormData.fromCode(code)

    fun values(): List<SupportedPaymentMethod> = lpmInitialFormData.values()

    /**
     * This method will read the [StripeIntent] and their specs as two separate parameters.
     * Any spec not found from the server will be read from disk json file.
     *
     * It is still possible that an lpm that is expected cannot be read successfully from
     * the json spec on disk or the server spec.
     *
     * It is also possible that an LPM is present in the repository that is not present
     * in the expected LPM list.
     *
     * Reading the server spec is all or nothing, any error means none will be read
     * so it is important that the json on disk is successful.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun update(
        stripeIntent: StripeIntent,
        serverLpmSpecs: String?,
        cardBillingDetailsCollectionConfiguration: CardBillingDetailsCollectionConfiguration =
            CardBillingDetailsCollectionConfiguration(),
    ) {
        val expectedLpms = stripeIntent.paymentMethodTypes

        serverSpecLoadingState = ServerSpecState.NoServerSpec(serverLpmSpecs)
        if (!serverLpmSpecs.isNullOrEmpty()) {
            serverSpecLoadingState = ServerSpecState.ServerNotParsed(serverLpmSpecs)
            val serverLpmObjects = lpmSerializer.deserializeList(serverLpmSpecs)
            if (serverLpmObjects.isNotEmpty()) {
                serverSpecLoadingState = ServerSpecState.ServerParsed(serverLpmSpecs)
            }
            update(stripeIntent, serverLpmObjects, cardBillingDetailsCollectionConfiguration)
        }

        // If the server does not return specs, or they are not parsed successfully
        // we will use the LPM on disk if found
        val lpmsNotParsedFromServerSpec = expectedLpms
            .filter { !lpmInitialFormData.containsKey(it) }
            .filter { supportsPaymentMethod(it) }

        if (lpmsNotParsedFromServerSpec.isNotEmpty()) {
            val mapFromDisk: Map<String, SharedDataSpec>? =
                readFromDisk()
                    ?.associateBy { it.type }
                    ?.filterKeys { expectedLpms.contains(it) }
            lpmInitialFormData.putAll(
                lpmsNotParsedFromServerSpec
                    .mapNotNull { mapFromDisk?.get(it) }
                    .mapNotNull {
                        convertToSupportedPaymentMethod(stripeIntent, it, cardBillingDetailsCollectionConfiguration)
                    }
                    .associateBy { it.code }
            )
            mapFromDisk
                ?.mapValues { it.value.nextActionSpec.transform() }
                ?.let {
                    lpmPostConfirmData.update(
                        it
                    )
                }
        }
    }

    @VisibleForTesting
    fun updateFromDisk(stripeIntent: StripeIntent) {
        update(stripeIntent, readFromDisk())
    }

    private fun readFromDisk() = parseLpms(arguments.resources?.assets?.open("lpms.json"))

    private fun update(
        stripeIntent: StripeIntent,
        lpms: List<SharedDataSpec>?,
        cardBillingDetailsCollectionConfiguration: CardBillingDetailsCollectionConfiguration =
            CardBillingDetailsCollectionConfiguration(),
    ) {
        val parsedSharedData = lpms
            ?.filter { supportsPaymentMethod(it.type) }
            ?.filterNot {
                !arguments.isFinancialConnectionsAvailable() &&
                    it.type == PaymentMethod.Type.USBankAccount.code
            }

        // By mapNotNull we will not accept any LPMs that are not known by the platform.
        parsedSharedData
            ?.mapNotNull {
                convertToSupportedPaymentMethod(stripeIntent, it, cardBillingDetailsCollectionConfiguration)
            }
            ?.toMutableList()
            ?.let {
                lpmInitialFormData.putAll(
                    it.associateBy { it.code }
                )
            }

        // Here nextActionSpec if null will convert to an explicit internal next action and status.
        parsedSharedData
            ?.associate { it.type to it.nextActionSpec.transform() }
            ?.let {
                lpmPostConfirmData.update(
                    it
                )
            }
    }

    private fun supportsPaymentMethod(paymentMethodCode: String): Boolean {
        return supportedPaymentMethodTypes.contains(paymentMethodCode)
    }

    private fun parseLpms(inputStream: InputStream?) =
        getJsonStringFromInputStream(inputStream)?.let { string ->
            lpmSerializer.deserializeList(string)
        }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }

    private fun convertToSupportedPaymentMethod(
        stripeIntent: StripeIntent,
        sharedDataSpec: SharedDataSpec,
        cardBillingDetailsCollectionConfiguration: CardBillingDetailsCollectionConfiguration,
    ) = when (sharedDataSpec.type) {
        PaymentMethod.Type.Card.code -> SupportedPaymentMethod(
            code = "card",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = true,
            requirement = CardRequirement,
            formSpec = if (sharedDataSpec.fields.isEmpty() || sharedDataSpec.fields == listOf(EmptyFormSpec)) {
                hardcodedCardSpec(cardBillingDetailsCollectionConfiguration).formSpec
            } else {
                LayoutSpec(sharedDataSpec.fields)
            }
        )
        PaymentMethod.Type.Bancontact.code -> SupportedPaymentMethod(
            code = "bancontact",
            requiresMandate = true,
            mandateRequirement = MandateRequirement.Always,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_bancontact,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bancontact,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = BancontactRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.Sofort.code -> SupportedPaymentMethod(
            code = "sofort",
            requiresMandate = true,
            mandateRequirement = MandateRequirement.Always,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_sofort,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = SofortRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.Ideal.code -> SupportedPaymentMethod(
            code = "ideal",
            requiresMandate = true,
            mandateRequirement = MandateRequirement.Always,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_ideal,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_ideal,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = IdealRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.SepaDebit.code -> SupportedPaymentMethod(
            code = "sepa_debit",
            requiresMandate = true,
            mandateRequirement = MandateRequirement.Always,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_sepa_debit,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = SepaDebitRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.Eps.code -> SupportedPaymentMethod(
            code = "eps",
            requiresMandate = true,
            mandateRequirement = MandateRequirement.Always,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_eps,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_eps,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = EpsRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.P24.code -> SupportedPaymentMethod(
            code = "p24",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_p24,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_p24,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = P24Requirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.Giropay.code -> SupportedPaymentMethod(
            code = "giropay",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_giropay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_giropay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = GiropayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.AfterpayClearpay.code -> SupportedPaymentMethod(
            code = "afterpay_clearpay",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = if (isClearpay()) {
                R.string.stripe_paymentsheet_payment_method_clearpay
            } else {
                R.string.stripe_paymentsheet_payment_method_afterpay
            },
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = AfterpayClearpayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.Klarna.code -> SupportedPaymentMethod(
            code = "klarna",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = KlarnaRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.PayPal.code -> {
            val localLayoutSpecs: List<FormItemSpec> = if (stripeIntent.payPalRequiresMandate()) {
                listOf(MandateTextSpec(stringResId = R.string.stripe_paypal_mandate))
            } else {
                emptyList()
            }

            SupportedPaymentMethod(
                code = "paypal",
                requiresMandate = stripeIntent.payPalRequiresMandate(),
                mandateRequirement = MandateRequirement.Dynamic,
                displayNameResource = R.string.stripe_paymentsheet_payment_method_paypal,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_paypal,
                lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
                darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
                tintIconOnSelection = false,
                requirement = PaypalRequirement,
                formSpec = LayoutSpec(sharedDataSpec.fields + localLayoutSpecs)
            )
        }
        PaymentMethod.Type.Affirm.code -> SupportedPaymentMethod(
            code = "affirm",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_affirm,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_affirm,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = AffirmRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.RevolutPay.code -> SupportedPaymentMethod(
            code = "revolut_pay",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_revolut_pay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_revolut_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = RevolutPayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.MobilePay.code -> SupportedPaymentMethod(
            code = "mobilepay",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_mobile_pay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_mobile_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = MobilePayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.Zip.code -> SupportedPaymentMethod(
            code = "zip",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_zip,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_zip,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = ZipRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.AuBecsDebit.code -> SupportedPaymentMethod(
            code = "au_becs_debit",
            requiresMandate = true,
            mandateRequirement = MandateRequirement.Always,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_au_becs_debit,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = true,
            requirement = AuBecsDebitRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.USBankAccount.code -> {
            val pmo = stripeIntent.getPaymentMethodOptions()[PaymentMethod.Type.USBankAccount.code]
            val verificationMethod = (pmo as? Map<*, *>)?.get("verification_method") as? String
            val supportsVerificationMethod = verificationMethod == "instant" ||
                verificationMethod == "automatic"
            val supported =
                (stripeIntent.clientSecret != null || arguments.enableACHV2InDeferredFlow) &&
                    supportsVerificationMethod
            if (supported) {
                SupportedPaymentMethod(
                    code = "us_bank_account",
                    requiresMandate = true,
                    mandateRequirement = MandateRequirement.Always,
                    displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
                    iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
                    lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
                    darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
                    tintIconOnSelection = true,
                    requirement = USBankAccountRequirement,
                    formSpec = LayoutSpec(sharedDataSpec.fields)
                )
            } else {
                null
            }
        }
        PaymentMethod.Type.Upi.code -> SupportedPaymentMethod(
            code = "upi",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_upi,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_upi,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = UpiRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.CashAppPay.code -> SupportedPaymentMethod(
            code = "cashapp",
            requiresMandate = false,
            mandateRequirement = MandateRequirement.Never,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_cashapp,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_cash_app_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = CashAppPayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
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
        val code: PaymentMethodCode,

        /** This describes if the LPM requires a mandate see [ConfirmPaymentIntentParams.mandateDataParams]. */
        val requiresMandate: Boolean,

        /** This describes when the LPM requires a mandate (see [ConfirmPaymentIntentParams.mandateDataParams]). */
        val mandateRequirement: MandateRequirement,

        /** This describes the name that appears under the selector. */
        @StringRes val displayNameResource: Int,

        /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
        @DrawableRes val iconResource: Int,

        /** An optional light theme icon url if it's supported. */
        val lightThemeIconUrl: String?,

        /** An optional dark theme icon url if it's supported. */
        val darkThemeIconUrl: String?,

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
        val formSpec: LayoutSpec
    ) {
        /**
         * Returns true if the payment method supports confirming from a saved
         * payment method of this type.  See [PaymentMethodRequirements] for
         * description of the values
         */
        fun supportsCustomerSavedPM() = requirement.getConfirmPMFromCustomer(code)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    class LpmInitialFormData {

        private var codeToSupportedPaymentMethod = mutableMapOf<String, SupportedPaymentMethod>()

        fun values(): List<SupportedPaymentMethod> = codeToSupportedPaymentMethod.values.toList()

        fun fromCode(code: String?) = code?.let { paymentMethodCode ->
            codeToSupportedPaymentMethod[paymentMethodCode]
        }

        fun containsKey(it: String) = codeToSupportedPaymentMethod.containsKey(it)

        fun putAll(map: Map<PaymentMethodCode, SupportedPaymentMethod>) {
            codeToSupportedPaymentMethod.putAll(map)
        }

        internal companion object {
            val Instance = LpmInitialFormData()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LpmRepository? = null
        fun getInstance(args: LpmRepositoryArguments): LpmRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LpmRepository(args).also { INSTANCE = it }
            }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val HardcodedCard = hardcodedCardSpec(CardBillingDetailsCollectionConfiguration())

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun hardcodedCardSpec(
            billingDetailsCollectionConfiguration: CardBillingDetailsCollectionConfiguration
        ): SupportedPaymentMethod {
            val specs = listOfNotNull(
                ContactInformationSpec(
                    collectName = false,
                    collectEmail = billingDetailsCollectionConfiguration.collectEmail,
                    collectPhone = billingDetailsCollectionConfiguration.collectPhone,
                ),
                CardDetailsSectionSpec(
                    collectName = billingDetailsCollectionConfiguration.collectName,
                ),
                CardBillingSpec(
                    collectionMode = billingDetailsCollectionConfiguration.address,
                ).takeIf {
                    billingDetailsCollectionConfiguration.collectAddress
                },
                SaveForFutureUseSpec(),
            )
            return SupportedPaymentMethod(
                code = "card",
                requiresMandate = false,
                mandateRequirement = MandateRequirement.Never,
                displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                tintIconOnSelection = true,
                requirement = CardRequirement,
                formSpec = LayoutSpec(specs),
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class ServerSpecState(val serverLpmSpecs: String?) {
        object Uninitialized : ServerSpecState(null)
        class NoServerSpec(serverLpmSpecs: String?) : ServerSpecState(serverLpmSpecs)
        class ServerParsed(serverLpmSpecs: String?) : ServerSpecState(serverLpmSpecs)
        class ServerNotParsed(serverLpmSpecs: String?) : ServerSpecState(serverLpmSpecs)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class LpmRepositoryArguments(
        val resources: Resources?,
        val isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable =
            DefaultIsFinancialConnectionsAvailable(),
        // Whether to enable ACHv2 in the deferred flow.
        // To be deleted when https://jira.corp.stripe.com/browse/BANKCON-6731 is completed.
        val enableACHV2InDeferredFlow: Boolean = BuildConfig.DEBUG,
    )
}

private fun StripeIntent.payPalRequiresMandate(): Boolean {
    return when (this) {
        is PaymentIntent -> setupFutureUsage != null
        is SetupIntent -> true
    }
}
