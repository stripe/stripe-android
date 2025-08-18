package com.stripe.android.link

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.image.StripeImageLoader
import org.junit.Rule
import org.junit.Test

class LinkControllerPaymentMethodPreviewScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(SystemAppearance.entries)

    @Test
    fun testPreview() {
        paparazziRule.snapshot {
            val context = LocalContext.current
            val iconLoader = remember(context) {
                PaymentSelection.IconLoader(
                    resources = context.resources,
                    imageLoader = StripeImageLoader(context),
                )
            }
            val cases = listOf(
                TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                    .copy(brand = CardBrand.Visa),
                TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                    .copy(brand = CardBrand.MasterCard),
                TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                    .copy(bankName = "Chase"),
                TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                    .copy(bankName = "Bank of America"),
                TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                    .copy(bankName = null, bankIconCode = "pnc"),
                TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                    .copy(bankName = "Foobar"),
                // Should never be passthrough.
                TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
            )
            MaterialTheme {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cases.forEach { details ->
                        PaymentMethodPreview(details.toPreview(context, iconLoader))
                    }
                }
            }
        }
    }

    @Composable
    private fun PaymentMethodPreview(preview: LinkController.PaymentMethodPreview) {
        val iconSize = 24.dp
        val contentColor = MaterialTheme.colors.onSurface
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(iconSize),
                painter = preview.iconPainter,
                contentDescription = null,
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    modifier = Modifier,
                    text = preview.label,
                    style = MaterialTheme.typography.h6,
                    color = contentColor,
                )
                preview.sublabel?.let { sublabel ->
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = sublabel,
                        style = MaterialTheme.typography.body2,
                        color = contentColor.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
