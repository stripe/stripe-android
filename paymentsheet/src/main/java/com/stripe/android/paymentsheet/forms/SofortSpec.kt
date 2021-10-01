package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.CountrySpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.FormRequirement
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.MandateTextSpec
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SaveMode
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.billingParams

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

internal val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

internal val sofortNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val sofortEmailSection = SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)
internal val sofortCountrySection =
    SectionSpec(
        IdentifierSpec.Generic("country_section"),
        CountrySpec(setOf("AT", "BE", "DE", "ES", "IT", "NL"))
    )
internal val sofortMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)

private val sofortUserSelectedSave = LayoutSpec.create(
        sofortNameSection,
        sofortEmailSection,
        sofortCountrySection,
        SaveForFutureUseSpec(listOf(sofortNameSection, sofortEmailSection, sofortMandate)),
        sofortMandate,
)

private val sofortMerchantRequiredSave = LayoutSpec.create(
        sofortNameSection,
        sofortEmailSection,
        sofortCountrySection,
        sofortMandate,
)

private val sofortOneTimeUse = LayoutSpec.create(
        sofortCountrySection,
)

/**
 * This payment method is authenticated.
 */
internal val sofort = PaymentMethodFormSpec(
    sofortParamKey,
    mapOf(
        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            // When saved this is a SEPA paymentMethod which requires SEPA requirements
            requirements = setOf(
                Requirement.DelayedPaymentMethodSupport,
                Requirement.Customer
            ).plus(sepaDebitReuseRequirements)
        ) to sofortUserSelectedSave,

        // When saved this is a SEPA paymentMethod which requires SEPA requirements
        FormRequirement(
            SaveMode.SetupIntentOrPaymentIntentWithFutureUsageSet,
            requirements = setOf(
                Requirement.DelayedPaymentMethodSupport,
            ).plus(sepaDebitReuseRequirements)
        ) to sofortMerchantRequiredSave,

        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            requirements = setOf(Requirement.DelayedPaymentMethodSupport)
        ) to sofortOneTimeUse,
    )
)
