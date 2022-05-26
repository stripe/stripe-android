package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.TranslationId

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val IdealForm = LayoutSpec.create(
    NameSpec(),
    DropdownSpec(
        IdentifierSpec.Generic("ideal[bank]"),
        TranslationId.IdealBank,
        listOf(
            DropdownItemSpec(
                apiValue = "abn_amro",
                displayText = "ABN Amro"
            ),
            DropdownItemSpec(
                apiValue = "asn_bank",
                displayText = "ASN Bank"
            ),
            DropdownItemSpec(
                apiValue = "bunq",
                displayText = "bunq B.V.‎"
            ),
            DropdownItemSpec(
                apiValue = "handelsbanken",
                displayText = "Handelsbanken"
            ),
            DropdownItemSpec(
                apiValue = "ing",
                displayText = "ING Bank"
            ),
            DropdownItemSpec(
                apiValue = "knab",
                displayText = "Knab"
            ),
            DropdownItemSpec(
                apiValue = "rabobank",
                displayText = "Rabobank"
            ),
            DropdownItemSpec(
                apiValue = "regiobank",
                displayText = "RegioBank"
            ),
            DropdownItemSpec(
                apiValue = "revolut",
                displayText = "Revolut"
            ),
            DropdownItemSpec(
                apiValue = "sns_bank",
                displayText = "SNS Bank"
            ),
            DropdownItemSpec(
                apiValue = "triodos_bank",
                displayText = "Triodos Bank"
            ),
            DropdownItemSpec(
                apiValue = "van_lanschot",
                displayText = "Van Lanschot"
            ),
            DropdownItemSpec(
                apiValue = null, // HIGHLIGHT
                displayText = "Other"
            )
        )
    )
)
