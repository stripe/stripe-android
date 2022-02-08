package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.databinding.IdScanFragmentBinding
import com.stripe.android.identity.viewmodel.IDScanViewModel

/**
 * TODO(ccen) connect the logic to initialize CameraAdapter and call IdentityActivity#ensureCameraPermission
 */
internal class IDScanFragment : Fragment() {

    companion object {
        fun newInstance() = IDScanFragment()
    }

    private lateinit var viewModel: IDScanViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return IdScanFragmentBinding.inflate(inflater, container, false).root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(IDScanViewModel::class.java)
        // TODO: Use the ViewModel
    }
}
