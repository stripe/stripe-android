package com.stripe.android.payments.bankaccount.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.CreateFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsContract
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import com.stripe.android.financialconnections.model.FinancialConnectionsSession as PaymentsFinancialConnectionsSession

@RunWith(RobolectricTestRunner::class)
class CollectSessionForDeferredPaymentsViewModelTest {

    private val createFinancialConnectionsSession: CreateFinancialConnectionsSession = mock()

    private val publishableKey = "publishable_key"
    private val stripeAccountId = "stripe_account_id"
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
    fun `init - when createFinancialConnectionsSession succeeds, opens connection flow`() = runTest {
        val viewEffect = MutableSharedFlow<CollectSessionForDeferredPaymentsViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionReturns(Result.success(financialConnectionsSession))

            // When
            buildViewModel(viewEffect, configuration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                CollectSessionForDeferredPaymentsViewEffect.OpenConnectionsFlow(
                    publishableKey = publishableKey,
                    financialConnectionsSessionSecret = financialConnectionsSession.clientSecret!!,
                    stripeAccountId = stripeAccountId
                )
            )
        }
    }

    @Test
    fun `init - when createFinancialConnectionsSession fails, finish with error`() = runTest {
        val viewEffect = MutableSharedFlow<CollectSessionForDeferredPaymentsViewEffect>()
        viewEffect.test {
            // Given
            val expectedException = Exception("Random error")
            givenCreateAccountSessionReturns(Result.failure(expectedException))

            // When
            buildViewModel(viewEffect, configuration())

            // Then
            assertThat(awaitItem()).isEqualTo(
                CollectSessionForDeferredPaymentsViewEffect.FinishWithResult(
                    CollectSessionForDeferredPaymentsResult.Failed(expectedException)
                )
            )
        }
    }

    @Test
    fun `connectionsResult - when createFinancialConnectionsSession succeeds, finish with success`() = runTest {
        val viewEffect = MutableSharedFlow<CollectSessionForDeferredPaymentsViewEffect>()
        viewEffect.test {
            // Given
            givenCreateAccountSessionReturns(Result.success(financialConnectionsSession))

            // When
            val viewModel = buildViewModel(viewEffect, configuration())
            viewModel.onConnectionsResult(
                FinancialConnectionsSheetResult.Completed(paymentsFinancialConnectionsSession)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                CollectSessionForDeferredPaymentsViewEffect.FinishWithResult(
                    CollectSessionForDeferredPaymentsResult.Completed(paymentsFinancialConnectionsSession)
                )
            )
        }
    }

    @Test
    fun `connectionsResult - when createFinancialConnectionsSession fails, finish with error`() = runTest {
        val viewEffect = MutableSharedFlow<CollectSessionForDeferredPaymentsViewEffect>()
        viewEffect.test {
            // Given
            val expectedException = Exception("Random error")
            givenCreateAccountSessionReturns(Result.failure(expectedException))

            // When
            val viewModel = buildViewModel(viewEffect, configuration())
            viewModel.onConnectionsResult(
                FinancialConnectionsSheetResult.Failed(expectedException)
            )

            // Then
            assertThat(expectMostRecentItem()).isEqualTo(
                CollectSessionForDeferredPaymentsViewEffect.FinishWithResult(
                    CollectSessionForDeferredPaymentsResult.Failed(expectedException)
                )
            )
        }
    }

    private fun givenCreateAccountSessionReturns(
        result: Result<FinancialConnectionsSession>
    ) {
        createFinancialConnectionsSession.stub {
            onBlocking {
                forDeferredPayments(
                    publishableKey = publishableKey,
                    stripeAccountId = stripeAccountId
                )
            }.doReturn(result)
        }
    }

    private fun buildViewModel(
        viewEffect: MutableSharedFlow<CollectSessionForDeferredPaymentsViewEffect>,
        args: CollectSessionForDeferredPaymentsContract.Args
    ) = CollectSessionForDeferredPaymentsViewModel(
        args = args,
        createFinancialConnectionsSession = createFinancialConnectionsSession,
        logger = Logger.noop(),
        savedStateHandle = SavedStateHandle(),
        _viewEffect = viewEffect
    )

    private fun configuration(): CollectSessionForDeferredPaymentsContract.Args {
        return CollectSessionForDeferredPaymentsContract.Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
        )
    }
}
