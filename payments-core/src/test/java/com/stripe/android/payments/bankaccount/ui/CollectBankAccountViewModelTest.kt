package com.stripe.android.payments.bankaccount.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.model.BankConnectionsLinkedAccountSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.domain.AttachLinkAccountSession
import com.stripe.android.payments.bankaccount.domain.CreateLinkAccountSession
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.Args.ForPaymentIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.Args.ForSetupIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponse
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult.Completed
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult.Failed
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithResult
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CollectBankAccountViewModelTest {

    private val createLinkAccountSession: CreateLinkAccountSession = mock()
    private val attachLinkAccountSession: AttachLinkAccountSession = mock()

    private val publishableKey = "publishable_key"
    private val clientSecret = "client_secret"
    private val name = "name"
    private val email = "email"
    private val linkedAccountSessionId = "las_id"
    private val linkedAccountSessionClientSecret = "las_client_secret"
    private val linkedAccountSession = BankConnectionsLinkedAccountSession(
        clientSecret = linkedAccountSessionClientSecret,
        id = linkedAccountSessionId
    )

    private val linkAccountSession = mock<LinkAccountSession> {
        on { this.clientSecret } doReturn "client_secret"
        on { this.id } doReturn linkedAccountSessionId
    }

    @Test
    fun `init - when createLinkAccountSession succeeds for PI, opens connection flow`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForPaymentIntentReturns(Result.success(linkedAccountSession))

            // When
            buildViewModel(viewEffect, paymentIntentConfiguration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                OpenConnectionsFlow(
                    publishableKey,
                    linkedAccountSession.clientSecret!!
                )
            )
        }
    }

    @Test
    fun `init - when createLinkAccountSession succeeds for SI, opens connection flow`() = runTest {
        val viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionForSetupIntentReturns(Result.success(linkedAccountSession))

            // When
            buildViewModel(viewEffect, setupIntentConfiguration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                OpenConnectionsFlow(
                    publishableKey,
                    linkedAccountSession.clientSecret!!
                )
            )
        }
    }

    @Test
    fun `init - when createLinkAccountSession fails, finish with error`() = runTest {
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
                    Failed(expectedException)
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
            givenCreateAccountSessionForPaymentIntentReturns(Result.success(linkedAccountSession))
            givenAttachAccountSessionForPaymentIntentReturns(Result.success(paymentIntent))

            // When
            val viewModel = buildViewModel(viewEffect, paymentIntentConfiguration())

            viewModel.onConnectionsResult(
                FinancialConnectionsSheetResult.Completed(linkAccountSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                FinishWithResult(
                    Completed(CollectBankAccountResponse(paymentIntent))
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
            givenCreateAccountSessionForSetupIntentReturns(Result.success(linkedAccountSession))
            givenAttachAccountSessionForSetupIntentReturns(Result.success(setupIntent))

            // When
            val viewModel = buildViewModel(viewEffect, setupIntentConfiguration())
            viewModel.onConnectionsResult(
                FinancialConnectionsSheetResult.Completed(linkAccountSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                FinishWithResult(
                    Completed(CollectBankAccountResponse(setupIntent))
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
            givenCreateAccountSessionForSetupIntentReturns(Result.success(linkedAccountSession))
            givenAttachAccountSessionForSetupIntentReturns(Result.failure(expectedException))

            // When
            val viewModel = buildViewModel(viewEffect, setupIntentConfiguration())
            viewModel.onConnectionsResult(
                FinancialConnectionsSheetResult.Completed(linkAccountSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                FinishWithResult(
                    Failed(expectedException)
                )
            )
        }
    }

    private fun givenCreateAccountSessionForPaymentIntentReturns(
        result: Result<BankConnectionsLinkedAccountSession>
    ) {
        createLinkAccountSession.stub {
            onBlocking {
                forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = name,
                    customerEmail = email
                )
            }.doReturn(result)
        }
    }

    private fun givenAttachAccountSessionForPaymentIntentReturns(
        result: Result<PaymentIntent>
    ) {
        attachLinkAccountSession.stub {
            onBlocking {
                forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    linkedAccountSessionId = linkedAccountSessionId,
                )
            }.doReturn(result)
        }
    }

    private fun givenAttachAccountSessionForSetupIntentReturns(
        result: Result<SetupIntent>
    ) {
        attachLinkAccountSession.stub {
            onBlocking {
                forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    linkedAccountSessionId = linkedAccountSessionId,
                )
            }.doReturn(result)
        }
    }

    private fun givenCreateAccountSessionForSetupIntentReturns(
        result: Result<BankConnectionsLinkedAccountSession>
    ) {
        createLinkAccountSession.stub {
            onBlocking {
                forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = name,
                    customerEmail = email
                )
            }.doReturn(result)
        }
    }

    private fun buildViewModel(
        viewEffect: MutableSharedFlow<CollectBankAccountViewEffect>,
        configuration: CollectBankAccountContract.Args
    ) = CollectBankAccountViewModel(
        args = configuration,
        createLinkAccountSession = createLinkAccountSession,
        attachLinkAccountSession = attachLinkAccountSession,
        logger = Logger.noop(),
        _viewEffect = viewEffect
    )

    private fun paymentIntentConfiguration(): ForPaymentIntent {
        return ForPaymentIntent(
            publishableKey = publishableKey,
            clientSecret = clientSecret,
            configuration = CollectBankAccountConfiguration.USBankAccount(
                name,
                email
            )
        )
    }

    private fun setupIntentConfiguration(): ForSetupIntent {
        return ForSetupIntent(
            publishableKey = publishableKey,
            clientSecret = clientSecret,
            configuration = CollectBankAccountConfiguration.USBankAccount(
                name,
                email
            )
        )
    }
}
