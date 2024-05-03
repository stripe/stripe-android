package com.stripe.android.view

import com.stripe.android.R

internal enum class FpxBank(
    override val id: String,
    override val code: String,
    override val displayName: String,
    override val brandIconResId: Int? = null
) : Bank {
    Maybank2U(
        "MB2U0227",
        "maybank2u",
        "Maybank2U",
        R.drawable.stripe_ic_bank_maybank
    ),
    Cimb(
        "BCBB0235",
        "cimb",
        "CIMB Clicks",
        R.drawable.stripe_ic_bank_cimb
    ),
    PublicBank(
        "PBB0233",
        "public_bank",
        "Public Bank",
        R.drawable.stripe_ic_bank_public
    ),
    Rhb(
        "RHB0218",
        "rhb",
        "RHB Bank",
        R.drawable.stripe_ic_bank_rhb
    ),
    HongLeongBank(
        "HLB0224",
        "hong_leong_bank",
        "Hong Leong Bank",
        R.drawable.stripe_ic_bank_hong_leong
    ),
    AmBank(
        "AMBB0209",
        "ambank",
        "AmBank",
        R.drawable.stripe_ic_bank_ambank
    ),
    AffinBank(
        "ABB0233",
        "affin_bank",
        "Affin Bank",
        R.drawable.stripe_ic_bank_affin
    ),
    AllianceBankBusiness(
        "ABMB0212",
        "alliance_bank",
        "Alliance Bank",
        R.drawable.stripe_ic_bank_alliance
    ),
    BankIslam(
        "BIMB0340",
        "bank_islam",
        "Bank Islam",
        R.drawable.stripe_ic_bank_islam
    ),
    BankMuamalat(
        "BMMB0341",
        "bank_muamalat",
        "Bank Muamalat",
        R.drawable.stripe_ic_bank_muamalat
    ),
    BankRakyat(
        "BKRM0602",
        "bank_rakyat",
        "Bank Rakyat",
        R.drawable.stripe_ic_bank_raykat
    ),
    Bsn(
        "BSN0601",
        "bsn",
        "BSN",
        R.drawable.stripe_ic_bank_bsn
    ),
    Hsbc(
        "HSBC0223",
        "hsbc",
        "HSBC Bank",
        R.drawable.stripe_ic_bank_hsbc
    ),
    Kfh(
        "KFH0346",
        "kfh",
        "KFH",
        R.drawable.stripe_ic_bank_kfh
    ),
    Maybank2E(
        "MBB0228",
        "maybank2e",
        "Maybank2E",
        R.drawable.stripe_ic_bank_maybank
    ),
    Ocbc(
        "OCBC0229",
        "ocbc",
        "OCBC Bank",
        R.drawable.stripe_ic_bank_ocbc
    ),
    StandardChartered(
        "SCB0216",
        "standard_chartered",
        "Standard Chartered",
        R.drawable.stripe_ic_bank_standard_chartered
    ),
    UobBank(
        "UOB0226",
        "uob",
        "UOB",
        R.drawable.stripe_ic_bank_uob
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
