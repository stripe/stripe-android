package com.stripe.android.payments.bankaccount

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.stripe.android.R

/**
 * No-UI activity that
 */
internal class CollectBankAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collect_bank_account)
    }
}