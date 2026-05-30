package com.stripe.android.samsungpay

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme
import com.stripe.android.uicore.StripeTheme

internal val SamsungPayBackgroundColorLight = Color.Black
internal val SamsungPayBackgroundColorDark = Color.White
private val SamsungPayButtonVerticalPadding = 10.dp
private val SamsungPayButtonHorizontalPadding = 25.dp

@Composable
internal fun SamsungPayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val buttonDescription = stringResource(R.string.stripe_samsung_pay_button_description)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PrimaryButtonTheme.shape.height)
            .semantics {
                contentDescription = buttonDescription
            },
        enabled = true,
        shape = RoundedCornerShape(
            StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSystemInDarkTheme()) SamsungPayBackgroundColorDark else SamsungPayBackgroundColorLight,
        ),
        contentPadding = PaddingValues(
            horizontal = SamsungPayButtonHorizontalPadding,
            vertical = SamsungPayButtonVerticalPadding
        )
    ) {
        Image(
            modifier = Modifier
                .height(PrimaryButtonTheme.shape.height - SamsungPayButtonVerticalPadding * 2),
            painter = painterResource(R.drawable.stripe_ic_samsung_pay),
            contentDescription = null,
            contentScale = ContentScale.FillHeight
        )
    }
}
