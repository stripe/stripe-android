package com.stripe.android.ui.core.elements

import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class BankDropdownSpec(
    override val identifier: IdentifierSpec,
    @StringRes val label: Int,
    val bankType: SupportedBankType
) : SectionFieldSpec(identifier) {
    fun transform(bankRepository: BankRepository): SectionFieldElement =
        SimpleDropdownElement(
            this.identifier,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label,
                    bankRepository.get(this.bankType)
                )
            )
        )
}
