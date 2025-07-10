package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkControllerViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val logger: Logger = mock()
    private val paymentElementLoader: PaymentElementLoader = mock()
    private val linkGateFactory: LinkGate.Factory = mock()
    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
    private val linkApiRepository: LinkApiRepository = mock()
    private val controllerComponentFactory: LinkControllerComponent.Factory = mock()

    @Test
    fun `Initial state is correct`() = runTest {
        val viewModel = createViewModel()

        viewModel.state(application).test {
            assertThat(awaitItem()).isEqualTo(
                LinkController.State(
                    isConsumerVerified = null,
                    selectedPaymentMethodPreview = null,
                    createdPaymentMethod = null
                )
            )
        }
    }

    private fun createViewModel(): LinkControllerViewModel {
        return LinkControllerViewModel(
            application = application,
            logger = logger,
            paymentElementLoader = paymentElementLoader,
            linkGateFactory = linkGateFactory,
            linkAccountHolder = linkAccountHolder,
            linkApiRepository = linkApiRepository,
            controllerComponentFactory = controllerComponentFactory
        )
    }
}
