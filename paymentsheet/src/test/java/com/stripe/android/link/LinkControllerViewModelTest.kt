package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.exceptions.MissingConfigurationException
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
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
    private val linkConfigurationLoader = FakeLinkConfigurationLoader()
    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
    private val linkRepository = FakeLinkRepository()
    private val controllerComponentFactory: LinkControllerComponent.Factory = mock()

    @Test
    fun `Initial state is correct`() = runTest {
        val viewModel = createViewModel()

        viewModel.state(application).test {
            assertThat(awaitItem()).isEqualTo(
                LinkController.State()
            )
        }
    }

    @Test
    fun `state is updated when account changes`() = runTest {
        val viewModel = createViewModel()

        viewModel.state(application).test {
            assertThat(awaitItem().isConsumerVerified).isNull()

            linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

            assertThat(awaitItem().isConsumerVerified).isTrue()

            val unverifiedSession = TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION)
            )
            linkAccountHolder.set(LinkAccountUpdate.Value(LinkAccount(unverifiedSession)))

            assertThat(awaitItem().isConsumerVerified).isFalse()

            linkAccountHolder.set(LinkAccountUpdate.Value(null))

            assertThat(awaitItem().isConsumerVerified).isNull()
        }
    }

    @Test
    fun `configure() sets new configuration and loads it`() = runTest {
        val linkConfigurationLoader = FakeLinkConfigurationLoader()
        val viewModel = createViewModel(linkConfigurationLoader = linkConfigurationLoader)

        val loadedConfiguration = mock<LinkConfiguration>()
        linkConfigurationLoader.linkConfigurationResult = Result.success(loadedConfiguration)

        assertThat(viewModel.configure(mock())).isEqualTo(LinkController.ConfigureResult.Success)
    }

    @Test
    fun `configure() fails when loader fails`() = runTest {
        val error = Exception("Failed to load")
        val linkConfigurationLoader = FakeLinkConfigurationLoader()
        val viewModel = createViewModel(linkConfigurationLoader = linkConfigurationLoader)

        linkConfigurationLoader.linkConfigurationResult = Result.failure(error)

        assertThat(viewModel.configure(mock())).isEqualTo(LinkController.ConfigureResult.Failed(error))
    }

    @Test
    fun `configure() resets state`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateState {
            it.copy(createdPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

        val loadedConfiguration = mock<LinkConfiguration>()
        linkConfigurationLoader.linkConfigurationResult = Result.success(loadedConfiguration)

        viewModel.state(application).test {
            assertThat(awaitItem()).isNotEqualTo(LinkController.State())
            assertThat(viewModel.configure(mock())).isEqualTo(LinkController.ConfigureResult.Success)
            assertThat(awaitItem()).isEqualTo(LinkController.State())
        }
    }

    @Test
    fun `onPresentPaymentMethods() fails when configuration is not set`() = runTest {
        val viewModel = createViewModel()

        viewModel.presentPaymentMethodsResultFlow.test {
            viewModel.onPresentPaymentMethods(mock(), "test@example.com")

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.PresentPaymentMethodsResult.Failed::class.java)
            val error = (result as LinkController.PresentPaymentMethodsResult.Failed).error
            assertThat(error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when configuration is not set`() = runTest {
        val viewModel = createViewModel()

        viewModel.createPaymentMethodResultFlow.test {
            viewModel.onCreatePaymentMethod()

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
            val error = (result as LinkController.CreatePaymentMethodResult.Failed).error
            assertThat(error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onLookupConsumer() emits success result when repository returns success`() = runTest {
        val viewModel = createViewModel()

        val consumerSessionLookup = ConsumerSessionLookup(exists = true, consumerSession = null)
        linkRepository.lookupConsumerResult = Result.success(consumerSessionLookup)

        viewModel.lookupConsumerResultFlow.test {
            viewModel.onLookupConsumer("test@example.com")
            val result = awaitItem()
            assertThat(result).isEqualTo(
                LinkController.LookupConsumerResult.Success("test@example.com", true)
            )
        }
    }

    @Test
    fun `onLookupConsumer() emits failure result when repository returns failure`() = runTest {
        val viewModel = createViewModel()

        val error = Exception("Error")
        linkRepository.lookupConsumerResult = Result.failure(error)

        viewModel.lookupConsumerResultFlow.test {
            viewModel.onLookupConsumer("test@example.com")
            val result = awaitItem()
            assertThat(result).isEqualTo(
                LinkController.LookupConsumerResult.Failed("test@example.com", error)
            )
        }
    }

    private fun createViewModel(
        linkConfigurationLoader: LinkConfigurationLoader = this.linkConfigurationLoader
    ): LinkControllerViewModel {
        return LinkControllerViewModel(
            application = application,
            logger = logger,
            linkConfigurationLoader = linkConfigurationLoader,
            linkAccountHolder = linkAccountHolder,
            linkApiRepository = linkRepository,
            controllerComponentFactory = controllerComponentFactory
        )
    }
}
