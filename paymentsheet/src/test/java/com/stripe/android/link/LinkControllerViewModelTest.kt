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
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.link.repositories.LinkRepository
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
    private val linkApiRepository: LinkRepository = FakeLinkRepository()
    private val controllerComponentFactory: LinkControllerComponent.Factory =
        object : LinkControllerComponent.Factory {
            override fun build(
                activity: Activity,
                lifecycleOwner: LifecycleOwner,
                activityResultRegistryOwner: ActivityResultRegistryOwner,
                presentPaymentMethodsCallback: LinkController.PresentPaymentMethodsCallback,
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

        viewModel.state(application).test {
            // Initial state is reset
            assertThat(awaitItem()).isEqualTo(LinkController.State())
        }
    }

    @Test
    fun `configure() fails when loader fails`() = runTest {
        val error = Exception("Failed to load")
        val linkConfigurationLoader = FakeLinkConfigurationLoader()
        val viewModel = createViewModel(linkConfigurationLoader = linkConfigurationLoader)

        linkConfigurationLoader.linkConfigurationResult = Result.failure(error)

        assertThat(viewModel.configure(mock())).isEqualTo(LinkController.ConfigureResult.Failed(error))

        viewModel.state(application).test {
            assertThat(awaitItem()).isEqualTo(LinkController.State())
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
            linkApiRepository = linkApiRepository,
            controllerComponentFactory = controllerComponentFactory
        )
    }
}
