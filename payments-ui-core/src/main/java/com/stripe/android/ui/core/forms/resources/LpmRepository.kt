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
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.forms.AffirmRequirement
import com.stripe.android.paymentsheet.forms.AfterpayClearpayRequirement
import com.stripe.android.paymentsheet.forms.AlipayRequirement
import com.stripe.android.paymentsheet.forms.AmazonPayRequirement
import com.stripe.android.paymentsheet.forms.AuBecsDebitRequirement
import com.stripe.android.paymentsheet.forms.BancontactRequirement
import com.stripe.android.paymentsheet.forms.BlikRequirement
import com.stripe.android.paymentsheet.forms.CardRequirement
import com.stripe.android.paymentsheet.forms.CashAppPayRequirement
import com.stripe.android.paymentsheet.forms.EpsRequirement
import com.stripe.android.paymentsheet.forms.FpxRequirement
import com.stripe.android.paymentsheet.forms.GiropayRequirement
import com.stripe.android.paymentsheet.forms.GrabPayRequirement
import com.stripe.android.paymentsheet.forms.IdealRequirement
import com.stripe.android.paymentsheet.forms.KlarnaRequirement
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
import com.stripe.android.ui.core.CardBillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.ContactInformationSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SepaMandateTextSpec
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
    ): Boolean {
        val expectedLpms = stripeIntent.paymentMethodTypes
        var didParseServerResponse = false

        if (!serverLpmSpecs.isNullOrEmpty()) {
            val serverLpmObjects = LpmSerializer.deserializeList(serverLpmSpecs)
            didParseServerResponse = serverLpmObjects.isNotEmpty()
            update(stripeIntent, serverLpmObjects, cardBillingDetailsCollectionConfiguration)
        }

        // If the server does not return specs, or they are not parsed successfully
        // we will use the LPM on disk if found
        val lpmsNotParsedFromServerSpec = expectedLpms.filter { it !in lpmInitialFormData }

        if (lpmsNotParsedFromServerSpec.isNotEmpty()) {
            parseMissingLpmsFromDisk(
                missingLpms = lpmsNotParsedFromServerSpec,
                stripeIntent = stripeIntent,
                cardBillingDetailsCollectionConfiguration = cardBillingDetailsCollectionConfiguration,
            )
        }

        return didParseServerResponse
    }

    private fun parseMissingLpmsFromDisk(
        missingLpms: List<String>,
        stripeIntent: StripeIntent,
        cardBillingDetailsCollectionConfiguration: CardBillingDetailsCollectionConfiguration,
    ) {
        val missingLpmsOnDisk = readFromDisk().filter { it.type in missingLpms }

        val missingLpmsByType = missingLpmsOnDisk.mapNotNull { spec ->
            convertToSupportedPaymentMethod(
                stripeIntent = stripeIntent,
                sharedDataSpec = spec,
                cardBillingDetailsCollectionConfiguration = cardBillingDetailsCollectionConfiguration,
            )
        }.associateBy {
            it.code
        }

        lpmInitialFormData.putAll(missingLpmsByType)
    }

    /**
     * This method can be used to initialize the LpmRepository with the hardcoded card spec.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun initializeWithCardSpec(
        billingConfiguration: CardBillingDetailsCollectionConfiguration =
            CardBillingDetailsCollectionConfiguration(),
    ) {
        lpmInitialFormData.putAll(
            mapOf(
                PaymentMethod.Type.Card.code to hardcodedCardSpec(
                    billingDetailsCollectionConfiguration = billingConfiguration
                )
            )
        )
    }

    @VisibleForTesting
    fun updateFromDisk(stripeIntent: StripeIntent) {
        update(stripeIntent, readFromDisk())
    }

    private fun readFromDisk(): List<SharedDataSpec> {
        return parseLpms(arguments.resources?.assets?.open("lpms.json"))
    }

    private fun update(
        stripeIntent: StripeIntent,
        specs: List<SharedDataSpec>,
        cardBillingDetailsCollectionConfiguration: CardBillingDetailsCollectionConfiguration =
            CardBillingDetailsCollectionConfiguration(),
    ) {
        val validSpecs = specs.filterNot { spec ->
            !arguments.isFinancialConnectionsAvailable() && spec.type == USBankAccount.code
        }

        // By mapNotNull we will not accept any LPMs that are not known by the platform.
        val supportedPaymentMethods = validSpecs.mapNotNull { spec ->
            convertToSupportedPaymentMethod(
                stripeIntent = stripeIntent,
                sharedDataSpec = spec,
                cardBillingDetailsCollectionConfiguration = cardBillingDetailsCollectionConfiguration,
            )
        }

        val supportedPaymentMethodsByType = supportedPaymentMethods.associateBy { it.code }
        lpmInitialFormData.putAll(supportedPaymentMethodsByType)

        val nextActionSpecsByType = validSpecs.associate { spec ->
            spec.type to spec.nextActionSpec.transform()
        }
        lpmPostConfirmData.update(nextActionSpecsByType)
    }

    private fun parseLpms(inputStream: InputStream?): List<SharedDataSpec> {
        return getJsonStringFromInputStream(inputStream)?.let { string ->
            LpmSerializer.deserializeList(string)
        }.orEmpty()
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
            displayNameResource = R.string.stripe_paymentsheet_payment_method_bancontact,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bancontact,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = BancontactRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields + stripeIntent.sepaMandateLayout())
        )
        PaymentMethod.Type.Sofort.code -> SupportedPaymentMethod(
            code = "sofort",
            requiresMandate = true,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_sofort,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = SofortRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields + stripeIntent.sepaMandateLayout(requireName = true))
        )
        PaymentMethod.Type.Ideal.code -> {
            val isSfu = (stripeIntent as? PaymentIntent)?.setupFutureUsage != null
            val isSetupIntent = stripeIntent is SetupIntent
            val localLayoutSpecs: List<FormItemSpec> = if (isSfu || isSetupIntent) {
                listOf(
                    EmailSpec(),
                    SepaMandateTextSpec()
                )
            } else {
                emptyList()
            }

            SupportedPaymentMethod(
                code = "ideal",
                requiresMandate = true,
                displayNameResource = R.string.stripe_paymentsheet_payment_method_ideal,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_ideal,
                lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
                darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
                tintIconOnSelection = false,
                requirement = IdealRequirement,
                formSpec = LayoutSpec(sharedDataSpec.fields + localLayoutSpecs)
            )
        }
        PaymentMethod.Type.SepaDebit.code -> SupportedPaymentMethod(
            code = "sepa_debit",
            requiresMandate = true,
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
            displayNameResource = R.string.stripe_paymentsheet_payment_method_revolut_pay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_revolut_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = RevolutPayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.AmazonPay.code -> SupportedPaymentMethod(
            code = "amazon_pay",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_amazon_pay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_amazon_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = AmazonPayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        PaymentMethod.Type.MobilePay.code -> SupportedPaymentMethod(
            code = "mobilepay",
            requiresMandate = false,
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
            displayNameResource = R.string.stripe_paymentsheet_payment_method_au_becs_debit,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = true,
            requirement = AuBecsDebitRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
        USBankAccount.code -> {
            val pmo = stripeIntent.getPaymentMethodOptions()[USBankAccount.code]
            val verificationMethod = (pmo as? Map<*, *>)?.get("verification_method") as? String
            val supportsVerificationMethod = verificationMethod in setOf("instant", "automatic")

            /**
             * US Bank Account requires the payment method option to have either automatic or
             * instant verification method in order to be a supported payment method. For example,
             * the intent should include:
             * {
             *     "payment_method_options" : {
             *         "us_bank_account": {
             *             "verification_method": "automatic"
             *         }
             *     }
             * }
             */
            if (supportsVerificationMethod) {
                SupportedPaymentMethod(
                    code = "us_bank_account",
                    requiresMandate = true,
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
            displayNameResource = R.string.stripe_paymentsheet_payment_method_upi,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_upi,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = UpiRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )

        PaymentMethod.Type.Blik.code -> SupportedPaymentMethod(
            code = "blik",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_blik,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_blik,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = BlikRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )

        PaymentMethod.Type.CashAppPay.code -> SupportedPaymentMethod(
            code = "cashapp",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_cashapp,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_cash_app_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = CashAppPayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
        )
        PaymentMethod.Type.GrabPay.code -> SupportedPaymentMethod(
            code = "grabpay",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_grabpay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_grabpay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = GrabPayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
        )
        PaymentMethod.Type.Fpx.code -> SupportedPaymentMethod(
            code = "fpx",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_fpx,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_fpx,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = FpxRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
        )
        PaymentMethod.Type.Alipay.code -> SupportedPaymentMethod(
            code = "alipay",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_alipay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_alipay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = AlipayRequirement,
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

        operator fun contains(key: String) = codeToSupportedPaymentMethod.containsKey(key)

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
    data class LpmRepositoryArguments(
        val resources: Resources?,
        val isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable =
            DefaultIsFinancialConnectionsAvailable(),
    )
}

private fun StripeIntent.payPalRequiresMandate(): Boolean {
    return when (this) {
        is PaymentIntent -> setupFutureUsage != null
        is SetupIntent -> true
    }
}

private fun StripeIntent.sepaMandateLayout(
    requireName: Boolean = false,
): List<FormItemSpec> {
    val isSfu = (this as? PaymentIntent)?.setupFutureUsage != null
    val isSetupIntent = this is SetupIntent
    return if (isSfu || isSetupIntent) {
        listOfNotNull(
            if (requireName) {
                NameSpec()
            } else {
                null
            },
            EmailSpec(),
            SepaMandateTextSpec()
        )
    } else {
        emptyList()
    }
}
