package com.stripe.android.link

import android.app.Activity
import android.app.Application
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.utils.FakePaymentElementLoader
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
    private val logger = FakeLogger()
    private val paymentElementLoader: PaymentElementLoader = FakePaymentElementLoader()
    private val linkGateFactory: LinkGate.Factory = LinkGate.Factory { FakeLinkGate() }
    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
    private val linkApiRepository: LinkRepository = FakeLinkRepository()
    private val controllerComponentFactory: LinkControllerComponent.Factory =
        object : LinkControllerComponent.Factory {
            override fun build(
                activity: Activity,
                lifecycleOwner: LifecycleOwner,
                activityResultRegistryOwner: ActivityResultRegistryOwner,
                presentPaymentMethodCallback: LinkController.PresentPaymentMethodsCallback,
                lookupConsumerCallback: LinkController.LookupConsumerCallback,
                createPaymentMethodCallback: LinkController.CreatePaymentMethodCallback
            ): LinkControllerComponent {
                return mock()
            }
        }

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
