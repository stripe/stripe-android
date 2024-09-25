package com.stripe.android.link

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.link.utils.InjectableActivityScenario
import com.stripe.android.link.utils.injectableActivityScenario
import com.stripe.android.link.utils.viewModelFactoryFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class LinkActivityTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<LinkActivity>()

    private val navHostController: NavHostController = mock()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that navigator pops back on back pressed`() {
        val vm = LinkActivityViewModel()
        val scenario = activityScenario(viewModel = vm)
        scenario.launchTest {
            vm.handleAction(LinkAction.BackPressed)

            composeTestRule.waitForIdle()

            verify(navHostController).popBackStack()
        }
    }

    @Test
    fun `test that navigator navigates to wallet on wallet clicked`() {
        val vm = LinkActivityViewModel()
        val scenario = activityScenario(viewModel = vm)
        scenario.launchTest {
            vm.handleAction(LinkAction.WalletClicked)

            composeTestRule.waitForIdle()

            verify(navHostController).navigate(LinkScreen.Wallet.route)
        }
    }

    private fun InjectableActivityScenario<LinkActivity>.launchTest(
        startIntent: Intent = Intent(context, LinkActivity::class.java),
        block: (LinkActivity) -> Unit
    ) {
        launch(startIntent).onActivity { activity ->
            activity.navController = navHostController
            block(activity)
        }
    }

    private fun activityScenario(
        viewModel: LinkActivityViewModel = LinkActivityViewModel(),
    ): InjectableActivityScenario<LinkActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }
}
