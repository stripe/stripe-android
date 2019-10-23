package com.stripe.android.view

import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

@Suppress("unused")
enum class FpxBank(
    val code: String,
    val displayName: String,
    val brandIconResId: Int = R.drawable.ic_bank_generic
) {
    AffinBank("affin_bank", "Affin Bank",
        R.drawable.ic_bank_affin),
    AllianceBankBusiness("alliance_bank", "Alliance Bank (Business)",
        R.drawable.ic_bank_alliance),
    AmBank("ambank", "AmBank",
        R.drawable.ic_bank_ambank),
    BankIslam("bank_islam", "Bank Islam",
        R.drawable.ic_bank_islam),
    BankMuamalat("bank_muamalat", "Bank Muamalat",
        R.drawable.ic_bank_muamalat),
    BankRakyat("bank_rakyat", "Bank Rakyat",
        R.drawable.ic_bank_raykat),
    Bsn("bsn", "BSN",
        R.drawable.ic_bank_bsn),
    Cimb("cimb", "CIMB Clicks",
        R.drawable.ic_bank_cimb),
    HongLeongBank("hong_leong_bank", "Hong Leong Bank",
        R.drawable.ic_bank_hong_leong),
    Hsbc("hsbc", "HSBC BANK",
        R.drawable.ic_bank_hsbc),
    Kfh("kfh", "KFH",
        R.drawable.ic_bank_kfh),
    Maybank2E("maybank2e", "Maybank2E",
        R.drawable.ic_bank_maybank),
    Maybank2U("maybank2u", "Maybank2U",
        R.drawable.ic_bank_maybank),
    Ocbc("ocbc", "OCBC Bank",
        R.drawable.ic_bank_ocbc),
    PublicBank("public_bank", "Public Bank",
        R.drawable.ic_bank_public),
    Rhb("rhb", "RHB Bank",
        R.drawable.ic_bank_rhb),
    StandardChartered("standard_chartered", "Standard Chartered",
        R.drawable.ic_bank_standard_chartered),
    UobBank("uob", "UOB Bank",
        R.drawable.ic_bank_uob);

    companion object {
        /**
         * Return the [FpxBank] that matches the given bank code (e.g. "affin_bank", "hsbc"),
         * or null if no match is found.
         *
         * The bank code should be obtained from [PaymentMethod.Fpx.bank].
         */
        @JvmStatic
        fun get(bankCode: String?): FpxBank? {
            return values().firstOrNull { it.code == bankCode }
        }
    }
}
