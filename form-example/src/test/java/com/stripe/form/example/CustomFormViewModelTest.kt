package com.stripe.form.example

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.form.example.ui.theme.customform.CustomFormViewModel
import com.stripe.form.example.ui.theme.customform.WelcomeSpec
import com.stripe.form.fields.DropdownSpec
import com.stripe.form.fields.TextFieldSpec
import com.stripe.form.testutils.contains
import com.stripe.form.testutils.contentAt
import com.stripe.form.testutils.contentOf
import com.stripe.form.testutils.hasContentSize
import com.stripe.form.text.TextSpec
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomFormViewModelTest {
    @Test
    fun `test contents`() = runTest {
        val vm = CustomFormViewModel()

        vm.state.test {
            val form = awaitItem().form
            form?.hasContentSize(9)
            form?.contentAt<WelcomeSpec>(0) { spec ->
                assertThat(spec.names).containsExactly("Stripe", "Mobile", "Elements")
            }

            form?.contains(
                contentOf<WelcomeSpec> { spec ->
                    assertThat(spec.names).containsExactly("Stripe", "Mobile", "Elements")
                },
                contentOf<TextSpec> { spec ->
                    assertThat(spec.text).isEqualTo(AnnotatedString("What is your name?"))
                },
                contentOf<TextFieldSpec>(),
                contentOf<DropdownSpec> { spec ->
                    assertThat(spec.state.options).containsExactly(
                        DropdownSpec.Option(
                            rawValue = "CA",
                            displayValue = "Canada 🇨🇦"
                        ),
                        DropdownSpec.Option(
                            rawValue = "US",
                            displayValue = "United States of America"
                        ),
                    )
                }
            )
        }
    }

    @Test
    fun `test content change`() = runTest {
        val vm = CustomFormViewModel()

        vm.state.test {
            val state = awaitItem()
            assertThat(state.valid).isFalse()

            state.form?.onValueChange(
                key = vm.nameKey,
                value = TextFieldValue("hello")
            )
            state.form?.onValueChange(
                key = vm.checkboxKey,
                value = true
            )
            state.form?.onValueChange(
                key = vm.sliderKey,
                value = .6f
            )
            state.form?.onValueChange(
                key = vm.dropdownKey,
                value = "US"
            )

            assertThat(awaitItem().valid).isTrue()


        }
    }
}
