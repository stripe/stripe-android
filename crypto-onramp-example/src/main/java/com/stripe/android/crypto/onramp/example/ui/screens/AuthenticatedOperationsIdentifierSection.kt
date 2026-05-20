package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.crypto.onramp.example.model.IdentifierInputEntry

@Composable
internal fun IdentifierSection(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    identifierInputs: List<IdentifierInputEntry>,
    onIdentifierTypeChange: (Int, String) -> Unit,
    onIdentifierValueChange: (Int, String) -> Unit,
    onAddIdentifier: () -> Unit,
    onRemoveIdentifier: (Int) -> Unit,
    missingIdentifiersSummary: String?,
    submitIdentifiersSummary: String?,
    onRetrieveMissingIdentifiers: () -> Unit,
    onSubmitIdentifiers: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!isExpanded) }
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Identifier Info",
            fontWeight = FontWeight.Bold
        )
        Text(text = if (isExpanded) "Hide" else "Show")
    }

    AnimatedVisibility(visible = isExpanded) {
        IdentifierInfoScreen(
            identifierInputs = identifierInputs,
            onIdentifierTypeChange = onIdentifierTypeChange,
            onIdentifierValueChange = onIdentifierValueChange,
            onAddIdentifier = onAddIdentifier,
            onRemoveIdentifier = onRemoveIdentifier,
            missingIdentifiersSummary = missingIdentifiersSummary,
            submitIdentifiersSummary = submitIdentifiersSummary,
            onRetrieveMissingIdentifiers = onRetrieveMissingIdentifiers,
            onSubmitIdentifiers = onSubmitIdentifiers
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun IdentifierInfoScreen(
    identifierInputs: List<IdentifierInputEntry>,
    onIdentifierTypeChange: (Int, String) -> Unit,
    onIdentifierValueChange: (Int, String) -> Unit,
    onAddIdentifier: () -> Unit,
    onRemoveIdentifier: (Int) -> Unit,
    missingIdentifiersSummary: String?,
    submitIdentifiersSummary: String?,
    onRetrieveMissingIdentifiers: () -> Unit,
    onSubmitIdentifiers: () -> Unit,
) {
    Column {
        Text(
            text = "Missing Identifiers",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Fetch missing identifier requirements for the current Link user.",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onRetrieveMissingIdentifiers,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Retrieve Missing Identifiers")
        }

        missingIdentifiersSummary?.let { summary ->
            Text(
                text = "Latest response:\n$summary",
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        Text(
            text = "Submit Identifiers",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Add as many identifiers as needed and submit them as a list.",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        identifierInputs.forEachIndexed { index, identifierInput ->
            Text(
                text = "Identifier ${index + 1}",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            KycTextField(
                value = identifierInput.type,
                label = "Identifier Type (e.g. mt_nic)",
                onChange = { onIdentifierTypeChange(index, it) }
            )
            KycTextField(
                value = identifierInput.value,
                label = "Identifier Value",
                onChange = { onIdentifierValueChange(index, it) }
            )
            TextButton(
                onClick = { onRemoveIdentifier(index) },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text("Remove Identifier")
            }
        }

        Button(
            onClick = onAddIdentifier,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Add Identifier")
        }

        Button(
            onClick = onSubmitIdentifiers,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Submit Identifiers")
        }

        submitIdentifiersSummary?.let { summary ->
            Text(
                text = "Latest submission result:\n$summary",
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
