package com.stripe.android.financialconnections.presentation

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.ThemeMode
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetFlowType
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.VisualUpdate
import com.stripe.android.financialconnections.ui.theme.Theme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class FinancialConnectionsSheetNativeStateTest {

    private val configuration = FinancialConnectionsSheetConfiguration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_API_CONFIG,
        preCollectedConsent = null,
        themeMode = ThemeMode.Automatic,
    )

    @Test
    fun `theme preserves the configured mode for every manifest branding`(
        @TestParameter themeMode: ThemeMode,
        @TestParameter(valuesProvider = ManifestThemeProvider::class)
        manifestTheme: FinancialConnectionsSessionManifest.Theme?,
    ) {
        val syncResponse = ApiKeyFixtures.syncResponse()
        val state = FinancialConnectionsSheetNativeState(
            args = FinancialConnectionsSheetNativeActivityArgs(
                flowType = FinancialConnectionsSheetFlowType.ForData,
                configuration = FinancialConnectionsSheetConfiguration(
                    ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                    ApiKeyFixtures.DEFAULT_API_CONFIG,
                    preCollectedConsent = null,
                    themeMode = themeMode,
                ),
                initialSyncResponse = syncResponse.copy(
                    manifest = syncResponse.manifest.copy(theme = manifestTheme),
                ),
                elementsSessionContext = null,
            ),
            savedState = null,
        )

        assertThat(state.theme.mode).isEqualTo(themeMode)
        val expectedType = if (manifestTheme == FinancialConnectionsSessionManifest.Theme.LINK_LIGHT) {
            Theme.LinkLight::class.java
        } else {
            Theme.DefaultLight::class.java
        }
        assertThat(state.theme).isInstanceOf(expectedType)
    }

    @Test
    fun `init - reducedBranding includes reducedBranding when true`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    VisualUpdate(
                        reducedBranding = true,
                        reducedManualEntryProminenceInErrors = false,
                        merchantLogos = emptyList()
                    )
                ),
                savedState = null
            ).reducedBranding,
        ).isTrue()
    }

    @Test
    fun `init - reducedBranding includes reducedBranding when false`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    VisualUpdate(
                        reducedBranding = false,
                        reducedManualEntryProminenceInErrors = false,
                        merchantLogos = emptyList()
                    )
                ),
                savedState = null
            ).reducedBranding,
        ).isFalse()
    }

    private fun args(visual: VisualUpdate) = FinancialConnectionsSheetNativeActivityArgs(
        flowType = FinancialConnectionsSheetFlowType.ForInstantDebits,
        configuration = configuration,
        initialSyncResponse = ApiKeyFixtures.syncResponse().copy(
            visual = visual
        )
    )

    class ManifestThemeProvider : TestParameterValuesProvider() {
        override fun provideValues(
            context: TestParameterValuesProvider.Context?,
        ): List<FinancialConnectionsSessionManifest.Theme?> =
            listOf(null) + FinancialConnectionsSessionManifest.Theme.entries
    }
}
