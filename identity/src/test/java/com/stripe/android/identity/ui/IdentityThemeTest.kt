package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet.Configuration.ButtonColor
import com.stripe.android.identity.IdentityVerificationSheet.Configuration.ButtonShape
import com.stripe.android.identity.IdentityVerificationSheet.Configuration.PrimaryButtonStyle
import com.stripe.android.identity.IdentityVerificationSheet.Configuration.SecondaryButtonStyle
import com.stripe.android.identity.TestApplication
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
internal class IdentityThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `null styles preserve host button defaults`() = runScenario {
        assertThat(primaryBackground).isEqualTo(defaultPrimaryBackground)
        assertThat(primaryText).isEqualTo(defaultPrimaryText)
        assertThat(primaryShape).isEqualTo(defaultShape)
        assertThat(primaryHeight).isNull()
        assertThat(primaryElevation).isNull()
        assertThat(primaryTextCase).isEqualTo(uppercaseButtonText)
        assertThat(secondaryBackground).isEqualTo(defaultSecondaryBackground)
        assertThat(secondaryText).isEqualTo(defaultSecondaryText)
        assertThat(secondaryShape).isEqualTo(defaultShape)
        assertThat(secondaryHeight).isNull()
        assertThat(secondaryElevation).isNull()
        assertThat(secondaryTextCase).isEqualTo(uppercaseButtonText)
        assertThat(secondaryHasBorder).isTrue()
    }

    @Test
    fun `styles resolve light colors and numeric dimensions`() = runScenario(
        primaryButtonStyle = primaryButtonStyle(),
        secondaryButtonStyle = secondaryButtonStyle()
    ) {
        assertThat(materialPrimary).isEqualTo(Color(brandColor))
        assertThat(primaryBackground).isEqualTo(Color(lightPrimaryBackground))
        assertThat(primaryText).isEqualTo(Color(lightPrimaryText))
        assertThat(primaryShape).isEqualTo(RoundedCornerShape(primaryRadius.dp))
        assertThat(primaryHeight).isEqualTo(configuredPrimaryHeight.dp)
        assertThat(primaryElevation).isEqualTo(configuredPrimaryElevation.dp)
        assertThat(primaryTextCase).isEqualTo(buttonText)
        assertThat(secondaryBackground).isEqualTo(Color(lightSecondaryBackground))
        assertThat(secondaryText).isEqualTo(Color(lightSecondaryText))
        assertThat(secondaryShape).isEqualTo(RoundedCornerShape(secondaryRadius.dp))
        assertThat(secondaryHeight).isEqualTo(configuredSecondaryHeight.dp)
        assertThat(secondaryElevation).isEqualTo(configuredSecondaryElevation.dp)
        assertThat(secondaryTextCase).isEqualTo(buttonText)
        assertThat(secondaryHasBorder).isFalse()
    }

    @Test
    @Config(qualifiers = "night")
    fun `styles resolve dark colors`() = runScenario(
        primaryButtonStyle = primaryButtonStyle(),
        secondaryButtonStyle = secondaryButtonStyle()
    ) {
        assertThat(primaryBackground).isEqualTo(Color(darkPrimaryBackground))
        assertThat(primaryText).isEqualTo(Color(darkPrimaryText))
        assertThat(secondaryBackground).isEqualTo(Color(darkSecondaryBackground))
        assertThat(secondaryText).isEqualTo(Color(darkSecondaryText))
    }

    @Test
    fun `missing fields and appearance colors fall back independently`() = runScenario(
        primaryButtonStyle = PrimaryButtonStyle(
            backgroundColor = ButtonColor(light = null, dark = darkPrimaryBackground),
            textColor = ButtonColor(light = lightPrimaryText, dark = null),
            shape = null,
            elevationDp = null,
            uppercase = null
        )
    ) {
        assertThat(primaryBackground).isEqualTo(defaultPrimaryBackground)
        assertThat(primaryText).isEqualTo(Color(lightPrimaryText))
        assertThat(primaryShape).isEqualTo(defaultShape)
        assertThat(primaryHeight).isNull()
        assertThat(primaryElevation).isNull()
    }

    @Test
    fun `secondary style does not affect primary button`() = runScenario(
        secondaryButtonStyle = secondaryButtonStyle()
    ) {
        assertThat(primaryBackground).isEqualTo(defaultPrimaryBackground)
        assertThat(primaryText).isEqualTo(defaultPrimaryText)
        assertThat(primaryShape).isEqualTo(defaultShape)
        assertThat(primaryHeight).isNull()
        assertThat(primaryElevation).isNull()
        assertThat(secondaryBackground).isEqualTo(Color(lightSecondaryBackground))
        assertThat(secondaryText).isEqualTo(Color(lightSecondaryText))
    }

    @Test
    fun `primary style does not affect secondary button`() = runScenario(
        primaryButtonStyle = primaryButtonStyle()
    ) {
        assertThat(primaryBackground).isEqualTo(Color(lightPrimaryBackground))
        assertThat(primaryText).isEqualTo(Color(lightPrimaryText))
        assertThat(secondaryBackground).isEqualTo(defaultSecondaryBackground)
        assertThat(secondaryText).isEqualTo(defaultSecondaryText)
        assertThat(secondaryShape).isEqualTo(defaultShape)
        assertThat(secondaryHeight).isNull()
        assertThat(secondaryElevation).isNull()
    }

    @Test
    fun `custom styles preserve disabled colors`() = runScenario(
        primaryButtonStyle = primaryButtonStyle(),
        secondaryButtonStyle = secondaryButtonStyle()
    ) {
        assertThat(disabledPrimaryBackground).isEqualTo(defaultDisabledPrimaryBackground)
        assertThat(disabledPrimaryText).isEqualTo(defaultDisabledPrimaryText)
        assertThat(disabledSecondaryBackground).isEqualTo(defaultDisabledSecondaryBackground)
        assertThat(disabledSecondaryText).isEqualTo(defaultDisabledSecondaryText)
    }

    private fun runScenario(
        primaryButtonStyle: PrimaryButtonStyle? = null,
        secondaryButtonStyle: SecondaryButtonStyle? = null,
        block: ThemeSnapshot.() -> Unit
    ) {
        lateinit var snapshot: ThemeSnapshot
        composeRule.setContent {
            IdentityTheme(
                brandColor = brandColor,
                primaryButtonStyle = primaryButtonStyle,
                secondaryButtonStyle = secondaryButtonStyle
            ) {
                val primaryStyle = IdentityButtonTheme.primary
                val secondaryStyle = IdentityButtonTheme.secondary
                val defaultPrimaryColors = ButtonDefaults.buttonColors()
                val defaultSecondaryColors = ButtonDefaults.outlinedButtonColors()
                snapshot = ThemeSnapshot(
                    materialPrimary = MaterialTheme.colors.primary,
                    primaryBackground = primaryStyle.colors.backgroundColor(enabled = true).value,
                    primaryText = primaryStyle.colors.contentColor(enabled = true).value,
                    primaryShape = primaryStyle.shape,
                    primaryHeight = primaryStyle.height,
                    primaryElevation = primaryStyle.elevationDp,
                    primaryTextCase = primaryStyle.text(buttonText, uppercase = true),
                    secondaryBackground = secondaryStyle.colors.backgroundColor(enabled = true).value,
                    secondaryText = secondaryStyle.colors.contentColor(enabled = true).value,
                    secondaryShape = secondaryStyle.shape,
                    secondaryHeight = secondaryStyle.height,
                    secondaryElevation = secondaryStyle.elevationDp,
                    secondaryTextCase = secondaryStyle.text(buttonText, uppercase = true),
                    secondaryHasBorder = secondaryStyle.border != null,
                    disabledPrimaryBackground = primaryStyle.colors.backgroundColor(enabled = false).value,
                    disabledPrimaryText = primaryStyle.colors.contentColor(enabled = false).value,
                    disabledSecondaryBackground = secondaryStyle.colors.backgroundColor(enabled = false).value,
                    disabledSecondaryText = secondaryStyle.colors.contentColor(enabled = false).value,
                    defaultPrimaryBackground = defaultPrimaryColors.backgroundColor(enabled = true).value,
                    defaultPrimaryText = defaultPrimaryColors.contentColor(enabled = true).value,
                    defaultSecondaryBackground = defaultSecondaryColors.backgroundColor(enabled = true).value,
                    defaultSecondaryText = defaultSecondaryColors.contentColor(enabled = true).value,
                    defaultDisabledPrimaryBackground = defaultPrimaryColors.backgroundColor(enabled = false).value,
                    defaultDisabledPrimaryText = defaultPrimaryColors.contentColor(enabled = false).value,
                    defaultDisabledSecondaryBackground = defaultSecondaryColors.backgroundColor(enabled = false).value,
                    defaultDisabledSecondaryText = defaultSecondaryColors.contentColor(enabled = false).value,
                    defaultShape = MaterialTheme.shapes.small
                )
            }
        }
        composeRule.waitForIdle()
        block(snapshot)
    }

    private fun primaryButtonStyle() = PrimaryButtonStyle(
        backgroundColor = ButtonColor(lightPrimaryBackground, darkPrimaryBackground),
        textColor = ButtonColor(lightPrimaryText, darkPrimaryText),
        shape = ButtonShape(primaryRadius, configuredPrimaryHeight),
        elevationDp = configuredPrimaryElevation,
        uppercase = false
    )

    private fun secondaryButtonStyle() = SecondaryButtonStyle(
        backgroundColor = ButtonColor(lightSecondaryBackground, darkSecondaryBackground),
        textColor = ButtonColor(lightSecondaryText, darkSecondaryText),
        shape = ButtonShape(secondaryRadius, configuredSecondaryHeight),
        elevationDp = configuredSecondaryElevation,
        uppercase = false,
        showBorder = false
    )

    private data class ThemeSnapshot(
        val materialPrimary: Color,
        val primaryBackground: Color,
        val primaryText: Color,
        val primaryShape: Shape,
        val primaryHeight: Dp?,
        val primaryElevation: Dp?,
        val primaryTextCase: String,
        val secondaryBackground: Color,
        val secondaryText: Color,
        val secondaryShape: Shape,
        val secondaryHeight: Dp?,
        val secondaryElevation: Dp?,
        val secondaryTextCase: String,
        val secondaryHasBorder: Boolean,
        val disabledPrimaryBackground: Color,
        val disabledPrimaryText: Color,
        val disabledSecondaryBackground: Color,
        val disabledSecondaryText: Color,
        val defaultPrimaryBackground: Color,
        val defaultPrimaryText: Color,
        val defaultSecondaryBackground: Color,
        val defaultSecondaryText: Color,
        val defaultDisabledPrimaryBackground: Color,
        val defaultDisabledPrimaryText: Color,
        val defaultDisabledSecondaryBackground: Color,
        val defaultDisabledSecondaryText: Color,
        val defaultShape: Shape
    )

    private companion object {
        val brandColor = AndroidColor.rgb(100, 101, 102)
        val lightPrimaryBackground = AndroidColor.rgb(1, 2, 3)
        val lightPrimaryText = AndroidColor.rgb(4, 5, 6)
        val darkPrimaryBackground = AndroidColor.rgb(7, 8, 9)
        val darkPrimaryText = AndroidColor.rgb(10, 11, 12)
        val lightSecondaryBackground = AndroidColor.rgb(13, 14, 15)
        val lightSecondaryText = AndroidColor.rgb(16, 17, 18)
        val darkSecondaryBackground = AndroidColor.rgb(19, 20, 21)
        val darkSecondaryText = AndroidColor.rgb(22, 23, 24)
        const val primaryRadius = 31f
        const val secondaryRadius = 7f
        const val configuredPrimaryHeight = 56f
        const val configuredSecondaryHeight = 48f
        const val configuredPrimaryElevation = 3f
        const val configuredSecondaryElevation = 5f
        const val buttonText = "Button text"
        const val uppercaseButtonText = "BUTTON TEXT"
    }
}
