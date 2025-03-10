package com.stripe.android.connect.example

import androidx.compose.ui.test.assertIsDisplayed
import com.stripe.android.connect.example.util.Fixtures
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SettingsTest : BaseConnectTest() {
    @Test
    fun testSettingsOpens() {
        loadData()
        navigateToSettings()
    }

    @Test
    fun testAccountsListUpdatesOnDataChanges() {
        val merchants = listOf(
            Fixtures.merchant(merchantId = "acct_123"),
            Fixtures.merchant(merchantId = "acct_456"),
        )
        loadData(merchants.subList(0, 1))
        navigateToSettings()
        Settings {
            hasAccount(merchants[0].merchantId)
            loadData(merchants)
            hasAccount(merchants[1].merchantId)
        }
    }

    private fun navigateToSettings() {
        ComponentList {
            openSettings()
        }
        Settings {
            demoAccountHeader.assertIsDisplayed()
        }
    }
}
