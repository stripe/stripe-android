package com.stripe.android.financialconnections.example

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheet

class FinancialConnectionsPlaygroundActivity : AppCompatActivity() {

    private val viewModel by viewModels<FinancialConnectionsPlaygroundViewModel>()

    private val sharedPreferences by lazy {
        getSharedPreferences("FINANCIAL_CONNECTIONS_DEBUG", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinancialConnectionsScreen()
        }
    }


    @Composable
    private fun FinancialConnectionsScreen() {
        val state: FinancialConnectionsExampleState by viewModel.state.collectAsState()
        val viewEffect: FinancialConnectionsExampleViewEffect? by viewModel.viewEffect.collectAsState(
            null
        )
        val financialConnectionsSheet =
            rememberFinancialConnectionsSheet(viewModel::onFinancialConnectionsSheetResult)

        LaunchedEffect(viewEffect) {
            viewEffect?.let {
                when (it) {
                    is OpenFinancialConnectionsSheetExample ->
                        financialConnectionsSheet.present(it.configuration)
                }
            }
        }

        FinancialConnectionsContent(
            state = state,
            onButtonClick = { mode -> viewModel.startFinancialConnectionsSessionForData(mode) }
        )
    }

    @Composable
    private fun FinancialConnectionsContent(
        state: FinancialConnectionsExampleState,
        onButtonClick: (Mode) -> Unit
    ) {
        val modeOptions = listOf(Mode.Test, Mode.Live)
        val (selectedMode, onModeSelected) = remember { mutableStateOf(modeOptions[0]) }
        Scaffold(
            topBar = { TopAppBar(title = { Text("Connections Playground") }) },
            content = {
                Column(
                    modifier = Modifier
                        .padding(it)
                        .padding(16.dp)
                ) {
                    OverrideFlowSection()
                    TestModeSection(modeOptions, selectedMode, onModeSelected)
                    if (state.loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = { onButtonClick(selectedMode) },
                    ) {
                        Text("Connect Accounts!")
                    }
                    Text(text = state.status)
                }
            }
        )

    }

    @Composable
    private fun OverrideFlowSection() {
        val radioOptions = listOf("none", "native", "web")
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
        LaunchedEffect(selectedOption) {
            sharedPreferences.edit {
                when (selectedOption) {
                    "native" -> putBoolean("override_native", true)
                    "web" -> putBoolean("override_native", false)
                    else -> clear()
                }
            }
        }
        Text(
            text = "Flow Override",
            style = MaterialTheme.typography.h6.merge(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            radioOptions.forEach { text ->
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = { onOptionSelected(text) }
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.body1.merge(),
                )
            }
        }
    }

    @Composable
    private fun TestModeSection(
        radioOptions: List<Mode>,
        selectedOption: Mode,
        onOptionSelected: (Mode) -> Unit
    ) {
        Text(
            text = "Mode",
            style = MaterialTheme.typography.h6.merge(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            radioOptions.forEach { text ->
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = { onOptionSelected(text) }
                )
                Text(
                    text = text.name,
                    style = MaterialTheme.typography.body1.merge(),
                )
            }
        }
    }

    @Preview
    @Composable
    fun ContentPreview() {
        FinancialConnectionsContent(
            state = FinancialConnectionsExampleState(false, "Result: Pending"),
            onButtonClick = {}
        )
    }
}
