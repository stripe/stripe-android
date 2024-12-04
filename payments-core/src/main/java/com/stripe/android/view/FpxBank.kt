package com.stripe.android.view

internal enum class FpxBank(
    override val id: String,
    override val code: String,
    override val displayName: String
) : Bank {
    Maybank2U(
        "MB2U0227",
        "maybank2u",
        "Maybank2U"
    ),
    Cimb(
        "BCBB0235",
        "cimb",
        "CIMB Clicks"
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
    HongLeongBank(
        "HLB0224",
        "hong_leong_bank",
        "Hong Leong Bank"
    ),
    AmBank(
        "AMBB0209",
        "ambank",
        "AmBank"
    ),
    AffinBank(
        "ABB0233",
        "affin_bank",
        "Affin Bank"
    ),
    AllianceBankBusiness(
        "ABMB0212",
        "alliance_bank",
        "Alliance Bank"
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
    Ocbc(
        "OCBC0229",
        "ocbc",
        "OCBC Bank"
    ),
    StandardChartered(
        "SCB0216",
        "standard_chartered",
        "Standard Chartered"
    ),
    UobBank(
        "UOB0226",
        "uob",
        "UOB"
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
