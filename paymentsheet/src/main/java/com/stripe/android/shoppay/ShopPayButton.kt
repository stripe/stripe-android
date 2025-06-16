package com.stripe.android.shoppay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme

private val ShopPayButtonHeight = 48.dp
private val ShopPayBackgroundColor = Color(0xFF5433EB)
private val ShopPayButtonVerticalPadding = 10.dp
private val ShopPayButtonHorizontalPadding = 25.dp

@Composable
internal fun ShopPayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val buttonDescription = stringResource(R.string.stripe_shop_pay_button_description)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(ShopPayButtonHeight)
            .semantics {
                contentDescription = buttonDescription
            },
        enabled = true,
        shape = RoundedCornerShape(
            StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ShopPayBackgroundColor,
        ),
        contentPadding = PaddingValues(
            horizontal = ShopPayButtonHorizontalPadding,
            vertical = ShopPayButtonVerticalPadding
        )
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_shop_pay_logo_white),
            contentDescription = null
        )
    }
}
