package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.utils.FakeAndroidKeyStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LinkActivityViewModelTest {
    private val defaultArgs = LinkActivityContract.Args(
        StripeIntentFixtures.PI_SUCCEEDED,
        MERCHANT_NAME,
        CUSTOMER_EMAIL,
        LinkActivityContract.Args.InjectionParams(
            INJECTOR_KEY,
            setOf(PRODUCT_USAGE),
            true,
            PUBLISHABLE_KEY,
            STRIPE_ACCOUNT_ID
        )
    )

    private val linkAccountManager = mock<LinkAccountManager>()
    private val confirmationManager = mock<ConfirmationManager>()
    private val navigator = mock<Navigator>()

    init {
        FakeAndroidKeyStore.setup()
    }

    @Test
    fun `When StripeIntent is missing required fields then it dismisses with error`() = runTest {
        createViewModel(
            defaultArgs.copy(StripeIntentFixtures.PI_SUCCEEDED.copy(clientSecret = null))
        )
        verify(navigator).dismiss(argWhere { it is LinkActivityResult.Failed })

        reset(navigator)
        createViewModel(
            defaultArgs.copy(StripeIntentFixtures.PI_SUCCEEDED.copy(id = null))
        )
        verify(navigator).dismiss(argWhere { it is LinkActivityResult.Failed })
        reset(navigator)

        reset(navigator)
        createViewModel(
            defaultArgs.copy(StripeIntentFixtures.PI_SUCCEEDED.copy(amount = null))
        )
        verify(navigator).dismiss(argWhere { it is LinkActivityResult.Failed })

        reset(navigator)
        createViewModel(
            defaultArgs.copy(StripeIntentFixtures.PI_SUCCEEDED.copy(currency = null))
        )
        verify(navigator).dismiss(argWhere { it is LinkActivityResult.Failed })
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val vmToBeReturned = mock<LinkActivityViewModel>()

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as LinkActivityViewModel.Factory
                factory.viewModel = vmToBeReturned
            }
        }
        WeakMapInjectorRegistry.register(injector, INJECTOR_KEY)

        val factory = LinkActivityViewModel.Factory(
            { ApplicationProvider.getApplicationContext() },
            { defaultArgs }
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(LinkActivityViewModel::class.java)
        verify(factorySpy, times(0)).fallbackInitialize(any())
        assertThat(createdViewModel).isEqualTo(vmToBeReturned)

        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runTest {
        val mockSavedStateRegistryOwner = mock<SavedStateRegistryOwner>()
        val mockSavedStateRegistry = mock<SavedStateRegistry>()
        val mockLifeCycle = mock<Lifecycle>()

        whenever(mockSavedStateRegistryOwner.savedStateRegistry).thenReturn(mockSavedStateRegistry)
        whenever(mockSavedStateRegistryOwner.lifecycle).thenReturn(mockLifeCycle)
        whenever(mockLifeCycle.currentState).thenReturn(Lifecycle.State.CREATED)

        val context = ApplicationProvider.getApplicationContext<Application>()
        val factory = LinkActivityViewModel.Factory(
            { ApplicationProvider.getApplicationContext() },
            { defaultArgs }
        )
        val factorySpy = spy(factory)

        assertNotNull(factorySpy.create(LinkActivityViewModel::class.java))
        verify(factorySpy).fallbackInitialize(
            argWhere {
                it.application == context
            }
        )
    }

    private fun createViewModel(args: LinkActivityContract.Args = defaultArgs) =
        LinkActivityViewModel(
            args,
            linkAccountManager,
            navigator,
            confirmationManager
        )

    private companion object {
        const val INJECTOR_KEY = "injectorKey"
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"
    }
}
