package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.common.nfcscan.injection.DaggerNfcScanningViewModelComponent
import com.stripe.android.core.utils.requireApplication
import javax.inject.Inject

internal class NfcScanningViewModel @Inject constructor(
    private val hardwareDelegate: NfcHardwareDelegate
) : ViewModel() {
    fun register(activity: AppCompatActivity) {
        hardwareDelegate.read(activity)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer<NfcScanningViewModel> {
                    DaggerNfcScanningViewModelComponent.factory()
                        .create(requireApplication())
                        .viewModel
                }
            }
        }
    }
}
