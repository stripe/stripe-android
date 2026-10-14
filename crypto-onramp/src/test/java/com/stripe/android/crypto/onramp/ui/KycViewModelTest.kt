package com.stripe.android.crypto.onramp.ui

import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.KycDocumentUploader
import com.stripe.android.crypto.onramp.KycSubmissionHandler
import com.stripe.android.crypto.onramp.KycSubmissionHandlerRegistry
import com.stripe.android.crypto.onramp.model.KycSubmission
import com.stripe.android.crypto.onramp.model.RetrieveKycRequirementsResponse
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class KycViewModelTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `upload survives activity recreation and completes in retained state`() = runScenario {
        viewModel.selectFile(Uri.fromFile(source), 0)
        val uploadedFile = uploader.calls.awaitItem()
        assertThat(viewModel.stateHolder.state.uploadingFileSlot).isEqualTo(0)

        controller.recreate()
        val retained = ViewModelProvider(controller.get())[KycViewModel::class.java]
        assertThat(retained).isSameInstanceAs(viewModel)
        assertThat(retained.stateHolder.state.uploadingFileSlot).isEqualTo(0)
        assertThat(uploadedFile.exists()).isTrue()
        uploader.calls.expectNoEvents()

        uploader.result.complete(Result.success("file_uploaded"))
        runCurrent()

        assertThat(retained.stateHolder.state.uploadingFileSlot).isNull()
        assertThat(retained.stateHolder.state.selectingFileSlot).isNull()
        val document = retained.stateHolder.createSubmission()?.requirements?.values?.single()?.documents?.single()
        assertThat(document?.uploadedFileIds).containsExactly("file_uploaded")
        assertThat(uploadedFile.exists()).isFalse()
    }

    @Test
    fun `explicit cancellation clears upload state and deletes temporary copy`() = runScenario {
        viewModel.selectFile(Uri.fromFile(source), 0)
        val uploadedFile = uploader.calls.awaitItem()
        assertThat(viewModel.stateHolder.state.uploadingFileSlot).isEqualTo(0)

        viewModel.cancelFileSelection()
        runCurrent()

        assertThat(viewModel.stateHolder.state.uploadingFileSlot).isNull()
        assertThat(viewModel.stateHolder.state.selectingFileSlot).isNull()
        assertThat(uploadedFile.exists()).isFalse()
        assertThat(source.exists()).isTrue()
        assertThat(viewModel.stateHolder.canSelectFile(0)).isTrue()
    }

    @Test
    fun `clearing ViewModel cancels upload and deletes temporary copy`() = runScenario {
        viewModel.selectFile(Uri.fromFile(source), 0)
        val uploadedFile = uploader.calls.awaitItem()

        controller.get().viewModelStore.clear()
        runCurrent()

        assertThat(viewModel.stateHolder.state.uploadingFileSlot).isNull()
        assertThat(uploadedFile.exists()).isFalse()
    }

    @Test
    fun `cancelled upload cannot clear a replacement selection`() = runScenario {
        viewModel.selectFile(Uri.fromFile(source), 0)
        val oldCopy = uploader.calls.awaitItem()

        viewModel.cancelFileSelection()
        viewModel.stateHolder.onFileSelectionStarted(0)
        viewModel.selectFile(Uri.fromFile(source), 0)
        val newCopy = uploader.calls.awaitItem()

        assertThat(oldCopy.exists()).isFalse()
        assertThat(newCopy.exists()).isTrue()
        assertThat(viewModel.stateHolder.state.uploadingFileSlot).isEqualTo(0)
        uploader.result.complete(Result.success("file_replacement"))
        runCurrent()
        val document = viewModel.stateHolder.createSubmission()?.requirements?.values?.single()?.documents?.single()
        assertThat(document?.uploadedFileIds).containsExactly("file_replacement")
        assertThat(newCopy.exists()).isFalse()
    }

    @Test
    fun `submission survives KycActivity recreation without a second request`() = runScenario {
        viewModel.stateHolder.onFileSelected(0, "document.pdf", "file_uploaded")
        composeRule.onNodeWithText("Submit").performClick()
        assertThat(handler.calls.awaitItem().requirements.values.single().documents.single().uploadedFileIds)
            .containsExactly("file_uploaded")
        assertThat(viewModel.stateHolder.state.submissionState).isEqualTo(KycSubmissionState.Submitting)

        controller.recreate()
        val retained = ViewModelProvider(controller.get())[KycViewModel::class.java]
        assertThat(retained).isSameInstanceAs(viewModel)
        retained.submit()
        runCurrent()
        handler.calls.expectNoEvents()

        handler.result.complete(Result.success(Unit))
        runCurrent()
        assertThat(retained.stateHolder.state.submissionState).isEqualTo(KycSubmissionState.Submitted)
        assertThat(retained.stateHolder.state.page).isEqualTo(KycCollectionPage.Submitted)
        assertThat(source.exists()).isTrue()
    }

    @Test
    fun `submission failure after recreation allows retry`() = runScenario {
        viewModel.stateHolder.onFileSelected(0, "document.pdf", "file_uploaded")
        viewModel.submit()
        handler.calls.awaitItem()
        controller.recreate()
        handler.result.complete(Result.failure(IllegalStateException("Test failure")))
        runCurrent()
        assertThat(viewModel.stateHolder.state.submissionState).isEqualTo(KycSubmissionState.Failed)
        assertThat(viewModel.stateHolder.state.canSubmit).isTrue()
        assertThat(source.exists()).isTrue()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val uploader = FakeUploader()
        val handler = FakeSubmissionHandler()
        KycSubmissionHandlerRegistry.setUploader("test_handler", uploader)
        KycSubmissionHandlerRegistry["test_handler"] = handler
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("kyc_requirements/proof_of_address_required.json")
        ).bufferedReader().use { it.readText() }
        val requirements = Json.decodeFromString<RetrieveKycRequirementsResponse>(fixture)
            .requirements.toKycRequirements()
        val intent = KycActivity.createIntent(
            RuntimeEnvironment.getApplication(),
            KycArgs(requirements, null, "test_handler"),
        )
        val controller = Robolectric.buildActivity(KycActivity::class.java, intent).setup()
        val source = File.createTempFile("kyc-test-", ".pdf", controller.get().cacheDir).apply {
            writeText("Test document")
        }
        val viewModel = ViewModelProvider(controller.get())[KycViewModel::class.java]
        viewModel.stateHolder.apply {
            onContinue()
            onDocumentSubtypeSelected(0, "utility_provider")
            onFileSelectionStarted(0)
        }
        try {
            Scenario(controller, viewModel, uploader, handler, source, { runCurrent() }).block()
            uploader.calls.ensureAllEventsConsumed()
            handler.calls.ensureAllEventsConsumed()
        } finally {
            controller.pause().stop().destroy()
            runCurrent()
            source.delete()
            KycSubmissionHandlerRegistry.remove("test_handler")
            Dispatchers.resetMain()
        }
    }

    private class Scenario(
        val controller: org.robolectric.android.controller.ActivityController<KycActivity>,
        val viewModel: KycViewModel,
        val uploader: FakeUploader,
        val handler: FakeSubmissionHandler,
        val source: File,
        val runCurrent: () -> Unit,
    )

    private class FakeSubmissionHandler : KycSubmissionHandler {
        val calls = Turbine<KycSubmission>()
        val result = CompletableDeferred<Result<Unit>>()

        override suspend fun submit(submission: KycSubmission): Result<Unit> {
            calls.add(submission)
            return result.await()
        }
    }

    private class FakeUploader : KycDocumentUploader {
        val calls = Turbine<File>()
        val result = CompletableDeferred<Result<String>>()

        override suspend fun upload(file: File): Result<String> {
            calls.add(file)
            return result.await()
        }
    }
}
