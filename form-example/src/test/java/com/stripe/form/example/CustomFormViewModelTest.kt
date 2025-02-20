package com.stripe.form.example

import androidx.compose.ui.text.AnnotatedString
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.form.ContentSpec
import com.stripe.form.example.ui.theme.customform.CustomFormViewModel
import com.stripe.form.example.ui.theme.customform.WelcomeSpec
import com.stripe.form.text.TextSpec
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomFormViewModelTest {
    @Test
    fun test() = runTest {
        val vm = CustomFormViewModel()

        vm.state.test {
            val form = awaitItem().form
            ContentSpecMatcher.matches<WelcomeSpec>(
                spec = form?.content?.get(0)!!
            ) { spec ->
                assertThat(spec.names).containsExactly("Stripe", "Mobile", "Elements")
            }
        }
    }
}