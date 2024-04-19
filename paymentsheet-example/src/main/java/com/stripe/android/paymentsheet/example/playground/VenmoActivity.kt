package com.stripe.android.paymentsheet.example.playground

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.ExternalPaymentMethodCreator
import com.stripe.android.paymentsheet.ExternalPaymentMethodInput
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

class VenmoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaymentSheetExampleTheme {
                Column {
                    for (result in Result.entries) {
                        ResultButton(result = result)
                    }
                }
            }
        }
    }

    enum class Result(val resultCode : Int) {
        CANCELED(resultCode = -1),
        COMPLETED(resultCode = 1),
        FAILED(resultCode = 0),
    }

    fun finishWithResult(result : Result) {
        Log.i("xkcd", "selected result: $result")
        setResult(result.resultCode)
        finish()
    }

    @Composable
    fun ResultButton(result : Result) {
        Button(onClick = { finishWithResult(result) }) {
            Text(text = result.toString())
        }
    }

    class VenmoPaymentMethodCreator : ExternalPaymentMethodCreator {
        override fun createIntent(context: Context, input: ExternalPaymentMethodInput): Intent {
            return Intent().setClass(
                context,
                VenmoActivity::class.java
            )
        }
    }
}
