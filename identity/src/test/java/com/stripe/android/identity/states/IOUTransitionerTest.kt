package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.states.IdentityScanState.ScanType
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IOUTransitionerTest {
    @Test
    fun `transitions to Unsatisfied when no match`() {
        val transitioner = IOUTransitioner()

        val resultState = transitioner.transition(mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_BACK)
        }, INITIAL_ID_FRONT_OUTPUT)


        assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        assertThat((resultState as IdentityScanState.Unsatisfied).reason).isEqualTo(
            "Type ${INITIAL_ID_FRONT_OUTPUT.category} doesn't match ${ScanType.ID_BACK}",
        )
        assertThat(resultState.type).isEqualTo(ScanType.ID_BACK)
    }

    @Test
    fun `transitions to Found with first matching result`() {
        val transitioner = IOUTransitioner()

        val mockFoundState = mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_FRONT)
        }

        val resultState = transitioner.transition(mockFoundState, INITIAL_ID_FRONT_OUTPUT)

        assertThat(resultState).isSameInstanceAs(mockFoundState)
    }

    @Test
    fun `transitions to Unsatisfied when iOUCheckPass failed`() {
        val transitioner = IOUTransitioner()

        val mockFoundState = mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_FRONT)
        }

        // initialize previousBoundingBox
        transitioner.transition(mockFoundState, INITIAL_ID_FRONT_OUTPUT)
        // send a low IOU result
        val resultState =
            transitioner.transition(
                mockFoundState,
                createAnalyzerOutputWithLowIOU(INITIAL_ID_FRONT_OUTPUT)
            )

        assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        assertThat((resultState as IdentityScanState.Unsatisfied).reason).isEqualTo(
            "IoU below threshold",
        )
        assertThat(resultState.type).isEqualTo(ScanType.ID_FRONT)
    }

    @Test
    fun `transitions to Found when moreResultsRequired and Satisfied when hitsRequired is met`() {
        val hitsRequired = 10
        val transitioner = IOUTransitioner(hitsRequired = 10)

        val mockFoundState = mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_FRONT)
        }

        var result = createAnalyzerOutputWithHighIOU(INITIAL_ID_FRONT_OUTPUT)
        for (i in 1..hitsRequired) {
            // send a high IOU result
            assertThat(
                transitioner.transition(
                    mockFoundState,
                    result
                )
            ).isSameInstanceAs(mockFoundState)
            result = createAnalyzerOutputWithHighIOU(result)
        }

        // send another high IOU result, transition to satisfied
        val resultState = transitioner.transition(
            mockFoundState,
            createAnalyzerOutputWithHighIOU(result)
        )


        assertThat(resultState).isInstanceOf(IdentityScanState.Satisfied::class.java)
        assertThat((resultState as IdentityScanState.Satisfied).type).isEqualTo(
            ScanType.ID_FRONT
        )
    }


    @Test
    fun `transitions to Found when moreResultsRequired and Unsatisfied when IOU check fails before hitsRequired is met`() {
        val hitsRequired = 10
        val transitioner = IOUTransitioner(hitsRequired = 10)

        val mockFoundState = mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_FRONT)
        }

        var result = createAnalyzerOutputWithHighIOU(INITIAL_ID_FRONT_OUTPUT)
        for (i in 1..(hitsRequired - 5)) {
            // send a high IOU result
            assertThat(
                transitioner.transition(
                    mockFoundState,
                    result
                )
            ).isSameInstanceAs(mockFoundState)
            result = createAnalyzerOutputWithHighIOU(result)
        }

        // send a low IOU result, break the hit streak, transitions to Unsatisfied
        val resultState = transitioner.transition(
            mockFoundState,
            createAnalyzerOutputWithLowIOU(result)
        )


        assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        assertThat((resultState as IdentityScanState.Unsatisfied).reason).isEqualTo(
            "IoU below threshold",
        )
        assertThat(resultState.type).isEqualTo(ScanType.ID_FRONT)
    }

    private fun createAnalyzerOutputWithHighIOU(previousAnalyzerOutput: AnalyzerOutput) =
        AnalyzerOutput(
            boundingBox = BoundingBox(
                previousAnalyzerOutput.boundingBox.left + 1,
                previousAnalyzerOutput.boundingBox.top + 1,
                previousAnalyzerOutput.boundingBox.width + 1,
                previousAnalyzerOutput.boundingBox.height + 1,
            ),
            previousAnalyzerOutput.category,
            previousAnalyzerOutput.resultScore,
            previousAnalyzerOutput.allScores
        )

    private fun createAnalyzerOutputWithLowIOU(previousAnalyzerOutput: AnalyzerOutput) =
        AnalyzerOutput(
            boundingBox = BoundingBox(
                previousAnalyzerOutput.boundingBox.left + 500f,
                previousAnalyzerOutput.boundingBox.top + 500f,
                previousAnalyzerOutput.boundingBox.width + 500f,
                previousAnalyzerOutput.boundingBox.height + 500f,
            ),
            previousAnalyzerOutput.category,
            previousAnalyzerOutput.resultScore,
            previousAnalyzerOutput.allScores
        )

    private companion object {
        val INITIAL_BOUNDING_BOX = BoundingBox(0f, 0f, 500f, 500f)
        val INITIAL_ID_FRONT_OUTPUT = AnalyzerOutput(
            INITIAL_BOUNDING_BOX,
            Category.ID_FRONT,
            0f,
            listOf()
        )
    }
}