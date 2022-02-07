package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stripe.android.identity.viewmodel.DriverLicenseScanViewModel
import com.stripe.android.identity.R

class DriverLicenseScanFragment : Fragment() {

    companion object {
        fun newInstance() = DriverLicenseScanFragment()
    }

    private lateinit var viewModel: DriverLicenseScanViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.driver_license_scan_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DriverLicenseScanViewModel::class.java)
        // TODO: Use the ViewModel
    }

}