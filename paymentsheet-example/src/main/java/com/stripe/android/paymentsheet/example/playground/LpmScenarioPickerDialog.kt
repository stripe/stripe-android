package com.stripe.android.paymentsheet.example.playground

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stripe.android.paymentsheet.example.playground.settings.LpmScenario
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.uicore.utils.collectAsState

/**
 * Lists every payment method PaymentSheet supports and applies the playground settings that offer it on its own.
 */
@Composable
internal fun LpmScenarioPickerDialog(
    playgroundSettings: PlaygroundSettings,
    onSelect: (LpmScenario) -> Unit,
    onDismiss: () -> Unit,
) {
    val settings by playgroundSettings.settings.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredScenarios = remember(searchQuery) {
        LpmScenario.all.filter { it.matches(searchQuery) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxHeight(DIALOG_HEIGHT_FRACTION),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LPM Scenarios",
                    style = MaterialTheme.typography.h6,
                )
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Name, type, merchant, or currency") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredScenarios, key = { it.code }) { scenario ->
                        LpmScenarioRow(
                            scenario = scenario,
                            isApplied = scenario.isAppliedTo(settings),
                            onClick = { onSelect(scenario) },
                        )
                    }
                    if (searchQuery.isEmpty()) {
                        item {
                            Text(
                                text = "Sets the merchant, currency, and amount, and turns off automatic payment " +
                                    "methods so the selected payment method is the only one offered.",
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun LpmScenario.matches(query: String): Boolean {
    return listOf(displayName, code, merchant.name, currency.displayName).any {
        it.contains(query, ignoreCase = true)
    }
}

@Composable
private fun LpmScenarioRow(
    scenario: LpmScenario,
    isApplied: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("lpm_scenario_${scenario.code}"),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scenario.displayName,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${scenario.code} · ${scenario.summary}",
                style = MaterialTheme.typography.caption,
            )
        }
        if (isApplied) {
            Text(
                text = "✓",
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private const val DIALOG_HEIGHT_FRACTION = 0.9f
