package com.stripe.android.financialconnections

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import com.airbnb.mvrx.viewModel

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity() {

    val viewModel: FinancialConnectionsSheetViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            setContent {
                Column {
                    Text("FinancialConnectionsSheetNativeActivity")
                }
            }
        }
    }
}
