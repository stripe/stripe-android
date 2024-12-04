package com.stripe.android.connect.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.connect.example.R

@Composable
fun BetaBadge() {
    val shape = RoundedCornerShape(4.dp)
    val labelMediumEmphasized = TextStyle.Default.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )
    Text(
        modifier = Modifier
            .border(1.dp, colorResource(R.color.default_border_color), shape)
            .background(
                color = colorResource(R.color.default_background_color),
                shape = shape
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
        color = colorResource(R.color.default_text_color),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        style = labelMediumEmphasized,
        text = stringResource(R.string.beta_all_caps)
    )
}

@Composable
@Preview(showBackground = true)
private fun BetaBadgePreview() {
    ConnectSdkExampleTheme {
        BetaBadge()
    }
}
