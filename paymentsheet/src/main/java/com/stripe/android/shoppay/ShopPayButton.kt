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
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme

@Composable
fun ShopPayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = true,
        shape = RoundedCornerShape(
            StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF7452FF),
//            disabledBackgroundColor = LinkTheme.colors.buttonBrand,
        ),
        contentPadding = PaddingValues(
            start = 25.dp,
            top = 10.dp,
            end = 25.dp,
            bottom = 10.dp
        )
    ) {
        Image(
            painter = painterResource(R.drawable.shop_pay_logo_white),
            contentDescription = "Shop Pay Logo"
        )
    }
}