package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.ui.core.injection.NonFallbackInjector
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AddressElementViewModelTest {
    private val defaultArgs = AddressElementActivityContract.Args(
        PaymentIntentFixtures.PI_SUCCEEDED,
        PaymentSheetFixtures.CONFIG_CUSTOMER,
        AddressElementActivityContract.Args.InjectionParams(
            INJECTOR_KEY,
            setOf(PRODUCT_USAGE),
            true
        )
    )

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val vmToBeReturned = mock<AddressElementViewModel>()

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as AddressElementViewModel.Factory
                factory.viewModel = vmToBeReturned
            }
        }
        WeakMapInjectorRegistry.register(injector, INJECTOR_KEY)

        val factory = AddressElementViewModel.Factory(
            { ApplicationProvider.getApplicationContext() },
            { defaultArgs }
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(AddressElementViewModel::class.java)
        verify(factorySpy, times(0)).fallbackInitialize(any())
        Truth.assertThat(createdViewModel).isEqualTo(vmToBeReturned)

        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val factory = AddressElementViewModel.Factory(
            { context },
            { defaultArgs }
        )
        val factorySpy = spy(factory)

        assertNotNull(factorySpy.create(AddressElementViewModel::class.java))
        verify(factorySpy).fallbackInitialize(
            argWhere {
                it.application == context
            }
        )
    }

    private companion object {
        const val INJECTOR_KEY = "injectorKey"
        const val PRODUCT_USAGE = "productUsage"
    }
}
