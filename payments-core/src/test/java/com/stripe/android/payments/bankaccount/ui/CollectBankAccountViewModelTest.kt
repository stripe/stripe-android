package com.stripe.android.payments.bankaccount.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.domain.AttachFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.CreateFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.RetrieveStripeIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.Args.ForPaymentIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.Args.ForSetupIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal.USBankAccountData
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithResult
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import com.stripe.android.financialconnections.model.FinancialConnectionsSession as PaymentsFinancialConnectionsSession

@RunWith(RobolectricTestRunner::class)
class CollectBankAccountViewModelTest {

    private val createFinancialConnectionsSession: CreateFinancialConnectionsSession = mock()
    private val attachFinancialConnectionsSession: AttachFinancialConnectionsSession = mock()
    private val retrieveStripeIntent: RetrieveStripeIntent = mock()

    private val publishableKey = "publishable_key"
    private val stripeAccountId = "stripe_account_id"
    private val clientSecret = "client_secret"
    private val name = "name"
    private val email = "email"
    private val linkedAccountSessionId = "las_id"
    private val linkedAccountSessionClientSecret = "las_client_secret"
    private val financialConnectionsSession = FinancialConnectionsSession(
        clientSecret = linkedAccountSessionClientSecret,
        id = linkedAccountSessionId
    )

    private val paymentsFinancialConnectionsSession = mock<PaymentsFinancialConnectionsSession> {
        on { this.clientSecret } doReturn "client_secret"
        on { this.id } doReturn linkedAccountSessionId
    }

    @Test
    fun `init - when createFinancialConnectionsSession succeeds for PI, opens connection flow`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForPaymentIntentReturns(Result.success(financialConnectionsSession))

            // When
            buildViewModel(viewEffect, paymentIntentConfiguration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                OpenConnectionsFlow(
                    publishableKey = publishableKey,
                    financialConnectionsSessionSecret = financialConnectionsSession.clientSecret!!,
                    stripeAccountId = stripeAccountId,
                    elementsSessionContext = null,
                )
            )
        }
    }

    @Test
    fun `init - when createFinancialConnectionsSession succeeds for SI, opens connection flow`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForSetupIntentReturns(Result.success(financialConnectionsSession))

            // When
            buildViewModel(viewEffect, setupIntentConfiguration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                OpenConnectionsFlow(
                    publishableKey = publishableKey,
                    financialConnectionsSessionSecret = financialConnectionsSession.clientSecret!!,
                    stripeAccountId = stripeAccountId,
                    elementsSessionContext = null,
                )
            )
        }
    }

    @Test
    fun `init - when create session succeeds for deferred payment, opens connection flow`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForDeferredPaymentsReturns(
                Result.success(financialConnectionsSession)
            )

            // When
            buildViewModel(viewEffect, deferredPaymentIntentConfiguration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                OpenConnectionsFlow(
                    publishableKey = publishableKey,
                    financialConnectionsSessionSecret = financialConnectionsSession.clientSecret!!,
                    stripeAccountId = stripeAccountId,
                    elementsSessionContext = null,
                )
            )
        }
    }

    @Test
    fun `init - when createFinancialConnectionsSession succeeds for deferred setup, opens connection flow`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForDeferredSetupReturns(
                Result.success(financialConnectionsSession)
            )

            // When
            buildViewModel(viewEffect, deferredSetupIntentConfiguration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                OpenConnectionsFlow(
                    publishableKey = publishableKey,
                    financialConnectionsSessionSecret = financialConnectionsSession.clientSecret!!,
                    stripeAccountId = stripeAccountId,
                    elementsSessionContext = null,
                )
            )
        }
    }

    @Test
    fun `init - when attachToIntent is false, attach is not called for payment intent`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForPaymentIntentReturns(Result.success(financialConnectionsSession))
            givenRetrieveStripeIntentReturns(Result.success(mock()))

            // When
            val viewModel = buildViewModel(viewEffect, paymentIntentConfiguration(attachToIntent = false))
            viewModel.onConnectionsForACHResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            cancelAndConsumeRemainingEvents()
            verify(attachFinancialConnectionsSession, never()).forPaymentIntent(
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `init - when attachToIntent is false, attach is not called for setup intent`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForSetupIntentReturns(Result.success(financialConnectionsSession))
            givenRetrieveStripeIntentReturns(Result.success(mock()))

            // When
            val viewModel = buildViewModel(viewEffect, setupIntentConfiguration(attachToIntent = false))
            viewModel.onConnectionsForACHResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            cancelAndConsumeRemainingEvents()
            verify(attachFinancialConnectionsSession, never()).forSetupIntent(
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `init - when createFinancialConnectionsSession fails, finish with error`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            val expectedException = Exception("Random error")
            givenCreateAccountSessionForPaymentIntentReturns(Result.failure(expectedException))

            // When
            buildViewModel(viewEffect, paymentIntentConfiguration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                FinishWithResult(
                    CollectBankAccountResultInternal.Failed(expectedException)
                )
            )
        }
    }

    @Test
    fun `connectionsResult - when attach succeeds for PI, finish with success`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            val paymentIntent = mock<PaymentIntent>()
            givenCreateAccountSessionForPaymentIntentReturns(Result.success(financialConnectionsSession))
            givenAttachAccountSessionForPaymentIntentReturns(Result.success(paymentIntent))

            // When
            val viewModel = buildViewModel(viewEffect, paymentIntentConfiguration())

            viewModel.onConnectionsForACHResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                FinishWithResult(
                    CollectBankAccountResultInternal.Completed(
                        CollectBankAccountResponseInternal(
                            paymentIntent,
                            usBankAccountData = USBankAccountData(paymentsFinancialConnectionsSession),
                            instantDebitsData = null
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `connectionsResult - when attach succeeds for SI, finish with success`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            val setupIntent = mock<SetupIntent>()
            givenCreateAccountSessionForSetupIntentReturns(Result.success(financialConnectionsSession))
            givenAttachAccountSessionForSetupIntentReturns(Result.success(setupIntent))

            // When
            val viewModel = buildViewModel(viewEffect, setupIntentConfiguration())
            viewModel.onConnectionsForACHResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                FinishWithResult(
                    CollectBankAccountResultInternal.Completed(
                        CollectBankAccountResponseInternal(
                            setupIntent,
                            usBankAccountData = USBankAccountData(paymentsFinancialConnectionsSession),
                            instantDebitsData = null
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `connectionsResult - when create succeeds for deferred payments, finish with success`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForDeferredPaymentsReturns(
                Result.success(financialConnectionsSession)
            )

            // When
            val viewModel = buildViewModel(viewEffect, deferredPaymentIntentConfiguration())
            viewModel.onConnectionsForACHResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                FinishWithResult(
                    CollectBankAccountResultInternal.Completed(
                        CollectBankAccountResponseInternal(
                            null,
                            usBankAccountData = USBankAccountData(paymentsFinancialConnectionsSession),
                            instantDebitsData = null
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `connectionsResult - when create session succeeds for deferred setup, finish with success`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForDeferredSetupReturns(
                Result.success(financialConnectionsSession)
            )

            // When
            val viewModel = buildViewModel(viewEffect, deferredSetupIntentConfiguration())
            viewModel.onConnectionsForACHResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                FinishWithResult(
                    CollectBankAccountResultInternal.Completed(
                        CollectBankAccountResponseInternal(
                            null,
                            usBankAccountData = USBankAccountData(paymentsFinancialConnectionsSession),
                            instantDebitsData = null
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `connectionsResult - when attach fails, finish with error`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            val expectedException = Exception("Random error")
            givenCreateAccountSessionForSetupIntentReturns(Result.success(financialConnectionsSession))
            givenAttachAccountSessionForSetupIntentReturns(Result.failure(expectedException))

            // When
            val viewModel = buildViewModel(viewEffect, setupIntentConfiguration())
            viewModel.onConnectionsForACHResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            val result = expectMostRecentItem()
            assertThat(result).isEqualTo(
                FinishWithResult(
                    CollectBankAccountResultInternal.Failed(expectedException)
                )
            )
        }
    }

    private fun givenCreateAccountSessionForPaymentIntentReturns(
        result: Result<FinancialConnectionsSession>
    ) {
        createFinancialConnectionsSession.stub {
            onBlocking {
                forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    stripeAccountId = stripeAccountId,
                    configuration = CollectBankAccountConfiguration.USBankAccount(
                        name = name,
                        email = email
                    ),
                    hostedSurface = "payment_element"
                )
            }.doReturn(result)
        }
    }

    private fun givenAttachAccountSessionForPaymentIntentReturns(
        result: Result<PaymentIntent>
    ) {
        attachFinancialConnectionsSession.stub {
            onBlocking {
                forPaymentIntent(
                    publishableKey = publishableKey,
                    linkedAccountSessionId = linkedAccountSessionId,
                    clientSecret = clientSecret,
                    stripeAccountId = stripeAccountId
                )
            }.doReturn(result)
        }
    }

    private fun givenAttachAccountSessionForSetupIntentReturns(
        result: Result<SetupIntent>
    ) {
        attachFinancialConnectionsSession.stub {
            onBlocking {
                forSetupIntent(
                    publishableKey = publishableKey,
                    linkedAccountSessionId = linkedAccountSessionId,
                    clientSecret = clientSecret,
                    stripeAccountId = stripeAccountId
                )
            }.doReturn(result)
        }
    }

    private fun givenCreateAccountSessionForSetupIntentReturns(
        result: Result<FinancialConnectionsSession>
    ) {
        createFinancialConnectionsSession.stub {
            onBlocking {
                forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    stripeAccountId = stripeAccountId,
                    configuration = CollectBankAccountConfiguration.USBankAccount(
                        name = name,
                        email = email
                    ),
                    hostedSurface = "payment_element"
                )
            }.doReturn(result)
        }
    }

    private fun givenCreateAccountSessionForDeferredPaymentsReturns(
        result: Result<FinancialConnectionsSession>
    ) {
        createFinancialConnectionsSession.stub {
            onBlocking {
                forDeferredIntent(
                    publishableKey = publishableKey,
                    stripeAccountId = stripeAccountId,
                    elementsSessionId = "elements_session_id",
                    customerId = "customer_id",
                    onBehalfOf = "on_behalf_of_id",
                    linkMode = null,
                    amount = 1000,
                    currency = "usd",
                    hostedSurface = "payment_element",
                    product = null,
                )
            }.doReturn(result)
        }
    }

    private fun givenCreateAccountSessionForDeferredSetupReturns(
        result: Result<FinancialConnectionsSession>
    ) {
        createFinancialConnectionsSession.stub {
            onBlocking {
                forDeferredIntent(
                    publishableKey = publishableKey,
                    stripeAccountId = stripeAccountId,
                    elementsSessionId = "elements_session_id",
                    customerId = "customer_id",
                    onBehalfOf = "on_behalf_of_id",
                    linkMode = null,
                    amount = null,
                    currency = null,
                    hostedSurface = "payment_element",
                    product = null,
                )
            }.doReturn(result)
        }
    }

    private fun givenRetrieveStripeIntentReturns(
        result: Result<StripeIntent>
    ) {
        retrieveStripeIntent.stub {
            onBlocking {
                this(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret
                )
            }.doReturn(result)
        }
    }

    private fun buildViewModel(
        viewEffect: MutableSharedFlow<CollectBankAccountViewEffect>,
        configuration: CollectBankAccountContract.Args
    ) = CollectBankAccountViewModel(
        args = configuration,
        createFinancialConnectionsSession = createFinancialConnectionsSession,
        attachFinancialConnectionsSession = attachFinancialConnectionsSession,
        retrieveStripeIntent = retrieveStripeIntent,
        logger = Logger.noop(),
        savedStateHandle = SavedStateHandle(),
        _viewEffect = viewEffect
    )

    private fun paymentIntentConfiguration(
        attachToIntent: Boolean = true
    ): ForPaymentIntent {
        return ForPaymentIntent(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            clientSecret = clientSecret,
            configuration = CollectBankAccountConfiguration.USBankAccount(
                name,
                email
            ),
            attachToIntent = attachToIntent,
            hostedSurface = "payment_element"
        )
    }

    private fun setupIntentConfiguration(
        attachToIntent: Boolean = true
    ): ForSetupIntent {
        return ForSetupIntent(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            clientSecret = clientSecret,
            configuration = CollectBankAccountConfiguration.USBankAccount(
                name,
                email
            ),
            attachToIntent = attachToIntent,
            hostedSurface = "payment_element"
        )
    }

    private fun deferredPaymentIntentConfiguration(): CollectBankAccountContract.Args.ForDeferredPaymentIntent {
        return CollectBankAccountContract.Args.ForDeferredPaymentIntent(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            configuration = CollectBankAccountConfiguration.USBankAccount(
                name,
                email
            ),
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
            amount = 1000,
            currency = "usd",
            hostedSurface = "payment_element"
        )
    }

    private fun deferredSetupIntentConfiguration(): CollectBankAccountContract.Args.ForDeferredSetupIntent {
        return CollectBankAccountContract.Args.ForDeferredSetupIntent(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            configuration = CollectBankAccountConfiguration.USBankAccount(
                name,
                email
            ),
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
            hostedSurface = "payment_element"
        )
    }
}
