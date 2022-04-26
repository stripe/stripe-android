package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class BankDropdownSpec(
    override val identifier: IdentifierSpec,
    @StringRes val label: Int,
    val bankType: SupportedBankType
) : SectionFieldSpec(identifier) {
    fun transform(bankRepository: BankRepository, initialValue: String?): SectionFieldElement =
        SimpleDropdownElement(
            this.identifier,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label,
                    bankRepository.get(this.bankType)
                ),
                initialValue = initialValue
            )
        )
}
