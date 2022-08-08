package com.stripe.android.ui.core.forms.resources

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
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
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    private val arguments: LpmRepositoryArguments
) {
    private val lpmSerializer = LpmSerializer()
    private val serverInitializedLatch = CountDownLatch(1)
    var serverSpecLoadingState: ServerSpecState = ServerSpecState.Uninitialized

    private var codeToSupportedPaymentMethod = mutableMapOf<String, SupportedPaymentMethod>()

    fun values() = codeToSupportedPaymentMethod.values

    fun fromCode(code: String?) = code?.let { paymentMethodCode ->
        codeToSupportedPaymentMethod[paymentMethodCode]
    }

    fun isLoaded() = serverInitializedLatch.count <= 0L

    fun waitUntilLoaded() {
        serverInitializedLatch.await(20, TimeUnit.SECONDS)
    }

    /**
     * This method will read the expected LPMs and their specs as two separate parameters.
     * Any spec now found from the server will be read from disk json file.
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
        expectedLpms: List<String>,
        serverLpmSpecs: String?
    ) = internalUpdate(expectedLpms, serverLpmSpecs, false)

    @VisibleForTesting
    fun forceUpdate(
        expectedLpms: List<String>,
        serverLpmSpecs: String?
    ) = internalUpdate(expectedLpms, serverLpmSpecs, true)

    /**
     * Will add the server specs to the repository and load the ones from disk if
     * server is not parseable.
     */
    private fun internalUpdate(
        expectedLpms: List<String>,
        serverLpmSpecs: String?,
        force: Boolean = false
    ) {
        // If the expectedLpms is different form last time, we still need to reload.
        var lpmsNotParsedFromServerSpec = expectedLpms
            .filter { !codeToSupportedPaymentMethod.containsKey(it) }
        if (!isLoaded() || force || lpmsNotParsedFromServerSpec.isNotEmpty()) {
            serverSpecLoadingState = ServerSpecState.NoServerSpec(serverLpmSpecs)
            if (!serverLpmSpecs.isNullOrEmpty()) {
                serverSpecLoadingState = ServerSpecState.ServerNotParsed(serverLpmSpecs)
                val serverLpmObjects = lpmSerializer.deserializeList(serverLpmSpecs)
                if (serverLpmObjects.isNotEmpty()) {
                    serverSpecLoadingState = ServerSpecState.ServerParsed(serverLpmSpecs)
                }
                update(serverLpmObjects)
            }

            // If the server does not return specs, or they are not parsed successfully
            // we will use the LPM on disk if found
            lpmsNotParsedFromServerSpec = expectedLpms
                .filter { !codeToSupportedPaymentMethod.containsKey(it) }
            if (lpmsNotParsedFromServerSpec.isNotEmpty()) {
                val mapFromDisk: Map<String, SharedDataSpec>? =
                    readFromDisk()
                        ?.associateBy { it.type }
                        ?.filterKeys { expectedLpms.contains(it) }
                codeToSupportedPaymentMethod.putAll(
                    lpmsNotParsedFromServerSpec
                        .mapNotNull { mapFromDisk?.get(it) }
                        .mapNotNull { convertToSupportedPaymentMethod(it) }
                        .associateBy { it.code }
                )
            }

            serverInitializedLatch.countDown()
        }
    }

    @VisibleForTesting
    fun updateFromDisk() {
        update(readFromDisk())
    }

    private fun readFromDisk() =
        parseLpms(arguments.resources?.assets?.open("lpms.json"))

    private fun update(lpms: List<SharedDataSpec>?) {
        // By mapNotNull we will not accept any LPMs that are not known by the platform.
        val parsedSupportedPaymentMethod = lpms
            ?.filter { exposedPaymentMethods.contains(it.type) }
            ?.mapNotNull { convertToSupportedPaymentMethod(it) }
            ?.toMutableList()

        parsedSupportedPaymentMethod?.removeAll {
            !arguments.isFinancialConnectionsAvailable() &&
                it.code == PaymentMethod.Type.USBankAccount.code
        }

        codeToSupportedPaymentMethod.putAll(
            parsedSupportedPaymentMethod?.associateBy { it.code } ?: emptyMap()
        )
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
                "card",
                false,
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
                "bancontact",
                true,
                R.string.stripe_paymentsheet_payment_method_bancontact,
                R.drawable.stripe_ic_paymentsheet_pm_bancontact,
                false,
                BancontactRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Sofort.code -> SupportedPaymentMethod(
                "sofort",
                true,
                R.string.stripe_paymentsheet_payment_method_sofort,
                R.drawable.stripe_ic_paymentsheet_pm_klarna,
                false,
                SofortRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Ideal.code -> SupportedPaymentMethod(
                "ideal",
                true,
                R.string.stripe_paymentsheet_payment_method_ideal,
                R.drawable.stripe_ic_paymentsheet_pm_ideal,
                false,
                IdealRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.SepaDebit.code -> SupportedPaymentMethod(
                "sepa_debit",
                true,
                R.string.stripe_paymentsheet_payment_method_sepa_debit,
                R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
                false,
                SepaDebitRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Eps.code -> SupportedPaymentMethod(
                "eps",
                true,
                R.string.stripe_paymentsheet_payment_method_eps,
                R.drawable.stripe_ic_paymentsheet_pm_eps,
                false,
                EpsRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.P24.code -> SupportedPaymentMethod(
                "p24",
                false,
                R.string.stripe_paymentsheet_payment_method_p24,
                R.drawable.stripe_ic_paymentsheet_pm_p24,
                false,
                P24Requirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Giropay.code -> SupportedPaymentMethod(
                "giropay",
                false,
                R.string.stripe_paymentsheet_payment_method_giropay,
                R.drawable.stripe_ic_paymentsheet_pm_giropay,
                false,
                GiropayRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.AfterpayClearpay.code -> SupportedPaymentMethod(
                "afterpay_clearpay",
                false,
                if (isClearpay()) {
                    R.string.stripe_paymentsheet_payment_method_clearpay
                } else {
                    R.string.stripe_paymentsheet_payment_method_afterpay
                },
                R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
                false,
                AfterpayClearpayRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Klarna.code -> SupportedPaymentMethod(
                "klarna",
                false,
                R.string.stripe_paymentsheet_payment_method_klarna,
                R.drawable.stripe_ic_paymentsheet_pm_klarna,
                false,
                KlarnaRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.PayPal.code -> SupportedPaymentMethod(
                "paypal",
                false,
                R.string.stripe_paymentsheet_payment_method_paypal,
                R.drawable.stripe_ic_paymentsheet_pm_paypal,
                false,
                PaypalRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.Affirm.code -> SupportedPaymentMethod(
                "affirm",
                false,
                R.string.stripe_paymentsheet_payment_method_affirm,
                R.drawable.stripe_ic_paymentsheet_pm_affirm,
                false,
                AffirmRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.AuBecsDebit.code -> SupportedPaymentMethod(
                "au_becs_debit",
                true,
                R.string.stripe_paymentsheet_payment_method_au_becs_debit,
                R.drawable.stripe_ic_paymentsheet_pm_bank,
                true,
                AuBecsDebitRequirement,
                LayoutSpec(sharedDataSpec.fields)
            )
            PaymentMethod.Type.USBankAccount.code -> SupportedPaymentMethod(
                "us_bank_account",
                true,
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
        val code: PaymentMethodCode,

        /** This describes if the LPM requires a mandate see [ConfirmPaymentIntentParams.mandateDataParams]. */
        val requiresMandate: Boolean,

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
        val formSpec: LayoutSpec
    ) {
        /**
         * Returns true if the payment method supports confirming from a saved
         * payment method of this type.  See [PaymentMethodRequirements] for
         * description of the values
         */
        fun supportsCustomerSavedPM() = requirement.getConfirmPMFromCustomer(code)
    }

    companion object {
        @Volatile
        private var INSTANCE: LpmRepository? = null
        fun getInstance(args: LpmRepositoryArguments): LpmRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LpmRepository(args).also { INSTANCE = it }
            }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val HardcodedCard = SupportedPaymentMethod(
            "card",
            false,
            R.string.stripe_paymentsheet_payment_method_card,
            R.drawable.stripe_ic_paymentsheet_pm_card,
            true,
            CardRequirement,
            LayoutSpec(listOf(CardDetailsSectionSpec(), CardBillingSpec(), SaveForFutureUseSpec()))
        )

        /**
         * This is a list of the payment methods that we are allowing in the release
         */
        val exposedPaymentMethods by lazy {
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
            DefaultIsFinancialConnectionsAvailable()
    )
}
