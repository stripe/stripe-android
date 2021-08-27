package com.stripe.android.paymentsheet.elements

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.stripe.android.paymentsheet.R

@Composable
internal fun AfterpayClearpayElementUI(
    enabled: Boolean,
    element: AfterpayClearpayHeaderElement
) {
    val context = LocalContext.current

    FlowRow(
        modifier = Modifier.padding(4.dp, 8.dp, 4.dp, 4.dp),
        crossAxisAlignment = FlowCrossAxisAlignment.Center
    ) {
        Text(
            element.getLabel(context.resources),
            Modifier
                .padding(end = 4.dp),
            color = if (isSystemInDarkTheme()) {
                Color.LightGray
            } else {
                Color.Black
            }
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_afterpay_clearpay_logo),
            contentDescription = stringResource(
                R.string.stripe_paymentsheet_payment_method_afterpay_clearpay
            ),
            colorFilter = if (isSystemInDarkTheme()) {
                ColorFilter.tint(Color.White)
            } else {
                null
            }
        )
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(element.infoUrl))
                )
            },
            modifier = Modifier.size(32.dp),
            enabled = enabled,
            contentPadding = PaddingValues(4.dp)
        ) {
            Text(
                text = "ⓘ",
                modifier = Modifier.padding(0.dp),
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = if (isSystemInDarkTheme()) {
                    Color.LightGray
                } else {
                    Color.Black
                }
            )
        }
    }
}
