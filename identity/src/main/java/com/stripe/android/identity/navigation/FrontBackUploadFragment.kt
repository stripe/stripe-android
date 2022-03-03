package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.FrontBackUploadFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.FrontBackUploadViewModel

/**
 * Fragment to upload front and back of a document.
 *
 * TODO(ccen): check camera permission and enable camera only when permission is granted.
 */
internal abstract class FrontBackUploadFragment(
    private val frontBackUploadViewModelFactory: ViewModelProvider.Factory
) : Fragment() {

    @get:StringRes
    abstract val titleRes: Int

    @get:StringRes
    abstract val contextRes: Int

    @get:StringRes
    abstract val frontTextRes: Int

    @get:StringRes
    abstract val backTextRes: Int

    @get:StringRes
    abstract val frontCheckMarkContentDescription: Int

    @get:StringRes
    abstract val backCheckMarkContentDescription: Int

    @get:IdRes
    abstract val continueButtonNavigationId: Int

    abstract val frontScanType: IdentityScanState.ScanType

    abstract val backScanType: IdentityScanState.ScanType

    lateinit var binding: FrontBackUploadFragmentBinding

    private val frontBackUploadViewModel: FrontBackUploadViewModel by viewModels { frontBackUploadViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frontBackUploadViewModel.registerActivityResultCaller(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrontBackUploadFragmentBinding.inflate(layoutInflater, container, false)
        binding.titleText.text = getString(titleRes)
        binding.contentText.text = getString(contextRes)
        binding.labelFront.text = getString(frontTextRes)
        binding.labelBack.text = getString(backTextRes)
        binding.finishedCheckMarkFront.contentDescription = getString(frontCheckMarkContentDescription)
        binding.finishedCheckMarkBack.contentDescription = getString(backCheckMarkContentDescription)

        binding.selectBack.setOnClickListener {
            buildBottomSheetDialog(backScanType).show()
        }
        binding.selectFront.setOnClickListener {
            buildBottomSheetDialog(frontScanType).show()
        }
        binding.kontinue.setOnClickListener {
            findNavController().navigate(continueButtonNavigationId)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        frontBackUploadViewModel.frontUploaded.observe(viewLifecycleOwner) {
            showFrontDone()
        }

        frontBackUploadViewModel.backUploaded.observe(viewLifecycleOwner) {
            showBackDone()
        }

        frontBackUploadViewModel.uploadFinished.observe(viewLifecycleOwner) {
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
            if (scanType == frontScanType) {
                frontBackUploadViewModel.takePhotoFront(requireContext(), ::uploadFront)
            } else if (scanType == backScanType) {
                frontBackUploadViewModel.takePhotoBack(requireContext(), ::uploadBack)
            }
        }
        dialog.findViewById<Button>(R.id.choose_file)?.setOnClickListener {
            Log.d(TAG, "Choose a file")
            dialog.dismiss()
            if (scanType == frontScanType) {
                frontBackUploadViewModel.chooseImageFront(::uploadFront)
            } else if (scanType == backScanType) {
                frontBackUploadViewModel.chooseImageBack(::uploadBack)
            }
        }
    }

    private fun uploadFront(frontUri: Uri) {
        showFrontUploading()
        frontBackUploadViewModel.uploadImageFront(frontUri)
    }

    private fun uploadBack(backUri: Uri) {
        showBackUploading()
        frontBackUploadViewModel.uploadImageBack(backUri)
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
        val TAG: String = this::class.java.simpleName
    }
}
