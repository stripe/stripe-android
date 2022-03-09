package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.PassportUploadFragmentBinding
import com.stripe.android.identity.viewmodel.PassportUploadViewModel

/**
 * Fragment to upload passport.
 */
internal class PassportUploadFragment(
    private val passportUploadViewModelFactory: ViewModelProvider.Factory
) : Fragment() {

    lateinit var binding: PassportUploadFragmentBinding

    private val passportUploadViewModel: PassportUploadViewModel by viewModels {
        passportUploadViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passportUploadViewModel.registerActivityResultCaller(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PassportUploadFragmentBinding.inflate(layoutInflater, container, false)

        binding.select.setOnClickListener {
            buildBottomSheetDialog().show()
        }

        binding.kontinue.setOnClickListener {
            findNavController().navigate(R.id.action_passportUploadFragment_to_confirmationFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passportUploadViewModel.uploaded.observe(viewLifecycleOwner) {
            showUploadDone()
        }
    }

    private fun buildBottomSheetDialog() = BottomSheetDialog(requireContext()).also { dialog ->
        dialog.setContentView(R.layout.get_local_image_fragment)
        dialog.setOnCancelListener {
        }
        dialog.findViewById<Button>(R.id.take_photo)?.setOnClickListener {
            dialog.dismiss()
            passportUploadViewModel.takePhoto(requireContext(), ::upload)
        }
        dialog.findViewById<Button>(R.id.choose_file)?.setOnClickListener {
            dialog.dismiss()
            passportUploadViewModel.chooseImage(::upload)
        }
    }

    private fun upload(frontUri: Uri) {
        showFrontUploading()
        passportUploadViewModel.uploadImage(frontUri, requireContext())
    }

    private fun showFrontUploading() {
        binding.select.visibility = View.GONE
        binding.progressCircular.visibility = View.VISIBLE
        binding.finishedCheckMark.visibility = View.GONE
    }

    private fun showUploadDone() {
        binding.select.visibility = View.GONE
        binding.progressCircular.visibility = View.GONE
        binding.finishedCheckMark.visibility = View.VISIBLE
        binding.kontinue.isEnabled = true
    }
}
