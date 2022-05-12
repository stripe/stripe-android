package com.stripe.android.link.ui.paymentmethod

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
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
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentMethodViewModelTest {
    private val clientSecret = "client_secret"
    private val linkAccount = mock<LinkAccount>().also {
        whenever(it.email).thenReturn("email@stripe.com")
    }
    private val args = mock<LinkActivityContract.Args>()
    private lateinit var linkRepository: LinkRepository
    private val linkAccountManager = mock<LinkAccountManager>()
    private val navigator = mock<Navigator>()
    private val confirmationManager = mock<ConfirmationManager>()
    private val logger = Logger.noop()
    private val cardFormFieldValues = mapOf(
        IdentifierSpec.CardNumber to FormFieldEntry("5555555555554444", true),
        IdentifierSpec.CardCvc to FormFieldEntry("123", true),
        IdentifierSpec.Generic("card[exp_month]") to FormFieldEntry("12", true),
        IdentifierSpec.Generic("card[exp_year]") to FormFieldEntry("2050", true),
        IdentifierSpec.Country to FormFieldEntry("US", true),
        IdentifierSpec.PostalCode to FormFieldEntry("12345", true),
    )

    @Before
    fun before() {
        linkRepository = mock()
        whenever(args.stripeIntent).thenReturn(StripeIntentFixtures.PI_SUCCEEDED)
        whenever(args.completePayment).thenReturn(true)
        whenever(linkAccount.clientSecret).thenReturn(clientSecret)
    }

    @Test
    fun `startPayment creates PaymentDetails`() = runTest {
        whenever(
            linkRepository.createPaymentDetails(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        ).thenReturn(Result.success(createLinkPaymentDetails()))

        createViewModel().startPayment(cardFormFieldValues)

        val paramsCaptor = argumentCaptor<ConsumerPaymentDetailsCreateParams>()
        verify(linkRepository).createPaymentDetails(
            paramsCaptor.capture(),
            any(),
            any(),
            anyOrNull()
        )

        assertThat(paramsCaptor.firstValue.toParamMap()).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_email_address" to "email@stripe.com",
                "card" to mapOf(
                    "number" to "5555555555554444",
                    "exp_month" to "12",
                    "exp_year" to "2050"
                ),
                "billing_address" to mapOf(
                    "country_code" to "US",
                    "postal_code" to "12345"
                )
            )
        )
    }

    @Test
    fun `startPayment completes payment when PaymentDetails creation succeeds and completePayment is true`() =
        runTest {
            val value = createLinkPaymentDetails()
            whenever(
                linkRepository.createPaymentDetails(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            )
                .thenReturn(Result.success(value))

            createViewModel().startPayment(cardFormFieldValues)

            val paramsCaptor = argumentCaptor<ConfirmStripeIntentParams>()
            verify(confirmationManager).confirmStripeIntent(paramsCaptor.capture(), any())

            assertThat(paramsCaptor.firstValue.toParamMap()).isEqualTo(
                mapOf(
                    "client_secret" to args.stripeIntent.clientSecret,
                    "use_stripe_sdk" to false,
                    "mandate_data" to mapOf(
                        "customer_acceptance" to mapOf(
                            "type" to "online",
                            "online" to mapOf(
                                "infer_from_client" to true
                            )
                        )
                    ),
                    "payment_method_data" to mapOf(
                        "type" to "link",
                        "link" to mapOf(
                            "payment_details_id" to "QAAAKJ6",
                            "credentials" to mapOf(
                                "consumer_session_client_secret" to clientSecret
                            ),
                            "card" to mapOf(
                                "cvc" to "123"
                            )
                        )
                    )
                )
            )
        }

    @Test
    fun `startPayment returns PaymentMethodCreateParams when PaymentDetails creation succeeds and completePayment is false`() =
        runTest {
            whenever(args.completePayment).thenReturn(false)

            val linkPaymentDetails = createLinkPaymentDetails()
            whenever(
                linkRepository.createPaymentDetails(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            )
                .thenReturn(Result.success(linkPaymentDetails))

            createViewModel().startPayment(cardFormFieldValues)

            val paramsCaptor = argumentCaptor<LinkActivityResult>()
            verify(navigator).dismiss(paramsCaptor.capture())

            assertThat(paramsCaptor.firstValue).isEqualTo(
                LinkActivityResult.Success.Selected(linkPaymentDetails)
            )
        }

    @Test
    fun `startPayment dismisses Link on success`() = runTest {
        whenever(
            linkRepository.createPaymentDetails(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        )
            .thenReturn(Result.success(createLinkPaymentDetails()))

        var callback: PaymentConfirmationCallback? = null
        whenever(
            confirmationManager.confirmStripeIntent(
                anyOrNull(),
                argWhere {
                    callback = it
                    true
                }
            )
        ).then {
            callback!!(Result.success(PaymentResult.Completed))
        }

        createViewModel().startPayment(cardFormFieldValues)

        verify(navigator).dismiss(LinkActivityResult.Success.Completed)
    }

    @Test
    fun `startPayment starts processing`() = runTest {
        whenever(
            linkRepository.createPaymentDetails(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        )
            .thenReturn(Result.success(createLinkPaymentDetails()))

        val viewModel = createViewModel()

        var isProcessing: Boolean? = null
        viewModel.isProcessing.asLiveData().observeForever {
            isProcessing = it
        }

        viewModel.startPayment(cardFormFieldValues)

        assertThat(isProcessing).isTrue()
    }

    @Test
    fun `startPayment stops processing on error`() = runTest {
        whenever(
            linkRepository.createPaymentDetails(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        )
            .thenReturn(Result.success(createLinkPaymentDetails()))

        var callback: PaymentConfirmationCallback? = null
        whenever(
            confirmationManager.confirmStripeIntent(
                anyOrNull(),
                argWhere {
                    callback = it
                    true
                }
            )
        ).then {
            callback!!(Result.success(PaymentResult.Failed(Error())))
        }

        val viewModel = createViewModel()

        var isProcessing: Boolean? = null
        viewModel.isProcessing.asLiveData().observeForever {
            isProcessing = it
        }

        viewModel.startPayment(cardFormFieldValues)

        assertThat(isProcessing).isFalse()
    }

    @Test
    fun `payAnotherWay dismisses and logs out`() = runTest {
        createViewModel().payAnotherWay()

        verify(navigator).dismiss()
        verify(linkAccountManager).logout()
    }

    @Test
    fun `Factory gets initialized by Injector`() {
        val mockBuilder = mock<SignedInViewModelSubcomponent.Builder>()
        val mockSubComponent = mock<SignedInViewModelSubcomponent>()
        val vmToBeReturned = mock<PaymentMethodViewModel>()

        whenever(mockBuilder.linkAccount(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.build()).thenReturn(mockSubComponent)
        whenever((mockSubComponent.paymentMethodViewModel)).thenReturn(vmToBeReturned)

        val mockSavedStateRegistryOwner = mock<SavedStateRegistryOwner>()
        val mockSavedStateRegistry = mock<SavedStateRegistry>()
        val mockLifeCycle = mock<Lifecycle>()

        whenever(mockSavedStateRegistryOwner.savedStateRegistry).thenReturn(mockSavedStateRegistry)
        whenever(mockSavedStateRegistryOwner.lifecycle).thenReturn(mockLifeCycle)
        whenever(mockLifeCycle.currentState).thenReturn(Lifecycle.State.CREATED)

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as PaymentMethodViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }

        val factory = PaymentMethodViewModel.Factory(
            mock(),
            injector
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(PaymentMethodViewModel::class.java)
        assertThat(createdViewModel).isEqualTo(vmToBeReturned)
    }

    private fun createViewModel() =
        PaymentMethodViewModel(
            args,
            linkAccount,
            linkRepository,
            linkAccountManager,
            navigator,
            confirmationManager,
            logger
        )

    private fun createLinkPaymentDetails() =
        PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first().let {
            LinkPaymentDetails(
                it,
                PaymentMethodCreateParams.createLink(
                    it.id,
                    clientSecret,
                    mapOf("card" to mapOf("cvc" to "123"))
                )
            )
        }

    private fun getContext() = ApplicationProvider.getApplicationContext<Context>()
}
