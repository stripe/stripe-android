package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
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
import com.stripe.android.identity.databinding.IdUploadFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IDUploadViewModel

/**
 * Fragment to upload ID.
 *
 * TODO(ccen): check camera permission and enable camera only when permission is granted.
 */
internal class IDUploadFragment(
    private val idUploadViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    private val idUploadViewModel: IDUploadViewModel by viewModels { idUploadViewModelFactory }

    lateinit var binding: IdUploadFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idUploadViewModel.registerActivityResultCaller(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IdUploadFragmentBinding.inflate(layoutInflater, container, false)
        binding.selectBack.setOnClickListener {
            buildBottomSheetDialog(IdentityScanState.ScanType.ID_BACK).show()
        }
        binding.selectFront.setOnClickListener {
            buildBottomSheetDialog(IdentityScanState.ScanType.ID_FRONT).show()
        }
        binding.kontinue.setOnClickListener {
            findNavController().navigate(R.id.action_IDUploadFragment_to_confirmationFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        idUploadViewModel.frontPicked.observe(viewLifecycleOwner) { frontUri ->
            showFrontUploading()
            idUploadViewModel.uploadImage(frontUri, IdentityScanState.ScanType.ID_FRONT)
        }

        idUploadViewModel.backPicked.observe(viewLifecycleOwner) { backUri ->
            showBackUploading()
            idUploadViewModel.uploadImage(backUri, IdentityScanState.ScanType.ID_BACK)
        }

        idUploadViewModel.frontUploaded.observe(viewLifecycleOwner) {
            showFrontDone()
        }

        idUploadViewModel.backUploaded.observe(viewLifecycleOwner) {
            showBackDone()
        }

        idUploadViewModel.uploadFinished.observe(viewLifecycleOwner) {
            enableContinue()
        }
    }

    private fun buildBottomSheetDialog(
        scanType: IdentityScanState.ScanType
    ) = BottomSheetDialog(requireContext()).also { dialog ->
        dialog.setContentView(R.layout.get_local_image_fragment)
        dialog.setOnCancelListener {
            Log.d(TAG, "dialog cancelled")
        }
        dialog.findViewById<Button>(R.id.take_photo)?.setOnClickListener {
            Log.d(TAG, "Take photo")
            dialog.dismiss()
            idUploadViewModel.takePhoto(scanType, requireContext())
        }
        dialog.findViewById<Button>(R.id.choose_file)?.setOnClickListener {
            Log.d(TAG, "Choose a file")
            dialog.dismiss()
            idUploadViewModel.chooseImage(scanType)
        }
    }

    private fun showFrontUploading() {
        binding.selectFront.visibility = View.GONE
        binding.progressCircularFront.visibility = View.VISIBLE
        binding.finishedCheckMarkFront.visibility = View.GONE
    }

    private fun showFrontDone() {
        binding.selectFront.visibility = View.GONE
        binding.progressCircularFront.visibility = View.GONE
        binding.finishedCheckMarkFront.visibility = View.VISIBLE
    }

    private fun showBackUploading() {
        binding.selectBack.visibility = View.GONE
        binding.progressCircularBack.visibility = View.VISIBLE
        binding.finishedCheckMarkBack.visibility = View.GONE
    }

    private fun showBackDone() {
        binding.selectBack.visibility = View.GONE
        binding.progressCircularBack.visibility = View.GONE
        binding.finishedCheckMarkBack.visibility = View.VISIBLE
    }

    private fun enableContinue() {
        binding.kontinue.isEnabled = true
    }

    companion object {
        val TAG: String = IDUploadFragment::class.java.simpleName
    }
}
