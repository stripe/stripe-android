package com.stripe.android.connect.example

import androidx.compose.ui.test.assertIsDisplayed
import com.stripe.android.connect.example.util.Fixtures
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SettingsTest : ConnectTest() {
    @Test
    fun testSettingsOpensWithAccountsLoaded() {
        loadData()
        ComponentList {
            openSettings()
        }
        Settings {
            demoAccountHeader.assertIsDisplayed()
            hasAccount(Fixtures.merchant().merchantId)
        }
    }
}
