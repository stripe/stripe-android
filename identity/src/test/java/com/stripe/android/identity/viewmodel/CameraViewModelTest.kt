package com.stripe.android.identity.viewmodel

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.states.ScanState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraViewModelTest {
    @Test
    fun `identityScanFlow is not null after initialization`() {
        val viewModel = CameraViewModel()
        viewModel.initializeScanFlow(ScanState.ScanType.ID_FRONT)

        assertThat(viewModel.identityScanFlow).isNotNull()
    }
}
