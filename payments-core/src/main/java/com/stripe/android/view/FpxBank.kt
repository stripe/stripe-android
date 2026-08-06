package com.stripe.android.view

internal enum class FpxBank(
    override val id: String,
    override val code: String,
    override val displayName: String
) : Bank {
    AffinBank(
        "ABB0233",
        "affin_bank",
        "Affin Bank"
    ),
    Agrobank(
        "AGRO01",
        "agrobank",
        "Agrobank"
    ),
    AllianceBank(
        "ABMB0212",
        "alliance_bank",
        "Alliance Bank"
    ),
    AmBank(
        "AMBB0209",
        "ambank",
        "AmBank"
    ),
    BankIslam(
        "BIMB0340",
        "bank_islam",
        "Bank Islam"
    ),
    BankMuamalat(
        "BMMB0341",
        "bank_muamalat",
        "Bank Muamalat"
    ),
    BankOfChina(
        "BOCM01",
        "bank_of_china",
        "Bank of China"
    ),
    BankRakyat(
        "BKRM0602",
        "bank_rakyat",
        "Bank Rakyat"
    ),
    Bsn(
        "BSN0601",
        "bsn",
        "BSN"
    ),
    Cimb(
        "BCBB0235",
        "cimb",
        "CIMB Clicks"
    ),
    HongLeongBank(
        "HLB0224",
        "hong_leong_bank",
        "Hong Leong Bank"
    ),
    Hsbc(
        "HSBC0223",
        "hsbc",
        "HSBC Bank"
    ),
    Kfh(
        "KFH0346",
        "kfh",
        "KFH"
    ),
    Maybank2E(
        "MBB0228",
        "maybank2e",
        "Maybank2E"
    ),
    Maybank2U(
        "MB2U0227",
        "maybank2u",
        "Maybank2U"
    ),
    MbsbBank(
        "MBSB001",
        "mbsb_bank",
        "MBSB Bank"
    ),
    Ocbc(
        "OCBC0229",
        "ocbc",
        "OCBC Bank"
    ),
    PublicBank(
        "PBB0233",
        "public_bank",
        "Public Bank"
    ),
    Rhb(
        "RHB0218",
        "rhb",
        "RHB Bank"
    ),
    StandardChartered(
        "SCB0216",
        "standard_chartered",
        "Standard Chartered"
    ),
    UobBank(
        "UOB0226",
        "uob",
        "UOB Bank"
    );

    companion object {
        /**
         * Return the [FpxBank] that matches the given bank code (e.g. "affin_bank", "hsbc"),
         * or null if no match is found.
         *
         * The bank code should be obtained from [PaymentMethod.Fpx.bank].
         */
        @JvmStatic
        fun get(bankCode: String?): FpxBank? {
            return entries.firstOrNull { it.code == bankCode }
        }
    }
}
