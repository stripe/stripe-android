package com.stripe.android.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.stripeColors
import com.stripe.payments.model.R as PaymentsModelR

@Composable
internal fun CardBrandChoiceDropdown(
    expanded: Boolean,
    currentBrand: CardBrand?,
    possibleBrands: List<CardBrand>,
    onBrandSelected: (CardBrand?) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = stringResource(R.string.stripe_card_brand_choice_selection_header),
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 13.dp),
        )

        CardBrandChoiceItem(
            icon = PaymentsModelR.drawable.stripe_ic_unknown,
            displayValue = stringResource(R.string.stripe_card_brand_choice_no_selection),
            isSelected = currentBrand == CardBrand.Unknown,
            currentTextColor = MaterialTheme.stripeColors.onComponent,
            onClick = {
                onBrandSelected(null)
            }
        )

        possibleBrands.forEach { brand ->
            CardBrandChoiceItem(
                icon = brand.icon,
                displayValue = brand.displayName,
                isSelected = brand == currentBrand,
                currentTextColor = MaterialTheme.stripeColors.onComponent,
                onClick = {
                    onBrandSelected(brand)
                }
            )
        }
    }
}

@Composable
private fun CardBrandChoiceItem(
    icon: Int,
    displayValue: String,
    isSelected: Boolean,
    currentTextColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .requiredSizeIn(minHeight = 48.dp)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.padding(start = 13.dp),
        )

        Text(
            text = displayValue,
            modifier = Modifier.padding(start = 16.dp),
            color = if (isSelected) {
                MaterialTheme.colors.primary
            } else {
                currentTextColor
            },
            fontWeight = if (isSelected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            maxLines = 1,
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier
                .height(20.dp)
                .padding(start = 8.dp, end = 16.dp)
                .alpha(if (isSelected) 1f else 0f),
            tint = MaterialTheme.colors.primary,
        )
    }
}

