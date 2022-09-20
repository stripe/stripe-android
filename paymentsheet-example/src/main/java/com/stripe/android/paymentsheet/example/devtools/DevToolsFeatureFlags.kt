package com.stripe.android.paymentsheet.example.devtools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.features.FeatureAvailability
import com.stripe.android.features.FeatureAvailability.Debug
import com.stripe.android.features.FeatureAvailability.Disabled
import com.stripe.android.features.FeatureAvailability.Enabled
import com.stripe.android.features.FeatureFlag
import com.stripe.android.features.isEnabled
import com.stripe.android.paymentsheet.PaymentSheetFeatures

@Composable
internal fun DevToolsFeatureFlags() {
    val features = remember { PaymentSheetFeatures.all }

    LazyColumn {
        itemsIndexed(features) { index, feature ->
            FeatureFlagItem(
                feature = feature,
                isLastItem = index == features.lastIndex,
                onToggle = { availability ->
                    feature.overrideAvailability = availability
                    features.removeAt(index)
                    features.add(index, feature)
                }
            )
        }
    }
}

@Composable
private fun FeatureFlagItem(
    feature: FeatureFlag,
    isLastItem: Boolean,
    onToggle: (FeatureAvailability) -> Unit
) {
    val isTurnedOn = feature.isEnabled
    val isInDevelopment = feature.availability == Debug

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val availability = if (!isTurnedOn) Enabled else Disabled
                onToggle(availability)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isTurnedOn,
            enabled = isInDevelopment,
            onCheckedChange = { isChecked ->
                val availability = if (isChecked) Enabled else Disabled
                onToggle(availability)
            }
        )
    }

    if (!isLastItem) {
        Divider()
    }
}
