package com.stripe.android.view

import com.stripe.android.R

class FpxBank(
    override var id: String,
    override var code: String,
    override var displayName: String,
    val brandIconResId: Int
    ) : Bank() {

    companion object {
        fun values() : Array<FpxBank> {
            return arrayOf(
                FpxBank(
                    "MB2U0227",
                    "maybank2u",
                    "Maybank2U",
                    R.drawable.stripe_ic_bank_maybank,
                ),
                FpxBank(
                    "BCBB0235",
                    "cimb",
                    "CIMB Clicks",
                    R.drawable.stripe_ic_bank_cimb
                ),
                FpxBank(
                    "PBB0233",
                    "public_bank",
                    "Public Bank",
                    R.drawable.stripe_ic_bank_public
                ),
                FpxBank(
                    "RHB0218",
                    "rhb",
                    "RHB Bank",
                    R.drawable.stripe_ic_bank_rhb
                ),
                FpxBank(
                    "HLB0224",
                    "hong_leong_bank",
                    "Hong Leong Bank",
                    R.drawable.stripe_ic_bank_hong_leong
                ),
                FpxBank(
                    "AMBB0209",
                    "ambank",
                    "AmBank",
                    R.drawable.stripe_ic_bank_ambank
                ),
                FpxBank(
                    "ABB0233",
                    "affin_bank",
                    "Affin Bank",
                    R.drawable.stripe_ic_bank_affin
                ),
                FpxBank(
                    "ABMB0212",
                    "alliance_bank",
                    "Alliance Bank",
                    R.drawable.stripe_ic_bank_alliance
                ),
                FpxBank(
                    "BIMB0340",
                    "bank_islam",
                    "Bank Islam",
                    R.drawable.stripe_ic_bank_islam
                ),
                FpxBank(
                    "BMMB0341",
                    "bank_muamalat",
                    "Bank Muamalat",
                    R.drawable.stripe_ic_bank_muamalat
                ),
                FpxBank(
                    "BKRM0602",
                    "bank_rakyat",
                    "Bank Rakyat",
                    R.drawable.stripe_ic_bank_raykat
                ),
                FpxBank(
                    "BSN0601",
                    "bsn",
                    "BSN",
                    R.drawable.stripe_ic_bank_bsn
                ),
                FpxBank(
                    "HSBC0223",
                    "hsbc",
                    "HSBC Bank",
                    R.drawable.stripe_ic_bank_hsbc
                ),
                FpxBank(
                    "KFH0346",
                    "kfh",
                    "KFH",
                    R.drawable.stripe_ic_bank_kfh
                ),
                FpxBank(
                    "MBB0228",
                    "maybank2e",
                    "Maybank2E",
                    R.drawable.stripe_ic_bank_maybank
                ),
                FpxBank(
                    "OCBC0229",
                    "ocbc",
                    "OCBC Bank",
                    R.drawable.stripe_ic_bank_ocbc
                ),
                FpxBank(
                    "SCB0216",
                    "standard_chartered",
                    "Standard Chartered",
                    R.drawable.stripe_ic_bank_standard_chartered
                ),
                FpxBank(
                    "UOB0226",
                    "uob",
                    "UOB",
                    R.drawable.stripe_ic_bank_uob
                ),
            )
        }

        /* Return the [FpxBank] that matches the given bank code (e.g. "affin_bank", "hsbc"),
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
