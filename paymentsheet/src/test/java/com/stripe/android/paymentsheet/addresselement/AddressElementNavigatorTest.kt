package com.stripe.android.paymentsheet.addresselement

import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class AddressElementNavigatorTest {
    @Test
    fun `back pops a child screen`() {
        val navigationController = mock<NavHostController>()
        whenever(navigationController.popBackStack()).thenReturn(true)
        val navigator = NavHostAddressElementNavigator().apply {
            this.navigationController = navigationController
        }

        assertThat(navigator.onBack()).isTrue()
        verify(navigationController).popBackStack()
    }

    @Test
    fun `back from the root screen is unhandled`() {
        val navigationController = mock<NavHostController>()
        whenever(navigationController.popBackStack()).thenReturn(false)
        val navigator = NavHostAddressElementNavigator().apply {
            this.navigationController = navigationController
        }

        assertThat(navigator.onBack()).isFalse()
        verify(navigationController).popBackStack()
    }
}
