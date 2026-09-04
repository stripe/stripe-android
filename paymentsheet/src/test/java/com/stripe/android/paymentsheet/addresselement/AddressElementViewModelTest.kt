package com.stripe.android.paymentsheet.addresselement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

internal class AddressElementViewModelTest {
    @Test
    fun `processing state survives view model provider recreation`() {
        val store = ViewModelStore()
        var factoryCalls = 0
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                factoryCalls += 1
                @Suppress("UNCHECKED_CAST")
                return AddressElementViewModel(
                    navigator = mock(),
                    inputAddressViewModelSubcomponentFactoryProvider = mock(),
                    autoCompleteViewModelSubcomponentFactoryProvider = mock(),
                ) as T
            }
        }
        val original = ViewModelProvider(store, factory)[AddressElementViewModel::class.java]
        original.processingState.tryStartProcessing()

        val recreated = ViewModelProvider(store, factory)[AddressElementViewModel::class.java]

        assertThat(recreated).isSameInstanceAs(original)
        assertThat(recreated.processingState.isProcessing.value).isTrue()
        assertThat(factoryCalls).isEqualTo(1)
    }
}
