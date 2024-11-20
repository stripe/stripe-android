package com.stripe.android.paymentsheet

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.paymentsheet.ui.NewPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.image.TEST_TAG_IMAGE_FROM_URL
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentMethodsUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val workingUrl = "working-url"
    private val brokenUrl = "broken-url"
    private val simpleBitmap = Bitmap.createBitmap(
        10,
        10,
        Bitmap.Config.ARGB_8888
    )

    @Test
    fun whenIconUrlIsPresent_iconUrlIsUsed() = runTest {
        testIcons(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_affirm,
            iconUrl = workingUrl,
            expectedTestTag = TEST_TAG_IMAGE_FROM_URL
        )
    }

    @Test
    fun whenIconUrlIsMissing_iconFromResIsUsed() = runTest {
        testIcons(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_affirm,
            iconUrl = null,
            expectedTestTag = TEST_TAG_ICON_FROM_RES
        )
    }

    @Test
    fun whenIconUrlIsPresent_failsToLoad_fallsBackToIconFromRes() = runTest {
        testIcons(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_affirm,
            iconUrl = brokenUrl,
            expectedTestTag = TEST_TAG_ICON_FROM_RES
        )
    }

    suspend fun testIcons(
        iconRes: Int?,
        iconUrl: String?,
        expectedTestTag: String,
    ) {
        val imageLoader = mock<StripeImageLoader>().also {
            whenever(it.load(eq(workingUrl), any(), any())).thenReturn(
                Result.success(
                    simpleBitmap
                )
            )
        }.also {
            whenever(it.load(eq(brokenUrl), any(), any())).thenReturn(Result.failure(Throwable()))
        }

        val paymentMethods = listOf(
            SupportedPaymentMethod(
                code = "example_pm",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_affirm,
                iconResource = iconRes ?: 0,
                lightThemeIconUrl = iconUrl,
                darkThemeIconUrl = null,
                iconRequiresTinting = false,
            )
        )
        composeTestRule.setContent {
            NewPaymentMethodTabLayoutUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                incentive = null,
                onItemSelectedListener = {},
                imageLoader = imageLoader,
            )
        }

        with(composeTestRule) {
            onNodeWithTag(expectedTestTag, useUnmergedTree = true).assertExists()
        }
    }
}
