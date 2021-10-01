package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
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

internal val bancontactParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "bancontact",
    "billing_details" to billingParams
)

internal val bancontactNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val bancontactEmailSection =
    SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)
internal val bancontactMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)
internal val bancontactUserSelectedSave = LayoutSpec.create(
    bancontactNameSection,
    bancontactEmailSection,
    SaveForFutureUseSpec(
        listOf(
            bancontactEmailSection, bancontactMandate
        )
    ),
    bancontactMandate,
)

internal val bancontactMerchantRequiredSave = LayoutSpec.create(
    bancontactNameSection,
    bancontactEmailSection,
    bancontactMandate,
)

internal val bancontactOneTimeUse = LayoutSpec.create(
    bancontactNameSection,
)

internal val bancontact = PaymentMethodFormSpec(
    bancontactParamKey,
    mapOf(
        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            // When saved this is a SEPA paymentMethod which requires SEPA requirements
            requirements = setOf(
                Requirement.DelayedPaymentMethodSupport,
                Requirement.Customer
            ).plus(sepaDebitReuseRequirements)
        ) to bancontactUserSelectedSave,

        // When saved this is a SEPA paymentMethod which requires SEPA requirements
        FormRequirement(
            SaveMode.SetupIntentOrPaymentIntentWithFutureUsageSet,
            requirements = setOf(
                Requirement.DelayedPaymentMethodSupport,
            ).plus(sepaDebitReuseRequirements)
        ) to bancontactMerchantRequiredSave,

        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            requirements = setOf(Requirement.DelayedPaymentMethodSupport)
        ) to bancontactOneTimeUse
    )
)
