package com.stripe.android.payments.bankaccount.domain

import androidx.activity.ComponentActivity
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForInstantDebitsLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BuildFinancialConnectionsLauncherTest {

    private lateinit var activity: ComponentActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).get()
    }

    @Test
    fun `when ACH and availability is Full, intent points to FinancialConnectionsSheetActivity`() {
        val config = CollectBankAccountConfiguration.USBankAccount(
            name = "Test Name",
            email = "test@example.com"
        )

        val testArgs = FinancialConnectionsSheetActivityArgs.ForData(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = "test_secret",
                publishableKey = "test_key"
            ),
            elementsSessionContext = null
        )

        val availability = FinancialConnectionsAvailability.Full
        val launcher = BuildFinancialConnectionsLauncher(
            activity = activity,
            configuration = config,
            financialConnectionsAvailability = availability,
            onConnectionsForInstantDebitsResult = { },
            onConnectionsForACHResult = { }
        ) as FinancialConnectionsSheetForDataLauncher

        val resultContract = launcher.activityResultLauncher.contract
        val intent = resultContract.createIntent(activity, testArgs)
        assertEquals(
            intent.component!!.className,
            FULL_ACTIVITY
        )
    }

    @Test
    fun `when ACH and availability is Lite, intent points to FinancialConnectionsSheetActivity`() {
        val config = CollectBankAccountConfiguration.USBankAccount(
            name = "Test Name",
            email = "test@example.com"
        )

        val testArgs = FinancialConnectionsSheetActivityArgs.ForData(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = "test_secret",
                publishableKey = "test_key"
            ),
            elementsSessionContext = null
        )

        val availability = FinancialConnectionsAvailability.Lite
        val launcher = BuildFinancialConnectionsLauncher(
            activity = activity,
            configuration = config,
            financialConnectionsAvailability = availability,
            onConnectionsForInstantDebitsResult = { },
            onConnectionsForACHResult = { }
        ) as FinancialConnectionsSheetForDataLauncher

        val resultContract = launcher.activityResultLauncher.contract
        val intent = resultContract.createIntent(activity, testArgs)
        assertEquals(
            intent.component!!.className,
            LITE_ACTIVITY
        )
    }

    @Test
    fun `when InstantDebits availability is lite, returns instant debits launcher pointing to lite activity`() {
        val config = CollectBankAccountConfiguration.InstantDebits(
            email = "email@test.com",
            elementsSessionContext = null
        )

        val testArgs = FinancialConnectionsSheetActivityArgs.ForInstantDebits(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = "test_secret",
                publishableKey = "test_key"
            ),
            elementsSessionContext = null
        )

        val availability = FinancialConnectionsAvailability.Lite
        val launcher = BuildFinancialConnectionsLauncher(
            activity = activity,
            configuration = config,
            financialConnectionsAvailability = availability,
            onConnectionsForInstantDebitsResult = { },
            onConnectionsForACHResult = { }
        ) as FinancialConnectionsSheetForInstantDebitsLauncher

        val resultContract = launcher.activityResultLauncher.contract
        val intent = resultContract.createIntent(activity, testArgs)
        assertEquals(
            intent.component!!.className,
            LITE_ACTIVITY
        )
    }

    @Test
    fun `when InstantDebits and availability is full, returns instant debits launcher pointing to full activity`() {
        val config = CollectBankAccountConfiguration.InstantDebits(
            email = "email@test.com",
            elementsSessionContext = null
        )

        val testArgs = FinancialConnectionsSheetActivityArgs.ForInstantDebits(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = "test_secret",
                publishableKey = "test_key"
            ),
            elementsSessionContext = null
        )

        val availability = FinancialConnectionsAvailability.Full
        val launcher = BuildFinancialConnectionsLauncher(
            activity = activity,
            configuration = config,
            financialConnectionsAvailability = availability,
            onConnectionsForInstantDebitsResult = { },
            onConnectionsForACHResult = { }
        ) as FinancialConnectionsSheetForInstantDebitsLauncher

        val resultContract = launcher.activityResultLauncher.contract
        val intent = resultContract.createIntent(activity, testArgs)
        assertEquals(
            intent.component!!.className,
            FULL_ACTIVITY
        )
    }

    companion object {
        private const val LITE_ACTIVITY =
            "com.stripe.android.financialconnections.lite.FinancialConnectionsSheetLiteActivity"
        private const val FULL_ACTIVITY =
            "com.stripe.android.financialconnections.FinancialConnectionsSheetActivity"
    }
}
