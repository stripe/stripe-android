package com.stripe.android.crypto.onramp.model.Compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * The type of compliance identifier required or submitted for regulatory compliance.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierType(
    val value: String
) {
    init {
        require(value.isNotBlank()) {
            "value must not be blank"
        }
    }

    companion object {
        val AT_STN = ComplianceIdentifierType("at_stn")
        val BE_NRN = ComplianceIdentifierType("be_nrn")
        val BG_UCN = ComplianceIdentifierType("bg_ucn")
        val HR_OIB = ComplianceIdentifierType("hr_oib")
        val CY_TIC = ComplianceIdentifierType("cy_tic")
        val CZ_RC = ComplianceIdentifierType("cz_rc")
        val DK_CPR = ComplianceIdentifierType("dk_cpr")
        val EE_IK = ComplianceIdentifierType("ee_ik")
        val FI_HETU = ComplianceIdentifierType("fi_hetu")
        val FR_SPI = ComplianceIdentifierType("fr_spi")
        val DE_STN = ComplianceIdentifierType("de_stn")
        val GR_AFM = ComplianceIdentifierType("gr_afm")
        val HU_AD = ComplianceIdentifierType("hu_ad")
        val IS_KT = ComplianceIdentifierType("is_kt")
        val IE_PPSN = ComplianceIdentifierType("ie_ppsn")
        val IT_CF = ComplianceIdentifierType("it_cf")
        val LV_PK = ComplianceIdentifierType("lv_pk")
        val LT_AK = ComplianceIdentifierType("lt_ak")
        val LU_NIF = ComplianceIdentifierType("lu_nif")
        val MT_NIC = ComplianceIdentifierType("mt_nic")
        val MT_PP = ComplianceIdentifierType("mt_pp")
        val NL_BSN = ComplianceIdentifierType("nl_bsn")
        val PL_PESEL = ComplianceIdentifierType("pl_pesel")
        val PL_NIP = ComplianceIdentifierType("pl_nip")
        val PT_NIF = ComplianceIdentifierType("pt_nif")
        val RO_CNP = ComplianceIdentifierType("ro_cnp")
        val SK_RC = ComplianceIdentifierType("sk_rc")
        val SI_PIN = ComplianceIdentifierType("si_pin")
        val SE_PIN = ComplianceIdentifierType("se_pin")

        fun fromValue(value: String): ComplianceIdentifierType {
            return ComplianceIdentifierType(value)
        }
    }
}
