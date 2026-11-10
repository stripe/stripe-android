package com.stripe.android.financialconnections

import android.content.Intent
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataContract
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import java.security.InvalidParameterException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestParameterInjector::class)
class FinancialConnectionsSheetForDataContractTest {

    @Test
    fun `theme mode survives parceling the launch arguments`(@TestParameter themeMode: ThemeMode) {
        val args = FinancialConnectionsSheetActivityArgs.ForData(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
                preCollectedConsent = null,
                themeMode = themeMode,
            ),
        )
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(args, 0)
            parcel.setDataPosition(0)
            val restored = parcel.readParcelable<FinancialConnectionsSheetActivityArgs.ForData>(
                FinancialConnectionsSheetActivityArgs.ForData::class.java.classLoader,
            )
            assertThat(restored?.configuration?.themeMode).isEqualTo(themeMode)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `parseResult() with RESULT_CANCELED should return canceled result`() {
        val contract = FinancialConnectionsSheetForDataContract { Intent() }
        val result = contract.parseResult(android.app.Activity.RESULT_CANCELED, null)

        assertThat(result).isInstanceOf(FinancialConnectionsSheetResult.Canceled::class.java)
    }

    @Test
    fun `validate() valid args`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_API_CONFIG,
            preCollectedConsent = null,
            themeMode = ThemeMode.Automatic,
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        args.validate()
    }

    @Test
    fun `validate() missing session client secret`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            " ",
            ApiKeyFixtures.DEFAULT_API_CONFIG,
            preCollectedConsent = null,
            themeMode = ThemeMode.Automatic,
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        assertFailsWith<InvalidParameterException>(
            "The financial connections session client secret cannot be an empty string."
        ) {
            args.validate()
        }
    }

    @Test
    fun `validate() missing publishable key`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_API_CONFIG.copy(publishableKey = " "),
            preCollectedConsent = null,
            themeMode = ThemeMode.Automatic,
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        assertFailsWith<InvalidParameterException>(
            "The publishable key cannot be an empty string."
        ) {
            args.validate()
        }
    }
}
