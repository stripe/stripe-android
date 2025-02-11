package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.stripeColors
import org.junit.Rule
import org.junit.Test

class PaymentMethodsUiExtensionScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
    )

    @Composable
    fun getSectionCardColor(): Color {
        return if (isSystemInDarkTheme()) {
            Color.DarkGray
        } else {
            Color.LightGray
        }
    }

    @Composable
    private fun iconCard(
        @DrawableRes iconRes: Int,
        backgroundColorOverride: Color? = null
    ) {
        SectionCard(
            isSelected = false,
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 6.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(backgroundColorOverride ?: MaterialTheme.stripeColors.component)
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .height(40.dp)
                        .width(56.dp)
                )
            }
        }
    }

    @Test
    fun `Drawable Reference points to correct drawable without override`() {
        paparazziRule.snapshot {
            iconAndThemesTestComposable(
                title = "no override\nisDarkTheme ${isSystemInDarkTheme()}",
                icons = getIcons(),
            )
        }
    }

    @Test
    fun `Drawable Reference points to correct drawable with override and light`() {
        paparazziRule.snapshot {
            iconAndThemesTestComposable(
                title = "override show light\nisDarkTheme ${isSystemInDarkTheme()}",
                icons = getIcons(showNightIcon = true),
                backgroundColorOverride = getSectionCardColor()
            )
        }
    }

    @Test
    fun `Drawable Reference points to correct drawable with override and dark`() {
        paparazziRule.snapshot {
            iconAndThemesTestComposable(
                title = "override show dark\nisDarkTheme ${isSystemInDarkTheme()}",
                icons = getIcons(showNightIcon = false),
                backgroundColorOverride = getSectionCardColor()
            )
        }
    }

    @Composable
    private fun iconAndThemesTestComposable(
        title: String = "",
        @DrawableRes icons: List<Int>,
        backgroundColorOverride: Color? = null
    ) {
        DefaultStripeTheme {
            LazyColumn(
                modifier = Modifier
                    .width(150.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(title)
                }
                items(
                    items = icons,
                    key = {
                        "icon_$it"
                    }
                ) { iconRes ->
                    iconCard(
                        iconRes,
                        backgroundColorOverride
                    )
                }
            }
        }
    }

    private fun getIcons(showNightIcon: Boolean? = null): List<Int> {
        return CardBrand.orderedBrands.map {
            it.getCardBrandIconForHorizontalMode(showNightIcon = showNightIcon)
        }.plus(
            getLinkIcon(showNightIcon = showNightIcon)
        ).plus(
            getSepaIcon(showNightIcon = showNightIcon)
        )
    }
}
