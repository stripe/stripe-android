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
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.IDScanDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
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
import org.mockito.kotlin.times
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

    private val verificationPageLiveData =
        MutableLiveData(Resource.success(mock<VerificationPage>()))
    private val targetScanTypeFlow = MutableStateFlow<IdentityScanState.ScanType?>(null)
    private val displayStateChangedFlow =
        MutableStateFlow<Pair<IdentityScanState, IdentityScanState?>?>(null)

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageLiveData
        on { pageAndModelFiles } doReturn mock()
        on { identityAnalyticsRequestFactory } doReturn mock()
        on { workContext } doReturn UnconfinedTestDispatcher()
        on { screenTracker } doReturn mock()
    }
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel> {
        on { targetScanTypeFlow } doReturn targetScanTypeFlow
        on { displayStateChangedFlow } doReturn displayStateChangedFlow
        on { interimResults } doReturn mock()
        on { finalResult } doReturn mock()
    }

    @Test
    fun verifyNullState() {
        testDocumentScanScreen {
            verify(mockIdentityViewModel, times(0)).resetDocumentUploadedState()
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_front_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_id_front))
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyNullStateWithShouldStartFromBack() {
        testDocumentScanScreen(shouldStartFromBack = true) {
            verify(mockIdentityViewModel).resetDocumentUploadedState()
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_front_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_id_front))
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyInitialStateWithFrontType() {
        testDocumentScanScreen(
            displayState = mock<IdentityScanState.Initial>(),
            targetScanType = IdentityScanState.ScanType.ID_FRONT
        ) {
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_front_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_id_front))
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyInitialStateWithBackType() {
        testDocumentScanScreen(
            displayState = mock<IdentityScanState.Initial>(),
            targetScanType = IdentityScanState.ScanType.ID_BACK
        ) {
            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_back_of_id))
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_id_back))
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyFinishedState() {
        testDocumentScanScreen(
            displayState = mock<IdentityScanState.Finished>(),
            targetScanType = IdentityScanState.ScanType.ID_FRONT
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
                    eq(CollectedDataParam.Type.IDCARD),
                    eq(IDScanDestination.ROUTE.route),
                    any()
                )
            }
        }
    }

    private fun testDocumentScanScreen(
        displayState: IdentityScanState? = null,
        targetScanType: IdentityScanState.ScanType? = null,
        shouldStartFromBack: Boolean = false,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        targetScanTypeFlow.update { targetScanType }
        displayState?.let {
            displayStateChangedFlow.update { displayState to mock() }
        }
        composeTestRule.setContent {
            DocumentScanScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                identityScanViewModel = mockIdentityScanViewModel,
                frontScanType = IdentityScanState.ScanType.ID_FRONT,
                backScanType = IdentityScanState.ScanType.ID_BACK,
                shouldStartFromBack = shouldStartFromBack,
                messageRes = DocumentScanMessageRes(
                    R.string.stripe_front_of_id,
                    R.string.stripe_back_of_id,
                    R.string.stripe_position_id_front,
                    R.string.stripe_position_id_back
                ),
                collectedDataParamType = CollectedDataParam.Type.IDCARD,
                route = IDScanDestination.ROUTE.route
            )
        }
        with(composeTestRule, testBlock)
    }
}
