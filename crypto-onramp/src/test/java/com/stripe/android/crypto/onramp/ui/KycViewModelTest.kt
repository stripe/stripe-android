package com.stripe.android.crypto.onramp.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.KycDocumentUploader
import com.stripe.android.crypto.onramp.model.RetrieveKycRequirementsResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class KycViewModelTest {
    @Test
    fun `upload survives activity recreation and completes in retained state`() = runScenario {
        viewModel.selectFile(Uri.fromFile(source), 0)
        val uploadedFile = uploader.calls.awaitItem()
        assertThat(viewModel.stateHolder.state.uploadingFileSlot).isEqualTo(0)

        controller.recreate()
        val retained = ViewModelProvider(controller.get(), factory)[KycViewModel::class.java]
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

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val source = File.createTempFile("kyc-test-", ".pdf", controller.get().cacheDir).apply {
            writeText("Test document")
        }
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("kyc_requirements/proof_of_address_required.json")
        ).bufferedReader().use { it.readText() }
        val stateHolder = KycStateHolder(
            Json.decodeFromString<RetrieveKycRequirementsResponse>(fixture).requirements.toKycRequirements()
        ).apply {
            onContinue()
            onDocumentSubtypeSelected(0, "utility_provider")
            onFileSelectionStarted(0)
        }
        val uploader = FakeUploader()
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return KycViewModel(controller.get().applicationContext, stateHolder, uploader) as T
            }
        }
        val viewModel = ViewModelProvider(controller.get(), factory)[KycViewModel::class.java]
        try {
            Scenario(controller, factory, viewModel, uploader, source, { runCurrent() }).block()
            uploader.calls.ensureAllEventsConsumed()
        } finally {
            controller.pause().stop().destroy()
            runCurrent()
            source.delete()
            Dispatchers.resetMain()
        }
    }

    private class Scenario(
        val controller: org.robolectric.android.controller.ActivityController<ComponentActivity>,
        val factory: ViewModelProvider.Factory,
        val viewModel: KycViewModel,
        val uploader: FakeUploader,
        val source: File,
        val runCurrent: () -> Unit,
    )

    private class FakeUploader : KycDocumentUploader {
        val calls = Turbine<File>()
        val result = CompletableDeferred<Result<String>>()

        override suspend fun upload(file: File): Result<String> {
            calls.add(file)
            return result.await()
        }
    }
}
