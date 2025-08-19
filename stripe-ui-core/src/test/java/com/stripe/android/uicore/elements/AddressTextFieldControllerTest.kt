package com.stripe.android.uicore.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AddressTextFieldControllerTest {
    @Test
    fun `when optional, initial field state should be Valid & Limitless`() = runTest {
        val controller = createAddressController(optional = true)

        controller.fieldState.test {
            assertThat(awaitItem()).isEqualTo(TextFieldStateConstants.Valid.Limitless)
        }
    }

    @Test
    fun `when required, initial field state should be Error & Blank`() = runTest {
        val controller = createAddressController(optional = false)

        controller.fieldState.test {
            assertThat(awaitItem()).isEqualTo(TextFieldStateConstants.Error.Blank)
        }
    }

    private fun createAddressController(
        optional: Boolean,
    ): AddressTextFieldController {
        return AddressTextFieldController(
            config = SimpleTextFieldConfig(
                label = resolvableString(value = "Name"),
                optional = optional,
            ),
        )
    }
}
