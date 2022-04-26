package com.stripe.android.financialconnections.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenConnectionsSheetExample
import com.stripe.financialconnections.compose.createComposable

class FinancialConnectionsComposeExampleActivity : AppCompatActivity() {

    private val viewModel by viewModels<FinancialConnectionsExampleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinancialConnectionsScreen()
        }
    }


    @Composable
    private fun FinancialConnectionsScreen() {
        val state: FinancialConnectionsExampleState by viewModel.state.collectAsState()
        val sideEffect: FinancialConnectionsExampleViewEffect? by viewModel.viewEffect.collectAsState(
            null
        )
        val launcher =
            FinancialConnectionsSheet.createComposable(viewModel::onFinancialConnectionsSheetResult)

        LaunchedEffect(sideEffect) {
            when (val effect = sideEffect) {
                is OpenConnectionsSheetExample -> launcher.present(effect.configuration)
                null -> Unit
            }
        }

        FinancialConnectionsContent(
            state = state,
            onButtonClick = { viewModel.startLinkAccountSession() }
        )
    }

    @Composable
    fun FinancialConnectionsContent(
        state: FinancialConnectionsExampleState,
        onButtonClick: () -> Unit
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            if (state.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = { onButtonClick() },
            ) {
                Text("Connect Accounts!")
            }

            Divider(modifier = Modifier.padding(vertical = 5.dp))
            Text(text = state.status)
        }
    }

}
