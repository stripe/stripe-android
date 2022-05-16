package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

internal val p24NameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val p24EmailSection = SectionSpec(
    IdentifierSpec.Generic("email_section"),
    EmailSpec
)
internal val p24BankSection =
    SectionSpec(
        IdentifierSpec.Generic("bank_section"),
        DropdownSpec(
            IdentifierSpec.Generic("p24[bank]"),
            R.string.p24_bank,
            listOf(
                DropdownItemSpec(
                    api_value = "alior_bank",
                    display_text = "Alior Bank"
                ),
                DropdownItemSpec(
                    api_value = "bank_millennium",
                    display_text = "Bank Millenium"
                ),
                DropdownItemSpec(
                    api_value = "bank_nowy_bfg_sa",
                    display_text = "Bank Nowy BFG S.A."
                ),
                DropdownItemSpec(
                    api_value = "bank_pekao_sa",
                    display_text = "Bank PEKAO S.A"
                ),
                DropdownItemSpec(
                    api_value = "banki_spbdzielcze",
                    display_text = "Bank spółdzielczy"
                ),
                DropdownItemSpec(
                    api_value = "blik",
                    display_text = "BLIK"
                ),
                DropdownItemSpec(
                    api_value = "bnp_paribas",
                    display_text = "BNP Paribas"
                ),
                DropdownItemSpec(
                    api_value = "boz",
                    display_text = "BOZ"
                ),
                DropdownItemSpec(
                    api_value = "citi_handlowy",
                    display_text = "CitiHandlowy"
                ),
                DropdownItemSpec(
                    api_value = "credit_agricole",
                    display_text = "Credit Agricole"
                ),
                DropdownItemSpec(
                    api_value = "etransfer_pocztowy24",
                    display_text = "e-Transfer Pocztowy24"
                ),
                DropdownItemSpec(
                    api_value = "getin_bank",
                    display_text = "Getin Bank"
                ),
                DropdownItemSpec(
                    api_value = "ideabank",
                    display_text = "IdeaBank"
                ),
                DropdownItemSpec(
                    api_value = "ing",
                    display_text = "ING"
                ),
                DropdownItemSpec(
                    api_value = "inteligo",
                    display_text = "inteligo"
                ),
                DropdownItemSpec(
                    api_value = "mbank_mtransfer",
                    display_text = "mBank"
                ),
                DropdownItemSpec(
                    api_value = "nest_przelew",
                    display_text = "Nest Przelew"
                ),
                DropdownItemSpec(
                    api_value = "noble_pay",
                    display_text = "Noble Pay"
                ),
                DropdownItemSpec(
                    api_value = "pbac_z_ipko",
                    display_text = "PBac z iPKO (PKO+BP"
                ),
                DropdownItemSpec(
                    api_value = "plus_bank",
                    display_text = "Plus Bank"
                ),
                DropdownItemSpec(
                    api_value = "santander_przelew24",
                    display_text = "Santander"
                ),
                DropdownItemSpec(
                    api_value = "toyota_bank",
                    display_text = "Toyota Bank"
                ),
                DropdownItemSpec(
                    api_value = "volkswagen_bank",
                    display_text = "Volkswagen Bank"
                ),
                DropdownItemSpec(
                    api_value = null,
                    display_text = "Other"
                )
            )
        )
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val P24Form = LayoutSpec.create(
    p24NameSection,
    p24EmailSection,
    p24BankSection
)
