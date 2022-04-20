package com.stripe.android.financialconnections

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_OFF
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.utils.InjectableActivityScenario
import com.stripe.android.financialconnections.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.financialconnections.utils.injectableActivityScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FinancialFinancialConnectionsSheetActivityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = FinancialConnectionsSheetContract()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val args = FinancialConnectionsSheetContract.Args(configuration)
    private val intent = contract.createIntent(context, args)
    private val viewModel = createViewModel()

    private fun createViewModel(): FinancialConnectionsSheetViewModel = runBlocking {
        FinancialConnectionsSheetViewModel(
            applicationId = "com.example.test",
            starterArgs = args,
            savedStateHandle = SavedStateHandle(),
            generateLinkAccountSessionManifest = mock(),
            fetchLinkAccountSession = mock(),
            eventReporter = mock()
        )
    }

    private fun activityScenario(
        viewModel: FinancialConnectionsSheetViewModel = this.viewModel
    ): InjectableActivityScenario<FinancialConnectionsSheetActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    @Test
    fun `onCreate() with no args returns Failed result`() {
        val scenario = activityScenario()
        val intent = Intent(context, FinancialConnectionsSheetActivity::class.java)
        scenario.launch(intent)
        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isInstanceOf(
            FinancialConnectionsSheetResult.Failed::class.java
        )
    }

    @Test
    fun `onCreate() with invalid args returns Failed result`() {
        val scenario = activityScenario()
        val configuration = FinancialConnectionsSheet.Configuration("", "")
        val args = FinancialConnectionsSheetContract.Args(configuration)
        val intent = contract.createIntent(context, args)
        scenario.launch(intent)
        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isInstanceOf(
            FinancialConnectionsSheetResult.Failed::class.java
        )
    }

    @Test
    fun `viewEffect - OpenAuthFlowWithUrl opens Chrome Custom Tab intent`() {
        val chromeCustomTabUrl = "www.authflow.com"
        val viewEffects = MutableSharedFlow<FinancialConnectionsSheetViewEffect>()
        val mockViewModel = mock<FinancialConnectionsSheetViewModel> {
            on { viewEffect } doReturn viewEffects
        }
        activityScenario(mockViewModel).launch(intent).suspendOnActivity {
            viewEffects.emit(FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl(chromeCustomTabUrl))
            val intent: Intent = shadowOf(it).nextStartedActivity
            assertThat(intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, 0)).isEqualTo(
                SHARE_STATE_OFF
            )
        }
    }

    @Test
    fun `onNewIntent() calls view model handleOnNewIntent()`() {
        val mockViewModel = mock<FinancialConnectionsSheetViewModel>()
        val scenario = activityScenario(mockViewModel)
        scenario.launch(intent).suspendOnActivity {
            val newIntent = Intent(Intent.ACTION_VIEW)
            newIntent.data = Uri.parse(ApiKeyFixtures.SUCCESS_URL)
            it.onNewIntent(newIntent)
            val argument: ArgumentCaptor<Intent> = ArgumentCaptor.forClass(Intent::class.java)
            verify(mockViewModel).handleOnNewIntent(argument.capture())
            assertThat(argument.value.data.toString()).isEqualTo(ApiKeyFixtures.SUCCESS_URL)
        }
    }

    @Test
    fun `onBackPressed() cancels connection sheet`() {
        val mockViewModel = mock<FinancialConnectionsSheetViewModel> {
            on { state } doReturn MutableStateFlow(FinancialConnectionsSheetState(authFlowActive = true))
        }
        val scenario = activityScenario(mockViewModel)
        scenario.launch(intent).suspendOnActivity {
            it.onBackPressed()
        }
        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isInstanceOf(
            FinancialConnectionsSheetResult.Canceled::class.java
        )
    }

    /**
     * When [InjectableActivityScenario.onActivity] triggers,
     * runs the given block within the provided [TestScope].
     */
    private fun InjectableActivityScenario<FinancialConnectionsSheetActivity>.suspendOnActivity(
        block: suspend TestScope.(FinancialConnectionsSheetActivity) -> Unit
    ) {
        this.onActivity {
            runTest {
                block(this, it)
            }
        }
    }
}
