package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.utils.FakeAndroidKeyStore
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
    private val config = LinkPaymentLauncher.Configuration(
        stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
        merchantName = MERCHANT_NAME,
        customerName = CUSTOMER_NAME,
        customerEmail = CUSTOMER_EMAIL,
        customerPhone = CUSTOMER_PHONE,
        customerBillingCountryCode = CUSTOMER_BILLING_COUNTRY_CODE,
        shippingValues = null,
    )

    private val defaultArgs = LinkActivityContract.Args(
        config,
        null,
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
            createArgs(StripeIntentFixtures.PI_SUCCEEDED.copy(clientSecret = null))
        )
        verify(navigator).dismiss(argWhere { it is LinkActivityResult.Failed })

        reset(navigator)
        createViewModel(
            createArgs(StripeIntentFixtures.PI_SUCCEEDED.copy(id = null))
        )
        verify(navigator).dismiss(argWhere { it is LinkActivityResult.Failed })
        reset(navigator)

        reset(navigator)
        createViewModel(
            createArgs(StripeIntentFixtures.PI_SUCCEEDED.copy(amount = null))
        )
        verify(navigator).dismiss(argWhere { it is LinkActivityResult.Failed })

        reset(navigator)
        createViewModel(
            createArgs(StripeIntentFixtures.PI_SUCCEEDED.copy(currency = null))
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

    @Test
    fun `Navigating back on root screen dismisses Link, but doesn't log out user`() {
        val viewModel = createViewModel()
        setupNavigation(hasBackStack = false)

        viewModel.onBackPressed()

        verify(navigator).onBack(userInitiated = eq(true))
        verify(linkAccountManager, never()).logout()
    }

    @Test
    fun `Navigating back on child screen navigates back, but doesn't dismiss Link`() {
        val viewModel = createViewModel()
        setupNavigation(hasBackStack = true)

        viewModel.onBackPressed()

        verify(navigator, never()).dismiss(any())
        verify(linkAccountManager, never()).logout()
    }

    @Test
    fun `Navigating back is prevented when back navigation is disabled`() {
        val viewModel = createViewModel()
        setupNavigation(userNavigationEnabled = false)

        viewModel.onBackPressed()

        verify(navigator, never()).dismiss(any())
        verify(linkAccountManager, never()).logout()
    }

    @Test
    fun `Logging out logs out the user and dismisses Link`() {
        val viewModel = createViewModel()

        viewModel.logout()

        verify(navigator).cancel(eq(Reason.LoggedOut))
        verify(linkAccountManager).logout()
    }

    private fun createViewModel(args: LinkActivityContract.Args = defaultArgs) =
        LinkActivityViewModel(
            args,
            linkAccountManager,
            navigator,
            confirmationManager
        )

    private fun createArgs(
        stripeIntent: StripeIntent
    ) = defaultArgs.copy(
        configuration = config.copy(
            stripeIntent = stripeIntent
        )
    )

    private fun setupNavigation(
        hasBackStack: Boolean = false,
        userNavigationEnabled: Boolean = true
    ) {
        val mockNavController = mock<NavHostController> {
            on { popBackStack() } doReturn hasBackStack
        }
        whenever(navigator.userNavigationEnabled).thenReturn(userNavigationEnabled)
        whenever(navigator.navigationController).thenReturn(mockNavController)
    }

    private companion object {
        const val INJECTOR_KEY = "injectorKey"
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"
        const val CUSTOMER_NAME = "Customer"
        const val CUSTOMER_PHONE = "1234567890"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "US"
    }
}
