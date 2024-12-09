package com.stripe.android.financialconnections.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheet

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
        val viewEffect: FinancialConnectionsExampleViewEffect? by viewModel.viewEffect.collectAsState(null)
        val financialConnectionsSheet = rememberFinancialConnectionsSheet(viewModel::onFinancialConnectionsSheetResult)

        LaunchedEffect(viewEffect) {
            viewEffect?.let {
                when (it) {
                    is OpenFinancialConnectionsSheetExample -> financialConnectionsSheet.present(it.configuration)
                }
            }
        }

        FinancialConnectionsContent(
            state = state,
            onButtonClick = { viewModel.startFinancialConnectionsSessionForData() }
        )
    }

    @Composable
    private fun FinancialConnectionsContent(
        state: FinancialConnectionsExampleState,
        onButtonClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .padding(
                    paddingValues = WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    ).asPaddingValues()
                )
        ) {
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
