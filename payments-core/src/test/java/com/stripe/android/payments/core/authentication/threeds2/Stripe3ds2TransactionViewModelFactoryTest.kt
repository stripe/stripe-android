package com.stripe.android.payments.core.authentication.threeds2

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.core.injection.Stripe3ds2TransactionViewModelSubcomponent
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2TransactionViewModelFactoryTest {
    // FragmentScenario is needed to provide SavedStateRegistryOwner required
    // by Stripe3ds2TransactionViewModelFactory
    private val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
        TestFragment()
    }

    internal class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = FrameLayout(inflater.context)
    }

    @Test
    fun `Stripe3ds2TransactionViewModelFactory gets initialized by Injector when Injector is available`() {
        scenario.onFragment { savedStateRegistryOwner ->
            // The reason the ViewModel cannot be mocked here is because
            // AbstractSavedStateViewModelFactory will call viewModel.setTagIfAbsent, which accesses
            // ViewModel.mBagOfTags that's initialized in base class.
            // Mocking it would leave this field null, causing an NPE.
            val viewModel = Stripe3ds2TransactionViewModel(
                mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), false
            )
            val mockBuilder = mock<Stripe3ds2TransactionViewModelSubcomponent.Builder>()
            val mockSubcomponent = mock<Stripe3ds2TransactionViewModelSubcomponent>()

            whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
            whenever(mockBuilder.application(any())).thenReturn(mockBuilder)
            whenever(mockBuilder.savedStateHandle(any())).thenReturn(mockBuilder)
            whenever(mockBuilder.args(any())).thenReturn(mockBuilder)
            whenever(mockSubcomponent.viewModel).thenReturn(viewModel)

            val injector = object : Injector {
                override fun inject(injectable: Injectable<*>) {
                    val factory = injectable as Stripe3ds2TransactionViewModelFactory
                    factory.subComponentBuilder = mockBuilder
                }
            }
            WeakMapInjectorRegistry.register(injector, INJECTOR_KEY)

            val factory = Stripe3ds2TransactionViewModelFactory(
                { ApplicationProvider.getApplicationContext() },
                savedStateRegistryOwner,
                { ARGS }
            )
            val factorySpy = spy(factory)
            val createdViewModel =
                factorySpy.create(Stripe3ds2TransactionViewModel::class.java)
            verify(factorySpy, times(0)).fallbackInitialize(any())
            assertThat(createdViewModel).isEqualTo(viewModel)

            WeakMapInjectorRegistry.clear()
        }
    }

    @Test
    fun `Stripe3ds2TransactionViewModelFactory gets initialized with fallback when no Injector is available`() {
        scenario.onFragment { savedStateRegistryOwner ->
            val application = ApplicationProvider.getApplicationContext<Application>()
            val factory = Stripe3ds2TransactionViewModelFactory(
                { application },
                savedStateRegistryOwner,
                { ARGS }
            )
            val factorySpy = spy(factory)

            assertNotNull(factorySpy.create(Stripe3ds2TransactionViewModel::class.java))
            verify(factorySpy).fallbackInitialize(
                argWhere {
                    it.application == application &&
                        it.enableLogging == ENABLE_LOGGING &&
                        it.productUsage == PRODUCT_USAGE &&
                        it.publishableKey == ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
                }
            )
        }
    }

    private companion object {
        val INJECTOR_KEY: String = WeakMapInjectorRegistry.nextKey("TestKey")
        const val ENABLE_LOGGING = false
        val PRODUCT_USAGE = setOf("TestProductUsage")
        val ARGS = Stripe3ds2TransactionContract.Args(
            SdkTransactionId.create(),
            PaymentAuthConfig.Stripe3ds2Config(
                timeout = 5,
                PaymentAuthConfig.Stripe3ds2UiCustomization(
                    StripeUiCustomization()
                )
            ),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.nextActionData
                as StripeIntent.NextActionData.SdkData.Use3DS2,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            enableLogging = ENABLE_LOGGING,
            statusBarColor = null,
            injectorKey = INJECTOR_KEY,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            PRODUCT_USAGE
        )
    }
}
