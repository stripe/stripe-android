package com.stripe.example.activity

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.stripe.example.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateCardTokenActivityTest {

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<LauncherActivity> = activityScenarioRule()

    @get:Rule
    val idlingResourceRule: IdlingResourceRule = IdlingResourceRule("CreateCardTokenActivityTest")

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun createCardToken() {
        // launch create a card token activity
        onView(withId(R.id.examples)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click())
        )

        // fill out card details
        onView(withId(R.id.card_number_edit_text))
            .perform(typeText("4242424242424242"))
        onView(withId(R.id.expiry_date_edit_text))
            .perform(typeText("01/25"))
        onView(withId(R.id.cvc_edit_text))
            .perform(typeText("111"))

        // click create card button
        onView(withId(R.id.create_token_button))
            .perform(click())

        // check that card token info has been added to the tokens list
        onView(withId(R.id.tokens_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    }
}
