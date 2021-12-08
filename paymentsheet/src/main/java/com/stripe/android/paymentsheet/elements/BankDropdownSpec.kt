package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class BankDropdownSpec(
    override val identifier: IdentifierSpec,
    @StringRes val label: Int,
    val bankType: SupportedBankType
) : SectionFieldSpec(identifier)
