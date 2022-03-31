package com.stripe.android.link.ui.wallet

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.confirmation.PaymentConfirmationCallback
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.PaymentDetailsFixtures
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class WalletViewModelTest {
    private val linkAccount = mock<LinkAccount>()
    private val args = mock<LinkActivityContract.Args>()
    private lateinit var linkRepository: LinkRepository
    private val linkAccountManager = mock<LinkAccountManager>()
    private val navigator = mock<Navigator>()
    private val confirmationManager = mock<ConfirmationManager>()
    private val logger = Logger.noop()

    @Before
    fun before() {
        linkRepository = mock()
        whenever(args.stripeIntent).thenReturn(StripeIntentFixtures.PI_SUCCEEDED)
    }

    @Test
    fun `On initialization payment details are loaded`() = runTest {
        val card1 = mock<ConsumerPaymentDetails.Card>()
        val card2 = mock<ConsumerPaymentDetails.Card>()
        val paymentDetails = mock<ConsumerPaymentDetails>()
        whenever(paymentDetails.paymentDetails).thenReturn(listOf(card1, card2))

        whenever(linkRepository.listPaymentDetails(anyOrNull()))
            .thenReturn(Result.success(paymentDetails))

        val viewModel = createViewModel()

        assertThat(viewModel.paymentDetails.value).containsExactly(card1, card2)
    }

    @Test
    fun `On initialization when no payment details then navigate to AddPaymentMethod`() = runTest {
        val response = mock<ConsumerPaymentDetails>()
        whenever(response.paymentDetails).thenReturn(emptyList())

        whenever(linkRepository.listPaymentDetails(anyOrNull()))
            .thenReturn(Result.success(response))

        createViewModel()

        verify(navigator).navigateTo(LinkScreen.PaymentMethod, false)
    }

    @Test
    fun `When PaymentIntent then button label displays amount`() {
        val label = createViewModel().payButtonLabel(getContext().resources)

        assertThat(label).isEqualTo("Pay $10.99")
    }

    @Test
    fun `When SetupIntent then button label displays set up`() {
        whenever(args.stripeIntent).thenReturn(StripeIntentFixtures.SI_NEXT_ACTION_REDIRECT)

        val label = createViewModel().payButtonLabel(getContext().resources)

        assertThat(label).isEqualTo("Set up")
    }

    @Test
    fun `completePayment starts payment confirmation`() {
        val clientSecret = "client_secret"
        whenever(linkAccount.clientSecret).thenReturn(clientSecret)
        val paymentDetails = PaymentDetailsFixtures.CONSUMER_PAYMENT_DETAILS.paymentDetails.first()

        createViewModel().completePayment(paymentDetails)

        val paramsCaptor = argumentCaptor<ConfirmStripeIntentParams>()
        verify(confirmationManager).confirmStripeIntent(paramsCaptor.capture(), any())

        assertThat(paramsCaptor.firstValue).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParams.createLink(
                    paymentDetails.id,
                    clientSecret
                ),
                StripeIntentFixtures.PI_SUCCEEDED.clientSecret!!
            )
        )
    }

    @Test
    fun `completePayment dismisses on success`() = runTest {
        whenever(confirmationManager.confirmStripeIntent(any(), any())).thenAnswer { invocation ->
            (invocation.getArgument(1) as? PaymentConfirmationCallback)?.let {
                it(Result.success(PaymentResult.Completed))
            }
        }
        whenever(linkAccount.clientSecret).thenReturn("secret")
        whenever(linkRepository.listPaymentDetails(anyOrNull()))
            .thenReturn(Result.success(PaymentDetailsFixtures.CONSUMER_PAYMENT_DETAILS))

        val paymentDetails = PaymentDetailsFixtures.CONSUMER_PAYMENT_DETAILS.paymentDetails.first()
        createViewModel().completePayment(paymentDetails)

        verify(navigator).dismiss(LinkActivityResult.Success)
    }

    @Test
    fun `Pay another way dismisses Link`() {
        val viewModel = createViewModel()

        viewModel.payAnotherWay()

        verify(navigator).dismiss()
    }

    @Test
    fun `Add new payment method navigates to AddPaymentMethod screen`() {
        val viewModel = createViewModel()

        viewModel.addNewPaymentMethod()

        verify(navigator).navigateTo(LinkScreen.PaymentMethod, false)
    }

    @Test
    fun `Factory gets initialized by Injector`() {
        val mockBuilder = mock<SignedInViewModelSubcomponent.Builder>()
        val mockSubComponent = mock<SignedInViewModelSubcomponent>()
        val vmToBeReturned = mock<WalletViewModel>()

        whenever(mockBuilder.linkAccount(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.build()).thenReturn(mockSubComponent)
        whenever((mockSubComponent.walletViewModel)).thenReturn(vmToBeReturned)

        val mockSavedStateRegistryOwner = mock<SavedStateRegistryOwner>()
        val mockSavedStateRegistry = mock<SavedStateRegistry>()
        val mockLifeCycle = mock<Lifecycle>()

        whenever(mockSavedStateRegistryOwner.savedStateRegistry).thenReturn(mockSavedStateRegistry)
        whenever(mockSavedStateRegistryOwner.lifecycle).thenReturn(mockLifeCycle)
        whenever(mockLifeCycle.currentState).thenReturn(Lifecycle.State.CREATED)

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as WalletViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }
        WeakMapInjectorRegistry.register(injector, DUMMY_INJECTOR_KEY)
        val factory = WalletViewModel.Factory(
            mock(),
            injector
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(WalletViewModel::class.java)
        assertThat(createdViewModel).isEqualTo(vmToBeReturned)

        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    private fun createViewModel() =
        WalletViewModel(
            args,
            linkAccount,
            linkRepository,
            linkAccountManager,
            navigator,
            confirmationManager,
            logger
        )

    private fun getContext() = ApplicationProvider.getApplicationContext<Context>()
}
