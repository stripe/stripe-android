package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
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
internal val bancontactUserSelectedSave = FormSpec(
    LayoutSpec(
        listOf(
            bancontactNameSection,
            bancontactEmailSection,
            SaveForFutureUseSpec(
                listOf(
                    bancontactEmailSection, bancontactMandate
                )
            ),
            bancontactMandate,
        )
    ),
    // When saved this is a SEPA paymentMethod which requires SEPA requirements
    requirements = sepaDebitUserSelectedSave.requirements
        .plus(Requirement.UserSelectableSave),
)
internal val bancontactMerchantRequiredSave = FormSpec(
    LayoutSpec(
        listOf(
            bancontactNameSection,
            bancontactEmailSection,
            bancontactMandate,
        )
    ),
    // When saved this is a SEPA paymentMethod which requires SEPA requirements
    requirements = sepaDebitMerchantRequiredSave.requirements
        .plus(Requirement.MerchantRequiresSave),
)
internal val bancontactOneTimeUse = FormSpec(
    LayoutSpec(
        listOf(
            bancontactNameSection,
        )
    ),
    requirements = setOf(
        Requirement.OneTimeUse
    )
)

internal val bancontact = PaymentMethodSpec(
    bancontactParamKey,
    listOf(
        bancontactUserSelectedSave,
        bancontactMerchantRequiredSave,
        bancontactOneTimeUse
    )
)
