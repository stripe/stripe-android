package com.stripe.android.link.ui.signup

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkAccountManager
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.injection.SignUpViewModelSubcomponent
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.signup.SignUpViewModel.Companion.LOOKUP_DEBOUNCE_MS
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SignUpViewModelTest {
    private val defaultArgs = LinkActivityContract.Args(
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
    private val linkRepository = mock<LinkRepository>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When email is valid then lookup is triggered with delay`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(customerEmail = null))
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingEmail)

            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "valid@email.com"))
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingEmail)

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 1)

            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.VerifyingEmail)
        }

    @Test
    fun `When multiple valid emails entered quickly then lookup is triggered only for last one`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(customerEmail = null))
            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "first@email.com"))
            advanceTimeBy(LOOKUP_DEBOUNCE_MS / 2)

            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "second@email.com"))
            advanceTimeBy(LOOKUP_DEBOUNCE_MS / 2)

            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "third@email.com"))
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingEmail)

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 1)

            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.VerifyingEmail)

            val emailCaptor = argumentCaptor<String>()
            verify(linkRepository).lookupConsumer(emailCaptor.capture())

            assertThat(emailCaptor.allValues.size).isEqualTo(1)
            assertThat(emailCaptor.firstValue).isEqualTo("third@email.com")
        }

    @Test
    fun `When email is provided it should not trigger lookup and should collect phone number`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(customerEmail = "valid@email.com"))
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingPhone)

            verify(linkRepository, times(0)).lookupConsumer(any())
        }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val mockBuilder = mock<SignUpViewModelSubcomponent.Builder>()
        val mockSubComponent = mock<SignUpViewModelSubcomponent>()
        val vmToBeReturned = mock<SignUpViewModel>()

        whenever(mockBuilder.args(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.build()).thenReturn(mockSubComponent)
        whenever((mockSubComponent.signUpViewModel)).thenReturn(vmToBeReturned)

        val mockSavedStateRegistryOwner = mock<SavedStateRegistryOwner>()
        val mockSavedStateRegistry = mock<SavedStateRegistry>()
        val mockLifeCycle = mock<Lifecycle>()

        whenever(mockSavedStateRegistryOwner.savedStateRegistry).thenReturn(mockSavedStateRegistry)
        whenever(mockSavedStateRegistryOwner.lifecycle).thenReturn(mockLifeCycle)
        whenever(mockLifeCycle.currentState).thenReturn(Lifecycle.State.CREATED)

        val injector = object : Injector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as SignUpViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }
        WeakMapInjectorRegistry.register(injector, INJECTOR_KEY)
        val factory = SignUpViewModel.Factory(
            ApplicationProvider.getApplicationContext(),
            { defaultArgs }
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(SignUpViewModel::class.java)
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
        val factory = SignUpViewModel.Factory(
            ApplicationProvider.getApplicationContext(),
            { defaultArgs }
        )
        val factorySpy = spy(factory)

        assertNotNull(factorySpy.create(SignUpViewModel::class.java))
        verify(factorySpy).fallbackInitialize(
            argWhere {
                it.application == context
            }
        )
    }

    private fun createViewModel(args: LinkActivityContract.Args = defaultArgs) = SignUpViewModel(
        args = args,
        linkAccountManager = LinkAccountManager(linkRepository),
        logger = Logger.noop(),
        navigator = mock()
    )

    companion object {
        const val INJECTOR_KEY = "injectorKey"
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"

        const val DELAY = 100L
    }
}
