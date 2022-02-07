package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stripe.android.identity.viewmodel.PassportUploadViewModel
import com.stripe.android.identity.R

class PassportUploadFragment : Fragment() {

    companion object {
        fun newInstance() = PassportUploadFragment()
    }

    private lateinit var viewModel: PassportUploadViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.passport_upload_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PassportUploadViewModel::class.java)
        // TODO: Use the ViewModel
    }

}