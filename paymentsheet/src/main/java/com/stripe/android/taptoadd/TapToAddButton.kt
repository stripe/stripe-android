package com.stripe.android.taptoadd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme

@Composable
fun TapToAddButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
    )

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(
                minHeight = StripeTheme.primaryButtonStyle.shape.height.dp
            ),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(
            width = 2.dp,
            color = GREY_BORDER_COLOR
        ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = GREY_BACKGROUND_COLOR,
            disabledBackgroundColor = GREY_BACKGROUND_COLOR,
        )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Row(Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(R.drawable.ic_contactless),
                    tint = GREY_COLOR,
                    contentDescription = null,
                )

                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = "Tap to Add Card",
                color = GREY_COLOR,
                style = MaterialTheme.typography.h5,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview
@Composable
private fun TestButton() {
    StripeTheme {
        TapToAddButton(enabled = true) {  }
    }
}

private val GREY_COLOR = Color(83, 83, 83)
private val GREY_BACKGROUND_COLOR = Color(243, 243, 243)
private val GREY_BORDER_COLOR = Color(207, 207, 211)