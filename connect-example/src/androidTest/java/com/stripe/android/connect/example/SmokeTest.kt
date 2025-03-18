package com.stripe.android.connect.example

import androidx.compose.ui.test.assertIsDisplayed
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SmokeTest : BaseConnectTest() {
    @Test
    fun testAppOpensToLoading() {
        Main {
            titleText.assertIsDisplayed()
            loadingText.assertIsDisplayed()
        }
    }

    @Test
    fun testOpenComponentListWhenDataLoads() {
        loadData()
        ComponentList {
            accountOnboardingItem.assertIsDisplayed()
        }
    }
}
