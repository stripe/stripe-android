package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

/** Visual comparison only: the shield is a test-only overlay, not a production button option. */
internal class PayButtonIconScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.LightTheme),
        listOf(PaymentSheetAppearance.DefaultAppearance),
        listOf(FontSize.DefaultFont),
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    )

    @Test
    fun withPadlock() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                locked = true,
                enabled = true,
                onClick = {},
            )
        }
    }

    @Test
    fun withoutIcon() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                locked = false,
                enabled = true,
                onClick = {},
            )
        }
    }

    @Test
    fun withShieldCheck() {
        paparazziRule.snapshot {
            Box {
                PrimaryButton(
                    label = "Pay $50.99",
                    locked = false,
                    enabled = true,
                    onClick = {},
                )

                // Match the lock's trailing inset: 8.dp button padding + 8.dp content padding.
                Icon(
                    imageVector = ShieldCheck,
                    contentDescription = null,
                    tint = PrimaryButtonTheme.colors.onBackground,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                )
            }
        }
    }
}

// Outline shield and checkmark inspired by the supplied reference, kept entirely in test sources.
private val ShieldCheck = ImageVector.Builder(
    name = "ShieldCheck",
    defaultWidth = 16.dp,
    defaultHeight = 16.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(12f, 3f)
        curveTo(9.5f, 5f, 7f, 5.5f, 4f, 6f)
        verticalLineTo(11f)
        curveTo(4f, 16f, 7f, 19f, 12f, 21f)
        curveTo(17f, 19f, 20f, 16f, 20f, 11f)
        verticalLineTo(6f)
        curveTo(17f, 5.5f, 14.5f, 5f, 12f, 3f)
        close()

        moveTo(8.5f, 11.5f)
        lineTo(11f, 14f)
        lineTo(15.5f, 9.5f)
    }
}.build()
