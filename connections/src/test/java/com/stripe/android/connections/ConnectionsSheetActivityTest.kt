package com.stripe.android.connections

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.utils.InjectableActivityScenario
import com.stripe.android.connections.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.connections.utils.injectableActivityScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ConnectionsSheetActivityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = ConnectionsSheetContract()
    private val configuration = ConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val args = ConnectionsSheetContract.Args(configuration)
    private val intent = contract.createIntent(context, args)

    private val viewEffects = MutableSharedFlow<ConnectionsSheetViewEffect>()
    private val states = MutableStateFlow(ConnectionsSheetState())

    private val mockViewModel = mock<ConnectionsSheetViewModel> {
        on { state } doReturn states
        on { viewEffect } doReturn viewEffects
    }

    private fun activityScenario(
        viewModel: ConnectionsSheetViewModel
    ): InjectableActivityScenario<ConnectionsSheetActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    @Test
    fun `onCreate() with no args returns Failed result`() {
        val scenario = activityScenario(mockViewModel)
        val intent = Intent(context, ConnectionsSheetActivity::class.java)
        scenario.launch(intent)
        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isInstanceOf(
            ConnectionsSheetResult.Failed::class.java
        )
    }

    @Test
    fun `onCreate() with invalid args returns Failed result`() {
        val scenario = activityScenario(mockViewModel)
        val configuration = ConnectionsSheet.Configuration("", "")
        val args = ConnectionsSheetContract.Args(configuration)
        val intent = contract.createIntent(context, args)
        scenario.launch(intent)
        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isInstanceOf(
            ConnectionsSheetResult.Failed::class.java
        )
    }

    @Test
    fun `viewEffect - OpenAuthFlowWithUrl starts activity with intent`() {
        activityScenario(mockViewModel).launch(intent).suspendOnActivity { activity, _ ->
            val mockDestinationActivity = ConnectionsSheetRedirectActivity::class.java
            val mockIntent = Intent(activity, mockDestinationActivity)
            viewEffects.emit(ConnectionsSheetViewEffect.OpenAuthFlowWithIntent(mockIntent))
            val intent: Intent = shadowOf(activity).nextStartedActivity
            assertThat(shadowOf(intent).intentClass).isEqualTo(mockDestinationActivity)
        }
    }

    @Test
    fun `onNewIntent() calls view model handleOnNewIntent()`() {
        activityScenario(mockViewModel).launch(intent).suspendOnActivity { activity, _ ->
            val newIntent = Intent(Intent.ACTION_VIEW)
            newIntent.data = Uri.parse(ApiKeyFixtures.SUCCESS_URL)
            activity.onNewIntent(newIntent)
            val argument: ArgumentCaptor<Intent> = ArgumentCaptor.forClass(Intent::class.java)
            verify(mockViewModel).handleOnNewIntent(argument.capture())
            assertThat(argument.value.data.toString()).isEqualTo(ApiKeyFixtures.SUCCESS_URL)
        }
    }

    @Test
    fun `onBackPressed() cancels connection sheet`() {
        activityScenario(mockViewModel).launch(intent).suspendOnActivity { activity, scenario ->
            activity.onBackPressed()
            assertThat(
                contract.parseResult(
                    scenario.getResult().resultCode,
                    scenario.getResult().resultData
                )
            ).isInstanceOf(
                ConnectionsSheetResult.Canceled::class.java
            )
        }
    }

    /**
     * When [InjectableActivityScenario.onActivity] triggers,
     * runs the given block within the provided [TestScope].
     */
    private fun InjectableActivityScenario<ConnectionsSheetActivity>.suspendOnActivity(
        block: suspend TestScope.(
            ConnectionsSheetActivity,
            InjectableActivityScenario<ConnectionsSheetActivity>
        ) -> Unit
    ) {
        onActivity {
            runTest {
                block(this, it, this@suspendOnActivity)
            }
        }
    }
}
