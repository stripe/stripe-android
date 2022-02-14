package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.databinding.CameraPermissionDeniedFragmentBinding
import com.stripe.android.identity.states.ScanState
import com.stripe.android.identity.viewmodel.CameraPermissionDeniedViewModel

/**
 * Fragment to show user denies camera permission.
 */
internal class CameraPermissionDeniedFragment : Fragment() {
    private lateinit var viewModel: CameraPermissionDeniedViewModel

    private lateinit var scanType: ScanState.ScanType

    private lateinit var binding: CameraPermissionDeniedFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireNotNull(arguments)

        scanType = args[ARG_SCAN_TYPE] as ScanState.ScanType
        binding = CameraPermissionDeniedFragmentBinding.inflate(inflater, container, false)
        binding.info.text = "Camera permission denied, scanType is ${scanType.name}"

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraPermissionDeniedViewModel::class.java)
        // TODO: Use the ViewModel
    }

    companion object {
        const val ARG_SCAN_TYPE = "scanType"
    }
}
