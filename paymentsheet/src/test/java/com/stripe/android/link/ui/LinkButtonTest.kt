package com.stripe.android.link.ui

import android.os.Build
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class LinkButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun signedInButton_hasUsefulContentDescription() {
        composeRule.setContent {
            LinkButton(
                state = LinkButtonState.Email("email@email.com"),
                enabled = true,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(
            LinkButtonTestTag
        ).onChildren().assertAny(
            hasContentDescription("Pay with Link")
        )
    }

    @Test
    fun signedOutButton_hasUsefulContentDescription() {
        composeRule.setContent {
            LinkButton(
                state = LinkButtonState.Default,
                enabled = true,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(
            LinkButtonTestTag
        ).assertContentDescriptionContains("Pay with Link")
    }
}
