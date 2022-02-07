package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stripe.android.identity.viewmodel.PassportScanViewModel
import com.stripe.android.identity.R

class PassportScanFragment : Fragment() {

    companion object {
        fun newInstance() = PassportScanFragment()
    }

    private lateinit var viewModel: PassportScanViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.passport_scan_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PassportScanViewModel::class.java)
        // TODO: Use the ViewModel
    }

}