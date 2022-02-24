package com.stripe.android.paymentsheet.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.R

@Composable
fun LpmSelectorText(text: String, modifier: Modifier, isEnabled: Boolean) {
    val textColor = colorResource(R.color.stripe_paymentsheet_title_text)
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
        lineHeight = 1.sp,
        modifier = modifier
    )
}