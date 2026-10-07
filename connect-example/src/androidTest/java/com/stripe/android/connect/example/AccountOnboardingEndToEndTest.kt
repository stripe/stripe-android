package com.stripe.android.connect.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.EmbeddedComponentServiceImpl
import com.stripe.android.connect.example.data.di.TestDataModule
import com.stripe.android.testing.ExternalUiTestDriver
import com.stripe.android.testing.ExternalUiTestDriver.Companion.id
import com.stripe.android.testing.ExternalUiTestDriver.Companion.text
import com.stripe.android.testing.ExternalUiTestRule
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Singleton

/** Real-backend onboarding coverage migrated from TestAccountOnboardingLoads. */
@HiltAndroidTest
@UninstallModules(TestDataModule::class)
@RunWith(AndroidJUnit4::class)
internal class AccountOnboardingEndToEndTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val externalUiRule = ExternalUiTestRule()

    @Before
    fun setUp() {
        assumeFalse(
            "Connect's demo merchant is not supported in the edge environment",
            InstrumentationRegistry.getArguments().getString("testEnvironment") == "edge",
        )
        clearSettings()
        hiltRule.inject()
    }

    @After
    fun clearSettings() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("SettingsService", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun accountOnboardingLoads() {
        ActivityScenario.launch(MainActivity::class.java).use {
            with(ExternalUiTestDriver()) {
                click(id("settings_button"))
                enterText(id("other_account_input"), "acct_1RKLk9PwPtoT2bUJ")
                click(id("save_button"))
                click(text("Account Onboarding"))
                assertThat(await(text("Review and confirm")).text).isEqualTo("Review and confirm")
            }
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class RealBackendModule {
        @Binds
        @Singleton
        abstract fun embeddedComponentService(impl: EmbeddedComponentServiceImpl): EmbeddedComponentService
    }
}
