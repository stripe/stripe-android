package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class SelfieWarmupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mock<NavController>()
    private val mockIdentityViewModel = mock<IdentityViewModel>()

    @Test
    fun verifyContentVisibleAndButtonClick() {
        composeTestRule.setContent {
            SelfieWarmupScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
            )
        }

        with(composeTestRule) {
            onNodeWithTag(SELFIE_WARMUP_CONTENT_TAG).assertExists()

            onNodeWithTag(SELFIE_CONTINUE_BUTTON_TAG).onChildAt(0).performClick()
            verify(mockNavController).navigate(
                eq(SelfieDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }
}
