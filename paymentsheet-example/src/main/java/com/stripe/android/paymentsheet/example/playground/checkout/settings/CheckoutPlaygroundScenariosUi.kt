package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val CheckoutScenariosScreenTestTag = "checkout_scenarios_screen"
private const val CheckoutScenarioTestTagPrefix = "checkout_scenario:"

@Composable
internal fun CheckoutPlaygroundScenariosUi(
    group: CheckoutPlaygroundScenario.Group,
    onOpenGroup: (CheckoutPlaygroundScenario.Group) -> Unit,
    onSelect: (CheckoutPlaygroundScenario.Leaf) -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.testTag(CheckoutScenariosScreenTestTag),
    ) {
        group.children.forEach { scenario ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(checkoutScenarioTestTag(scenario))
                    .clickable {
                        when (scenario) {
                            is CheckoutPlaygroundScenario.Group -> onOpenGroup(scenario)
                            is CheckoutPlaygroundScenario.Leaf -> onSelect(scenario)
                        }
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = scenario.displayName,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = if (scenario is CheckoutPlaygroundScenario.Group) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                )
                if (scenario is CheckoutPlaygroundScenario.Group) {
                    Text(text = "›", style = MaterialTheme.typography.h5)
                }
            }
        }
    }
}

internal fun checkoutScenarioTestTag(scenario: CheckoutPlaygroundScenario): String {
    return CheckoutScenarioTestTagPrefix + scenario.key
}
