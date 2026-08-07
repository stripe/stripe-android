package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.models.VerificationPage.Companion.enable3DFaceCapture
import com.stripe.android.identity.networking.models.VerificationPage.Companion.has3DFaceCaptureExperiment
import com.stripe.android.identity.networking.models.VerificationPage.Companion.shouldSubmit3DFaceCaptureData
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class VerificationPageTest {

    @Test
    fun `has3DFaceCaptureExperiment is true when the experiment is present`() {
        val page = verificationPageWith(
            selfieCapture = null,
            experiments = listOf(
                experiment("some_other_experiment"),
                experiment(VerificationPage.IDPROD_3D_FACE_CAPTURE_MOBILE_EXPERIMENT)
            )
        )

        assertThat(page.has3DFaceCaptureExperiment()).isTrue()
    }

    @Test
    fun `has3DFaceCaptureExperiment is false when the experiment is absent`() {
        val page = verificationPageWith(
            selfieCapture = null,
            experiments = listOf(experiment("some_other_experiment"))
        )

        assertThat(page.has3DFaceCaptureExperiment()).isFalse()
    }

    @Test
    fun `enable3DFaceCapture is true when pose sequence includes a side pose`() {
        val page = verificationPageWith(
            selfieCapture = selfieCapturePageWithPoseSequence(listOf("front", "left")),
            experiments = emptyList()
        )

        assertThat(page.enable3DFaceCapture()).isTrue()
    }

    @Test
    fun `enable3DFaceCapture is true when only the experiment is present`() {
        val page = verificationPageWith(
            selfieCapture = selfieCapturePageWithPoseSequence(null),
            experiments = listOf(experiment(VerificationPage.IDPROD_3D_FACE_CAPTURE_MOBILE_EXPERIMENT))
        )

        assertThat(page.enable3DFaceCapture()).isTrue()
    }

    @Test
    fun `enable3DFaceCapture is false without a side pose or the experiment`() {
        val page = verificationPageWith(
            selfieCapture = selfieCapturePageWithPoseSequence(listOf("front")),
            experiments = emptyList()
        )

        assertThat(page.enable3DFaceCapture()).isFalse()
    }

    @Test
    fun `shouldSubmit3DFaceCaptureData is true when pose sequence includes a side pose`() {
        val page = verificationPageWith(
            selfieCapture = selfieCapturePageWithPoseSequence(listOf("right")),
            experiments = emptyList()
        )

        assertThat(page.shouldSubmit3DFaceCaptureData()).isTrue()
    }

    @Test
    fun `shouldSubmit3DFaceCaptureData is true when only the experiment is present`() {
        val page = verificationPageWith(
            selfieCapture = selfieCapturePageWithPoseSequence(null),
            experiments = listOf(experiment(VerificationPage.IDPROD_3D_FACE_CAPTURE_MOBILE_EXPERIMENT))
        )

        assertThat(page.shouldSubmit3DFaceCaptureData()).isTrue()
    }

    @Test
    fun `shouldSubmit3DFaceCaptureData is false without a side pose or the experiment`() {
        val page = verificationPageWith(
            selfieCapture = selfieCapturePageWithPoseSequence(emptyList()),
            experiments = listOf(experiment("some_other_experiment"))
        )

        assertThat(page.shouldSubmit3DFaceCaptureData()).isFalse()
    }

    private fun verificationPageWith(
        selfieCapture: VerificationPageStaticContentSelfieCapturePage?,
        experiments: List<VerificationPageStaticContentExperiment>
    ) = mock<VerificationPage> {
        on { this.selfieCapture } doReturn selfieCapture
        on { this.experiments } doReturn experiments
    }

    private fun selfieCapturePageWithPoseSequence(
        poseSequence: List<String>?
    ) = mock<VerificationPageStaticContentSelfieCapturePage> {
        on { this.poseSequence } doReturn poseSequence
    }

    private fun experiment(name: String) = VerificationPageStaticContentExperiment(
        experimentName = name,
        eventName = "event",
        eventMetadata = emptyMap()
    )
}
