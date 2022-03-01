package com.stripe.android.paymentsheet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher


/**
 * Matches the [PaymentOptionsAdapter.ViewHolder]s card label.
 */
//fun matchFirstPaymentOptionsHolder(cardLabel: String): Matcher<RecyclerView.ViewHolder?> {
//    return object : TypeSafeMatcher<RecyclerView.ViewHolder?>() {
//        private var findFirst = false
//        override fun matchesSafely(customHolder: RecyclerView.ViewHolder?): Boolean {
//            return when (customHolder) {
//                is PaymentOptionsAdapter.SavedPaymentMethodViewHolder -> {
//                    if (!findFirst &&
//                        customHolder.binding.label.text.toString().contains(cardLabel)
//                    ) {
//                        findFirst = true
//                        true
//                    } else {
//                        false
//                    }
//                }
//                else -> {
//                    false
//                }
//            }
//        }
//
//        override fun describeTo(description: Description) {
//            description.appendText("matches card: $cardLabel")
//        }
//    }
//}


/**
 * Matches the [PaymentOptionsAdapter.ViewHolder]s card label.
 */
fun matchPaymentMethodHolder(
    composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule,
    lpm: String
): Matcher<RecyclerView.ViewHolder?> {
    return object : TypeSafeMatcher<RecyclerView.ViewHolder?>() {
        override fun matchesSafely(customHolder: RecyclerView.ViewHolder?): Boolean {
            return when (customHolder) {
                is AddPaymentMethodsAdapter.AddPaymentMethodViewHolder -> {
                    return try {
                        composeTestRule.onNodeWithText("Card").assertIsDisplayed()
                        true
                    } catch (e: AssertionError) {
                        false
                    }
                }
                else -> {
                    false
                }
            }
        }

        override fun describeTo(description: Description) {
            description.appendText("matches lpm: $lpm")
        }
    }
}
