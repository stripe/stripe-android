package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.CountrySpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.FormSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.MandateTextSpec
import com.stripe.android.paymentsheet.elements.PaymentMethodSpec
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
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

private val sofortUserSelectedSave = FormSpec(
    LayoutSpec(
        listOf(
            sofortNameSection,
            sofortEmailSection,
            sofortCountrySection,
            SaveForFutureUseSpec(listOf(sofortNameSection, sofortEmailSection, sofortMandate)),
            sofortMandate,
        )
    ),
    // When saved this is a SEPA paymentMethod which requires SEPA requirements
    requirements = setOf(
        Requirement.DelayedSettlementSupport,
        Requirement.UserSelectableSave
    ).plus(sepaDebitUserSelectedSave.requirements)
)
private val sofortMerchantRequiredSave = FormSpec(
    LayoutSpec(
        listOf(
            sofortNameSection,
            sofortEmailSection,
            sofortCountrySection,
            sofortMandate,
        )
    ),
    // When saved this is a SEPA paymentMethod which requires SEPA requirements
    requirements = setOf(
        Requirement.DelayedSettlementSupport,
        Requirement.MerchantSelectedSave
    ).plus(sepaDebitMerchantRequiredSave.requirements)
)

private val sofortOneTimeUse = FormSpec(
    LayoutSpec(
        listOf(
            sofortCountrySection,
        )
    ),
    requirements = setOf(
        Requirement.DelayedSettlementSupport,
        Requirement.OneTimeUse
    ),
)

/**
 * This payment method is authenticated.
 */
internal val sofort = PaymentMethodSpec(
    sofortParamKey,
    listOf(
        sofortUserSelectedSave,
        sofortMerchantRequiredSave,
        sofortOneTimeUse,
    )
)
