package com.stripe.android.identity.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class DocumentScanScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockNavController = mock<NavController>()

    private val targetScanTypeFlow =
        MutableStateFlow<IdentityScanState.ScanType?>(null)
    private val scannerStateFlow =
        MutableStateFlow<IdentityScanViewModel.State>(IdentityScanViewModel.State.Initializing)
    private val collectedDataFlow = MutableStateFlow(CollectedDataParam())
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { identityAnalyticsRequestFactory } doReturn mock()
        on { workContext } doReturn UnconfinedTestDispatcher()
        on { screenTracker } doReturn mock()
        on { pageAndModelFiles } doReturn MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>(
            Resource.success(
                IdentityViewModel.PageAndModelFiles(mock(), mock(), null)
            )
        )
        on { collectedData } doReturn collectedDataFlow
    }
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel> {
        on { targetScanTypeFlow } doReturn targetScanTypeFlow
        on { fpsTracker } doReturn mock()
        on { scannerState } doReturn scannerStateFlow
    }

    @Test
    fun verifyLoading() {
        testDocumentScanScreen(
            scannerState = IdentityScanViewModel.State.Initializing
        ) {
            onNodeWithTag(LOADING_SCREEN_TAG).assertExists()
        }
    }

    @Test
    fun verifyInitializingState() {
        testDocumentScanScreen(
            scannerState = IdentityScanViewModel.State.Initializing
        ) {
            onNodeWithTag(LOADING_SCREEN_TAG).assertExists()
        }
    }

    @Test
    fun verifyScanningNullState() {
        testDocumentScanScreen(
            scannerState = IdentityScanViewModel.State.Scanning(),
        ) {
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_front_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_id_front))
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyScanningStateWithFrontType() {
        testDocumentScanScreen(
            targetScanType = IdentityScanState.ScanType.DOC_FRONT,
            scannerState = IdentityScanViewModel.State.Scanning(mock<IdentityScanState.Initial>())
        ) {
            verify(mockIdentityScanViewModel).startScan(
                eq(IdentityScanState.ScanType.DOC_FRONT),
                any()
            )
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_front_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_id_front))
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyScanningStateWithBackType() {
        testDocumentScanScreen(
            targetScanType = IdentityScanState.ScanType.DOC_BACK,
            shouldStartFromBack = true,
            scannerState = IdentityScanViewModel.State.Scanning(mock<IdentityScanState.Initial>())
        ) {
            verify(mockIdentityScanViewModel).startScan(
                eq(IdentityScanState.ScanType.DOC_BACK),
                any()
            )
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_back_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_id_back))
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyScannedState() {
        testDocumentScanScreen(
            targetScanType = IdentityScanState.ScanType.DOC_FRONT,
            scannerState = IdentityScanViewModel.State.Scanned(mock())
        ) {
            verify(mockIdentityScanViewModel).stopScan(any())
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_front_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_scanned))
            onNodeWithTag(CHECK_MARK_TAG).assertExists()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsEnabled()

            onNodeWithTag(CONTINUE_BUTTON_TAG).performClick()
            runBlocking {
                verify(mockIdentityViewModel).collectDataForDocumentScanScreen(
                    eq(mockNavController),
                    eq(true),
                    any()
                )
            }
        }
    }

    private fun testDocumentScanScreen(
        targetScanType: IdentityScanState.ScanType? = null,
        shouldStartFromBack: Boolean = false,
        scannerState: IdentityScanViewModel.State,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        targetScanTypeFlow.update { targetScanType }
        collectedDataFlow.update {
            if (shouldStartFromBack) {
                CollectedDataParam(
                    idDocumentFront = mock()
                )
            } else {
                CollectedDataParam()
            }
        }
        scannerStateFlow.update { scannerState }
        composeTestRule.setContent {
            DocumentScanScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                identityScanViewModel = mockIdentityScanViewModel,
            )
        }
        with(composeTestRule, testBlock)
    }
}
