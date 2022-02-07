package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.viewmodel.CameraErrorViewModel

class CameraErrorFragment : Fragment() {

    companion object {
        fun newInstance() = CameraErrorFragment()
    }

    private lateinit var viewModel: CameraErrorViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_error_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraErrorViewModel::class.java)
        // TODO: Use the ViewModel
    }
}
