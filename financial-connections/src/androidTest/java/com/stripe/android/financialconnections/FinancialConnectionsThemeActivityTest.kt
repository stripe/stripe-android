package com.stripe.android.financialconnections

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetFlowType
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.VisualUpdate
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.theme.Neutral100
import com.stripe.android.financialconnections.ui.theme.Neutral100Dark
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class FinancialConnectionsThemeActivityTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun forcedThemeDoesNotRecreateActivity(
        @TestParameter activityType: ActivityType,
        @TestParameter forcedTheme: ForcedTheme,
    ) = runTest {
        runScenario(activityType, forcedTheme) { activity ->
            val expectedColor = if (forcedTheme.isDark) Neutral100Dark else Neutral100
            assertThat(activity.window.navigationBarColor).isEqualTo(expectedColor.toArgb())
            assertThat(
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .isAppearanceLightNavigationBars
            ).isEqualTo(!forcedTheme.isDark)
            val hostIsDark = activity.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            assertThat(hostIsDark).isEqualTo(!forcedTheme.isDark)
            if (activity is FinancialConnectionsSheetActivity) {
                assertThat(activity.viewModel.stateFlow.value.activityRecreated).isFalse()
            }
        }
    }

    private suspend fun runScenario(
        activityType: ActivityType,
        forcedTheme: ForcedTheme,
        block: (Activity) -> Unit,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val lifecycleMonitor = ActivityLifecycleMonitorRegistry.getInstance()
        val creations = Turbine<Activity>()
        val callback = ActivityLifecycleCallback { activity, stage ->
            if (stage == Stage.CREATED && activityType.activityClass.isInstance(activity)) {
                creations.add(activity)
            }
        }
        val previousNightMode = AppCompatDelegate.getDefaultNightMode()
        val analyticsEnabled = AnalyticsRequestExecutor.ENABLED
        instrumentation.runOnMainSync {
            AnalyticsRequestExecutor.ENABLED = false
            AppCompatDelegate.setDefaultNightMode(
                if (forcedTheme.isDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            )
            lifecycleMonitor.addLifecycleCallback(callback)
        }
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val intent = activityType.intent(context, forcedTheme.themeMode)
            ActivityScenario.launch<Activity>(intent).use { scenario ->
                composeRule.waitForIdle()
                val createdActivity = creations.awaitItem()
                scenario.onActivity { activity ->
                    assertThat(activity).isSameInstanceAs(createdActivity)
                    block(activity)
                }
                creations.ensureAllEventsConsumed()
            }
        } finally {
            instrumentation.runOnMainSync {
                lifecycleMonitor.removeLifecycleCallback(callback)
                AppCompatDelegate.setDefaultNightMode(previousNightMode)
                AnalyticsRequestExecutor.ENABLED = analyticsEnabled
            }
        }
    }

    enum class ForcedTheme(val themeMode: ThemeMode, val isDark: Boolean) {
        LightOnDarkHost(ThemeMode.AlwaysLight, false),
        DarkOnLightHost(ThemeMode.AlwaysDark, true),
    }

    enum class ActivityType(val activityClass: Class<out Activity>) {
        Loading(FinancialConnectionsSheetActivity::class.java),
        Native(FinancialConnectionsSheetNativeActivity::class.java);

        fun intent(context: Context, themeMode: ThemeMode): Intent {
            val configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = "fcsess_theme_secret",
                apiConfiguration = ApiConfiguration.State("pk_test_theme", null),
                preCollectedConsent = null,
                themeMode = themeMode,
            )
            return when (this) {
                Loading -> FinancialConnectionsSheetActivity.intent(
                    context,
                    FinancialConnectionsSheetActivityArgs.ForData(configuration, elementsSessionContext = null),
                ).putExtra(
                    FinancialConnectionsSheetState.KEY_SAVED_STATE,
                    Bundle().apply {
                        putParcelable(FinancialConnectionsSheetState.KEY_MANIFEST, manifest)
                        putSerializable(
                            FinancialConnectionsSheetState.KEY_WEB_AUTH_FLOW_STATUS,
                            FinancialConnectionsSheetState.AuthFlowStatus.NONE,
                        )
                    },
                )
                Native -> FinancialConnectionsSheetNativeActivity.intent(
                    context,
                    FinancialConnectionsSheetNativeActivityArgs(
                        flowType = FinancialConnectionsSheetFlowType.ForData,
                        configuration = configuration,
                        initialSyncResponse = SynchronizeSessionResponse(
                            manifest = manifest,
                            text = null,
                            visual = VisualUpdate(false, false, emptyList()),
                        ),
                        elementsSessionContext = null,
                    ),
                )
            }
        }
    }

    private companion object {
        val manifest = FinancialConnectionsSessionManifest(
            allowManualEntry = true,
            consentRequired = false,
            consentAcquiredAt = null,
            customManualEntryHandling = false,
            disableLinkMoreAccounts = true,
            id = "fcsess_theme",
            instantVerificationDisabled = false,
            institutionSearchDisabled = false,
            appVerificationEnabled = false,
            livemode = true,
            manualEntryUsesMicrodeposits = true,
            mobileHandoffEnabled = false,
            nextPane = FinancialConnectionsSessionManifest.Pane.MANUAL_ENTRY,
            manualEntryMode = ManualEntryMode.AUTOMATIC,
            permissions = emptyList(),
            product = FinancialConnectionsSessionManifest.Product.STRIPE_CARD,
            singleAccount = true,
            useSingleSortSearch = true,
            theme = FinancialConnectionsSessionManifest.Theme.LIGHT,
        )
    }
}
