package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.StringRepository

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val EpsForm: List<FormItemSpec> = listOf(
    NameSpec(),
    DropdownSpec(
        api_path = IdentifierSpec.Generic("eps[bank]"),
        label = StringRepository.TranslationId.EpsBank,
        items = listOf(
            DropdownItemSpec(
                api_value = "arzte_und_apotheker_bank",
                display_text = "Ärzte- und Apothekerbank",
            ),
            DropdownItemSpec(
                api_value = "austrian_anadi_bank_ag",
                display_text = "Austrian Anadi Bank AG",
            ),
            DropdownItemSpec(
                api_value = "bank_austria",
                display_text = "Bank Austria",
            ),
            DropdownItemSpec(
                api_value = "bankhaus_carl_spangler",
                display_text = "Bankhaus Carl Spängler & Co.AG",
            ),
            DropdownItemSpec(
                api_value = "bankhaus_schelhammer_und_schattera_ag",
                display_text = "Bankhaus Schelhammer & Schattera AG",
            ),
            DropdownItemSpec(
                api_value = "bawag_psk_ag",
                display_text = "BAWAG P.S.K. AG",
            ),
            DropdownItemSpec(
                api_value = "bks_bank_ag",
                display_text = "BKS Bank AG",
            ),
            DropdownItemSpec(
                api_value = "brull_kallmus_bank_ag",
                display_text = "Brüll Kallmus Bank AG",
            ),
            DropdownItemSpec(
                api_value = "btv_vier_lander_bank",
                display_text = "BTV VIER LÄNDER BANK",
            ),
            DropdownItemSpec(
                api_value = "capital_bank_grawe_gruppe_ag",
                display_text = "Capital Bank Grawe Gruppe AG",
            ),
            DropdownItemSpec(
                api_value = "dolomitenbank",
                display_text = "Dolomitenbank",
            ),
            DropdownItemSpec(
                api_value = "easybank_ag",
                display_text = "Easybank AG",
            ),
            DropdownItemSpec(
                api_value = "erste_bank_und_sparkassen",
                display_text = "Erste Bank und Sparkassen",
            ),
            DropdownItemSpec(
                api_value = "hypo_alpeadriabank_international_ag",
                display_text = "Hypo Alpe-Adria-Bank International AG",
            ),
            DropdownItemSpec(
                api_value = "hypo_noe_lb_fur_niederosterreich_u_wien",
                display_text = "HYPO NOE LB für Niederösterreich u. Wien",
            ),
            DropdownItemSpec(
                api_value = "hypo_oberosterreich_salzburg_steiermark",
                display_text = "HYPO Oberösterreich,Salzburg,Steiermark",
            ),
            DropdownItemSpec(
                api_value = "hypo_tirol_bank_ag",
                display_text = "Hypo Tirol Bank AG",
            ),
            DropdownItemSpec(
                api_value = "hypo_vorarlberg_bank_ag",
                display_text = "Hypo Vorarlberg Bank AG",
            ),
            DropdownItemSpec(
                api_value = "hypo_bank_burgenland_aktiengesellschaft",
                display_text = "HYPO-BANK BURGENLAND Aktiengesellschaft",
            ),
            DropdownItemSpec(
                api_value = "marchfelder_bank",
                display_text = "Marchfelder Bank",
            ),
            DropdownItemSpec(
                api_value = "oberbank_ag",
                display_text = "Oberbank AG",
            ),
            DropdownItemSpec(
                api_value = "raiffeisen_bankengruppe_osterreich",
                display_text = "Raiffeisen Bankengruppe Österreich",
            ),
            DropdownItemSpec(
                api_value = "schoellerbank_ag",
                display_text = "Schoellerbank AG",
            ),
            DropdownItemSpec(
                api_value = "sparda_bank_wien",
                display_text = "Sparda-Bank Wien",
            ),
            DropdownItemSpec(
                api_value = "volksbank_gruppe",
                display_text = "Volksbank Gruppe",
            ),
            DropdownItemSpec(
                api_value = "volkskreditbank_ag",
                display_text = "Volkskreditbank AG",
            ),
            DropdownItemSpec(
                api_value = "vr_bank_braunau",
                display_text = "VR-Bank Braunau",
            ),
            DropdownItemSpec(
                api_value = null,
                display_text = "Other"
            )
        )
    )
)
