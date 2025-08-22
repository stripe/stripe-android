package com.stripe.android.cardscan

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.cardscan.CardScanContract
import com.stripe.android.ui.core.cardscan.CardScanGoogleLauncher
import com.stripe.android.ui.core.cardscan.FakeCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalTestCardScanGoogleLauncher
import com.stripe.android.ui.core.elements.CardDetailsController
import com.stripe.android.ui.core.elements.CardDetailsElement
import com.stripe.android.ui.core.elements.CardDetailsSectionController
import com.stripe.android.ui.core.elements.ScanCardButtonContent
import com.stripe.android.ui.core.elements.ScanCardButtonUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ScanCardButtonUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeEventsReporter = FakeCardScanEventsReporter()

    // Create mock controller for visibility tests
    private fun createMockController(
        isStripeCardScanAvailable: Boolean = true
    ): CardDetailsSectionController {
        val controller = mock<CardDetailsSectionController>()
        val mockIsStripeCardScanAvailable = mock<DefaultIsStripeCardScanAvailable>()
        val mockCardDetailsElement = mock<CardDetailsElement>()
        val mockCardDetailsController = mock<CardDetailsController>()

        // Always return true even in production code
        whenever(controller.isCardScanEnabled).thenReturn(true)
        whenever(controller.isStripeCardScanAvailable).thenReturn(mockIsStripeCardScanAvailable)
        whenever(mockIsStripeCardScanAvailable.invoke()).thenReturn(isStripeCardScanAvailable)
        whenever(controller.elementsSessionId).thenReturn("test_session")
        whenever(controller.cardDetailsElement).thenReturn(mockCardDetailsElement)
        whenever(mockCardDetailsElement.controller).thenReturn(mockCardDetailsController)
        
        // Mock the onCardScanResult function that Google launcher needs
        whenever(mockCardDetailsController.onCardScanResult).thenReturn { }
        
        return controller
    }

    @Test
    fun `ScanCardButtonUI shows when card scan is enabled and available`() = runTest {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
        val controller = createMockController(
            isStripeCardScanAvailable = true
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardScanEventsReporter provides fakeEventsReporter
            ) {
                ScanCardButtonUI(
                    enabled = true,
                    controller = controller
                )
            }
        }

        composeTestRule.onNode(hasText("Scan card")).assertExists()
    }

    @Test
    fun `ScanCardButtonUI shows when Google migration is enabled`() = runTest {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)
        val controller = createMockController(
            isStripeCardScanAvailable = false // Even when Stripe scan unavailable
        )

        // Mock the Google launcher to be available
        val mockGoogleLauncher = mock<CardScanGoogleLauncher>()
        val availabilityFlow = MutableStateFlow(true)
        whenever(mockGoogleLauncher.isAvailable).thenReturn(availabilityFlow)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardScanEventsReporter provides fakeEventsReporter,
                LocalTestCardScanGoogleLauncher provides mockGoogleLauncher
            ) {
                ScanCardButtonUI(
                    enabled = true,
                    controller = controller
                )
            }
        }

        composeTestRule.onNode(hasText("Scan card")).assertExists()
    }

    @Test
    fun `ScanCardButtonUI hidden when card scan is disabled`() = runTest {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
        val controller = createMockController(
            isStripeCardScanAvailable = false
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardScanEventsReporter provides fakeEventsReporter
            ) {
                ScanCardButtonUI(
                    enabled = true,
                    controller = controller
                )
            }
        }

        composeTestRule.onNodeWithText("Scan card").assertDoesNotExist()
    }

    @Test
    fun `ScanCardButtonContent click behavior with Google launcher`() = runTest {
        val mockCardScanLauncher = mock<ManagedActivityResultLauncher<CardScanContract.Args, CardScanSheetResult>>()
        val mockCardScanGoogleLauncher = mock<CardScanGoogleLauncher>()
        
        // Mock the Google launcher as available
        val availabilityFlow = MutableStateFlow(true)
        whenever(mockCardScanGoogleLauncher.isAvailable).thenReturn(availabilityFlow)
        
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardScanEventsReporter provides fakeEventsReporter
            ) {
                ScanCardButtonContent(
                    enabled = true,
                    elementsSessionId = "test_session",
                    cardScanLauncher = mockCardScanLauncher,
                    cardScanGoogleLauncher = mockCardScanGoogleLauncher
                )
            }
        }

        composeTestRule.onNode(hasText("Scan card")).performClick()
        
        // When Google migration is enabled, should use Google launcher
        verify(mockCardScanGoogleLauncher).launch(any())
    }

    @Test
    fun `ScanCardButtonContent click behavior with legacy launcher`() = runTest {
        val mockCardScanLauncher = mock<ManagedActivityResultLauncher<CardScanContract.Args, CardScanSheetResult>>()
        
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardScanEventsReporter provides fakeEventsReporter
            ) {
                ScanCardButtonContent(
                    enabled = true,
                    elementsSessionId = "test_session",
                    cardScanLauncher = mockCardScanLauncher,
                    cardScanGoogleLauncher = null
                )
            }
        }

        composeTestRule.onNode(hasText("Scan card")).performClick()
        
        // When Google migration is disabled, should use legacy launcher
        verify(mockCardScanLauncher).launch(any())
    }
}