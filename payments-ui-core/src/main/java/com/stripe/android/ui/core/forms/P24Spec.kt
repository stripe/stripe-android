package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.TranslationId

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val P24Form = LayoutSpec.create(
    NameSpec(),
    EmailSpec(),
    DropdownSpec(
        IdentifierSpec.Generic("p24[bank]"),
        TranslationId.P24Bank,
        listOf(
            DropdownItemSpec(
                apiValue = "alior_bank",
                displayText = "Alior Bank"
            ),
            DropdownItemSpec(
                apiValue = "bank_millennium",
                displayText = "Bank Millenium"
            ),
            DropdownItemSpec(
                apiValue = "bank_nowy_bfg_sa",
                displayText = "Bank Nowy BFG S.A."
            ),
            DropdownItemSpec(
                apiValue = "bank_pekao_sa",
                displayText = "Bank PEKAO S.A"
            ),
            DropdownItemSpec(
                apiValue = "banki_spbdzielcze",
                displayText = "Bank spółdzielczy"
            ),
            DropdownItemSpec(
                apiValue = "blik",
                displayText = "BLIK"
            ),
            DropdownItemSpec(
                apiValue = "bnp_paribas",
                displayText = "BNP Paribas"
            ),
            DropdownItemSpec(
                apiValue = "boz",
                displayText = "BOZ"
            ),
            DropdownItemSpec(
                apiValue = "citi_handlowy",
                displayText = "CitiHandlowy"
            ),
            DropdownItemSpec(
                apiValue = "credit_agricole",
                displayText = "Credit Agricole"
            ),
            DropdownItemSpec(
                apiValue = "etransfer_pocztowy24",
                displayText = "e-Transfer Pocztowy24"
            ),
            DropdownItemSpec(
                apiValue = "getin_bank",
                displayText = "Getin Bank"
            ),
            DropdownItemSpec(
                apiValue = "ideabank",
                displayText = "IdeaBank"
            ),
            DropdownItemSpec(
                apiValue = "ing",
                displayText = "ING"
            ),
            DropdownItemSpec(
                apiValue = "inteligo",
                displayText = "inteligo"
            ),
            DropdownItemSpec(
                apiValue = "mbank_mtransfer",
                displayText = "mBank"
            ),
            DropdownItemSpec(
                apiValue = "nest_przelew",
                displayText = "Nest Przelew"
            ),
            DropdownItemSpec(
                apiValue = "noble_pay",
                displayText = "Noble Pay"
            ),
            DropdownItemSpec(
                apiValue = "pbac_z_ipko",
                displayText = "PBac z iPKO (PKO+BP"
            ),
            DropdownItemSpec(
                apiValue = "plus_bank",
                displayText = "Plus Bank"
            ),
            DropdownItemSpec(
                apiValue = "santander_przelew24",
                displayText = "Santander"
            ),
            DropdownItemSpec(
                apiValue = "toyota_bank",
                displayText = "Toyota Bank"
            ),
            DropdownItemSpec(
                apiValue = "volkswagen_bank",
                displayText = "Volkswagen Bank"
            ),
            DropdownItemSpec(
                apiValue = null,
                displayText = "Other"
            )
        )
    )
)
