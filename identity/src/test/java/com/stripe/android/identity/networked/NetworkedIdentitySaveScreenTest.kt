package com.stripe.android.identity.networked

import android.os.Build
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.NetworkedIdentitySaveDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
internal class NetworkedIdentitySaveScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val submit = MutableStateFlow<Resource<Int>>(Resource.idle())
    private val identityViewModel = mock<IdentityViewModel> { on { verificationPageSubmit } doReturn submit }
    private val navController = mock<NavController>()

    @Test
    fun `offers saving the ID`() = runScenario(includesSelfie = false) {
        composeRule.onNodeWithTag(NI_SAVE_BODY_TAG).assertTextEquals("Save your ID with Link.")
    }

    @Test
    fun `offers saving the ID and selfie when the verification has one`() = runScenario(includesSelfie = true) {
        composeRule.onNodeWithTag(NI_SAVE_BODY_TAG).assertTextEquals("Save your ID and selfie with Link.")
    }

    @Test
    fun `saving starts with Link`() = runScenario {
        composeRule.onNodeWithTag(NI_SAVE_BUTTON_TAG).performClick()

        assertThat(viewModel.mode.value).isEqualTo(NetworkedIdentityMode.Save)
        runBlocking { linkSession.configureCalls.awaitItem() }
    }

    @Test
    fun `saving that is starting can't be raced by finishing`() = runScenario {
        composeRule.onNodeWithTag(NI_SAVE_FINISH_TAG).assertIsEnabled()
        composeRule.onNodeWithTag(NI_SAVE_BUTTON_TAG).assertIsEnabled()

        composeRule.onNodeWithTag(NI_SAVE_BUTTON_TAG).performClick()

        composeRule.onNodeWithTag(NI_SAVE_FINISH_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(NI_SAVE_BUTTON_TAG).assertIsNotEnabled()
        runBlocking { linkSession.configureCalls.awaitItem() }
    }

    @Test
    fun `choices wait while the verification is submitted`() = runScenario {
        composeRule.onNodeWithTag(NI_SAVE_BUTTON_TAG).assertIsEnabled()
        composeRule.onNodeWithTag(NI_SAVE_FINISH_TAG).assertIsEnabled()

        submit.value = Resource.loading()

        composeRule.onNodeWithTag(NI_SAVE_BUTTON_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(NI_SAVE_FINISH_TAG).assertIsNotEnabled()
    }

    @Test
    fun `a recorded save can't be finished without saving`() = runScenario(
        config = niConfig(route = NetworkedIdentityRoute.ResumeSave)
    ) {
        composeRule.onNodeWithTag(NI_SAVE_FINISH_TAG).assertDoesNotExist()
    }

    @Test
    fun `finishing without saving records the decline, then submits`() = runScenario {
        composeRule.onNodeWithTag(NI_SAVE_FINISH_TAG).performClick()
        verifyBlocking(identityViewModel, never()) { submitAndNavigate(any(), any()) }

        runBlocking { actions.skipCalls.awaitItem() }.response.complete(Result.success(niActionPageData()))
        composeRule.waitForIdle()

        verifyBlocking(identityViewModel) {
            submitAndNavigate(navController, NetworkedIdentitySaveDestination.ROUTE.route)
        }
    }

    private fun runScenario(
        includesSelfie: Boolean = false,
        config: NetworkedIdentityConfig = niConfig(),
        block: Scenario.() -> Unit,
    ) {
        val linkSession = FakeNetworkedIdentityLinkSession()
        val repository = FakeNetworkedIdentityRepository()
        val actions = FakeNetworkedIdentityActions()
        val viewModel = niViewModel(
            linkSession = linkSession,
            repository = repository,
            actions = actions,
            config = config,
            includesSelfie = includesSelfie,
        )
        composeRule.setContent {
            NetworkedIdentitySaveScreen(navController, identityViewModel, viewModel)
        }

        Scenario(viewModel, linkSession, actions).block()

        linkSession.ensureAllEventsConsumed()
        repository.ensureAllEventsConsumed()
        actions.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val viewModel: NetworkedIdentityViewModel,
        val linkSession: FakeNetworkedIdentityLinkSession,
        val actions: FakeNetworkedIdentityActions,
    )
}
