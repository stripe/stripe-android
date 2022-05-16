package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

internal val idealNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val idealBankSection = SectionSpec(
    IdentifierSpec.Generic("bank_section"),
    DropdownSpec(
        IdentifierSpec.Generic("ideal[bank]"),
        R.string.ideal_bank,
        listOf(
            DropdownItemSpec(
                api_value = "abn_amro",
                display_text = "ABN Amro"
            ),
            DropdownItemSpec(
                api_value = "asn_bank",
                display_text = "ASN Bank"
            ),
            DropdownItemSpec(
                api_value = "bunq",
                display_text = "bunq B.V.‎"
            ),
            DropdownItemSpec(
                api_value = "handelsbanken",
                display_text = "Handelsbanken"
            ),
            DropdownItemSpec(
                api_value = "ing",
                display_text = "ING Bank"
            ),
            DropdownItemSpec(
                api_value = "knab",
                display_text = "Knab"
            ),
            DropdownItemSpec(
                api_value = "rabobank",
                display_text = "Rabobank"
            ),
            DropdownItemSpec(
                api_value = "regiobank",
                display_text = "RegioBank"
            ),
            DropdownItemSpec(
                api_value = "revolut",
                display_text = "Revolut"
            ),
            DropdownItemSpec(
                api_value = "sns_bank",
                display_text = "SNS Bank"
            ),
            DropdownItemSpec(
                api_value = "triodos_bank",
                display_text = "Triodos Bank"
            ),
            DropdownItemSpec(
                api_value = "van_lanschot",
                display_text = "Van Lanschot"
            ),
            DropdownItemSpec(
                api_value = null, // HIGHLIGHT
                display_text = "Other"
            )
        )
    )
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val IdealForm = LayoutSpec.create(
    idealNameSection,
    idealBankSection,
)
