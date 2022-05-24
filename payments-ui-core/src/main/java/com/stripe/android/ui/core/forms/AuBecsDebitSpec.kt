package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.StringRepository

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AuBecsDebitForm: List<FormItemSpec> = listOf(
    EmailSpec(),
    BsbSpec(),
    AuBankAccountNumberSpec(),
    NameSpec(
        IdentifierSpec.Name,
        label = StringRepository.TranslationId.AuBecsAccountName
    ),
    AuBecsDebitMandateTextSpec()
)
