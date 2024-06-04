package com.stripe.android.view

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import org.junit.Rule
import org.junit.Test

class CardInputWidgetTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun restoresViewWidthsWhenUsingSaveableState() {
        val cardInputWidgetState = mutableStateOf(value = State.Show)

        composeTestRule.activity.setTheme(R.style.StripeDefaultTheme)

        PaymentConfiguration.init(composeTestRule.activity, "publishable_key")

        composeTestRule.setContent {
            val stateHolder = rememberSaveableStateHolder()

            val state by cardInputWidgetState

            if (state == State.Show) {
                stateHolder.SaveableStateProvider("stripe_elements") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            CardInputWidget(context).apply {
                                id = VIEW_ID
                                postalCodeRequired = true
                            }
                        },
                        update = {
                            // do nothing
                        },
                        onRelease = {
                        },
                    )
                }
            }
        }

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        onView(withHint("1234 1234 1234 1234")).perform(typeText("4242 4242 4242 4242"))
        onView(withHint("MM/YY")).perform(typeText("04/25"))

        Espresso.onIdle()

        onView(withHint("CVC")).perform(typeText("404"))
        onView(withHint("Postal code")).perform(typeText("A1B3C7"))

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        onView(withText("4242 4242 4242 4242")).check(matches(isDisplayed()))
        onView(withText("04/25")).check(matches(isDisplayed()))
        onView(withText("404")).check(matches(isDisplayed()))
        onView(withText("A1B3C7")).check(matches(isDisplayed()))

        cardInputWidgetState.value = State.Hide

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        cardInputWidgetState.value = State.Show

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        onView(withText("4242 4242 4242 4242")).check(matches(isDisplayed()))
        onView(withText("04/25")).check(matches(isDisplayed()))
        onView(withText("404")).check(matches(isDisplayed()))
        onView(withText("A1B3C7")).check(matches(isDisplayed()))
    }

    private enum class State {
        Show,
        Hide,
    }

    private companion object {
        const val VIEW_ID = 12345
    }
}
