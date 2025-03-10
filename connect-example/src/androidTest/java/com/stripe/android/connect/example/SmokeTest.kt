package com.stripe.android.connect.example

import androidx.compose.ui.test.assertIsDisplayed
import com.stripe.android.connect.example.data.FakeEmbeddedComponentService
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SmokeTest : ConnectTest() {
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
