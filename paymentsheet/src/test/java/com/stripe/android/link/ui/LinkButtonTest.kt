package com.stripe.android.link.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.stripe.android.model.LinkBrand
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
    fun signedInButton_hasUsefulContentDescription_forLink() {
        composeRule.setContent {
            LinkButton(
                state = LinkButtonState.Email("email@email.com"),
                enabled = true,
                onClick = {},
                linkBrand = LinkBrand.Link,
            )
        }

        composeRule.onNodeWithTag(
            LinkButtonTestTag
        ).onChildren().assertAny(
            hasContentDescription("Pay with Link")
        )
    }

    @Test
    fun signedInButton_hasUsefulContentDescription_forOnelink() {
        composeRule.setContent {
            LinkButton(
                state = LinkButtonState.Email("email@email.com"),
                enabled = true,
                onClick = {},
                linkBrand = LinkBrand.Onelink,
            )
        }

        composeRule.onNodeWithTag(
            LinkButtonTestTag
        ).onChildren().assertAny(
            hasContentDescription("Pay with Onelink")
        )
    }

    @Test
    fun signedOutButton_hasUsefulContentDescription_forLink() {
        composeRule.setContent {
            LinkButton(
                state = LinkButtonState.Default,
                enabled = true,
                onClick = {},
                linkBrand = LinkBrand.Link,
            )
        }

        composeRule.onNodeWithTag(
            LinkButtonTestTag
        ).assertContentDescriptionContains("Pay with Link")
    }

    @Test
    fun signedOutButton_hasUsefulContentDescription_forOnelink() {
        composeRule.setContent {
            LinkButton(
                state = LinkButtonState.Default,
                enabled = true,
                onClick = {},
                linkBrand = LinkBrand.Onelink,
            )
        }

        composeRule.onNodeWithTag(
            LinkButtonTestTag
        ).assertContentDescriptionContains("Pay with Onelink")
    }

    @Test
    fun signedOutButton_displaysCompactContent_belowCompactWidth() {
        composeRule.setContent {
            Box(modifier = Modifier.requiredWidth(399.dp)) {
                LinkButton(
                    state = LinkButtonState.Default,
                    enabled = true,
                    onClick = {},
                    linkBrand = LinkBrand.Link,
                )
            }
        }

        composeRule.onNodeWithTag(LinkButtonCompactContentTestTag, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(LinkButtonFullContentTestTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun signedOutButton_displaysFullContent_aboveCompactWidth() {
        composeRule.setContent {
            Box(modifier = Modifier.requiredWidth(401.dp)) {
                LinkButton(
                    state = LinkButtonState.Default,
                    enabled = true,
                    onClick = {},
                    linkBrand = LinkBrand.Link,
                )
            }
        }

        composeRule.onNodeWithTag(LinkButtonFullContentTestTag, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(LinkButtonCompactContentTestTag, useUnmergedTree = true).assertDoesNotExist()
    }
}
