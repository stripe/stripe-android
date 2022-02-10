package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.viewmodel.CameraPermissionDeniedViewModel

class CameraPermissionDeniedFragment : Fragment() {

    companion object {
        fun newInstance() = CameraPermissionDeniedFragment()
    }

    private lateinit var viewModel: CameraPermissionDeniedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_permission_denied_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraPermissionDeniedViewModel::class.java)
        // TODO: Use the ViewModel
    }
}
