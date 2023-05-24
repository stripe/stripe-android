package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text

internal class CustomerSheetActivity : AppCompatActivity() {

    private val starterArgs: CustomerSheetContract.Args? by lazy {
        CustomerSheetContract.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Text(starterArgs?.data.orEmpty())
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(InternalCustomerSheetResult.Canceled)
        }
    }

    private fun finishWithResult(result: InternalCustomerSheetResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }
}
