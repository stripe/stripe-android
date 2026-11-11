package com.stripe.android.financialconnections

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetFlowType
import com.stripe.android.financialconnections.ui.theme.Theme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class FinancialConnectionsSheetStateTest {
    @Test
    fun `theme preserves the configured mode for each flow`(
        @TestParameter themeMode: ThemeMode,
        @TestParameter(value = ["ForData", "ForInstantDebits"]) flowType: FinancialConnectionsSheetFlowType,
    ) {
        val configuration = FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
            preCollectedConsent = null,
            themeMode = themeMode,
        )
        val args = if (flowType == FinancialConnectionsSheetFlowType.ForInstantDebits) {
            FinancialConnectionsSheetActivityArgs.ForInstantDebits(configuration, elementsSessionContext = null)
        } else {
            FinancialConnectionsSheetActivityArgs.ForData(configuration, elementsSessionContext = null)
        }
        val state = FinancialConnectionsSheetState(args = args, savedState = null)

        assertThat(state.theme.mode).isEqualTo(themeMode)
        val expectedType = if (flowType == FinancialConnectionsSheetFlowType.ForInstantDebits) {
            Theme.Link::class.java
        } else {
            Theme.Default::class.java
        }
        assertThat(state.theme).isInstanceOf(expectedType)
    }
}
