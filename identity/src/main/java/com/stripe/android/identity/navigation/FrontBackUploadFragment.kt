package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.FrontBackUploadFragmentBinding
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CAMERA
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.FrontBackUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Fragment to upload front and back of a document.
 *
 * TODO(ccen): check camera permission and enable camera only when permission is granted.
 */
internal abstract class FrontBackUploadFragment(
    private val frontBackUploadViewModelFactory: ViewModelProvider.Factory,
    private val identityViewModelFactory: ViewModelProvider.Factory
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

    abstract val frontScanType: IdentityScanState.ScanType

    abstract val backScanType: IdentityScanState.ScanType

    lateinit var binding: FrontBackUploadFragmentBinding

    private var shouldShowCamera: Boolean = false

    private val frontBackUploadViewModel: FrontBackUploadViewModel by viewModels { frontBackUploadViewModelFactory }

    private val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frontBackUploadViewModel.registerActivityResultCaller(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireNotNull(arguments) {
            "Argument to FrontBackUploadFragment is null"
        }
        shouldShowCamera = args[ARG_SHOULD_SHOW_CAMERA] as Boolean

        binding = FrontBackUploadFragmentBinding.inflate(layoutInflater, container, false)
        binding.titleText.text = getString(titleRes)
        binding.contentText.text = getString(contextRes)
        binding.labelFront.text = getString(frontTextRes)
        binding.labelBack.text = getString(backTextRes)
        binding.finishedCheckMarkFront.contentDescription =
            getString(frontCheckMarkContentDescription)
        binding.finishedCheckMarkBack.contentDescription =
            getString(backCheckMarkContentDescription)

        binding.selectBack.setOnClickListener {
            buildBottomSheetDialog(backScanType).show()
        }
        binding.selectFront.setOnClickListener {
            buildBottomSheetDialog(frontScanType).show()
        }
        binding.kontinue.isEnabled = false
        binding.kontinue.setText(getString(R.string.kontinue))
        return binding.root
    }

    private fun IdentityScanState.ScanType.toType(): IdDocumentParam.Type =
        when (this) {
            IdentityScanState.ScanType.ID_FRONT -> IdDocumentParam.Type.IDCARD
            IdentityScanState.ScanType.ID_BACK -> IdDocumentParam.Type.IDCARD
            IdentityScanState.ScanType.PASSPORT -> IdDocumentParam.Type.PASSPORT
            IdentityScanState.ScanType.DL_FRONT -> IdDocumentParam.Type.DRIVINGLICENSE
            IdentityScanState.ScanType.DL_BACK -> IdDocumentParam.Type.DRIVINGLICENSE
            else -> {
                throw IllegalArgumentException("Unknown type: $this")
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        frontBackUploadViewModel.frontUploaded.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    showFrontDone()
                }
                Status.ERROR -> {
                    Log.e(TAG, "error uploading front image: $it")
                    navigateToDefaultErrorFragment()
                }
                Status.LOADING -> {
                    showFrontUploading()
                }
            }
        }

        frontBackUploadViewModel.backUploaded.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    showBackDone()
                }
                Status.ERROR -> {
                    Log.e(TAG, "error uploading front image: $it")
                    navigateToDefaultErrorFragment()
                }
                Status.LOADING -> {
                    showBackUploading()
                }
            }
        }

        frontBackUploadViewModel.uploadFinished.observe(viewLifecycleOwner) {
            binding.kontinue.isEnabled = true
            binding.kontinue.setOnClickListener {
                binding.kontinue.toggleToLoading()
                lifecycleScope.launch {
                    runCatching {
                        val front =
                            requireNotNull(frontBackUploadViewModel.frontUploaded.value?.data) {
                                "frontUploaded value is still null"
                            }
                        val back =
                            requireNotNull(frontBackUploadViewModel.backUploaded.value?.data) {
                                "backUploaded value is still null"
                            }
                        postVerificationPageDataAndMaybeSubmit(
                            identityViewModel = identityViewModel,
                            collectedDataParam = CollectedDataParam(
                                idDocument = IdDocumentParam(
                                    front = DocumentUploadParam(
                                        highResImage = requireNotNull(front.first.id) {
                                            "front uploaded file id is null"
                                        },
                                        uploadMethod = front.second
                                    ),
                                    back = DocumentUploadParam(
                                        highResImage = requireNotNull(back.first.id) {
                                            "back uploaded file id is null"
                                        },
                                        uploadMethod = back.second
                                    ),
                                    type = frontScanType.toType()
                                )
                            ),
                            clearDataParam = ClearDataParam.UPLOAD_TO_CONFIRM,
                            shouldNotSubmit = { false }
                        )
                    }.onFailure {
                        Log.d(TAG, "fail to submit uploaded files: $it")
                        navigateToDefaultErrorFragment()
                    }
                }
            }
        }
    }

    private fun buildBottomSheetDialog(
        scanType: IdentityScanState.ScanType
    ) = BottomSheetDialog(requireContext()).also { dialog ->
        dialog.setContentView(R.layout.get_local_image_fragment)
        dialog.setOnCancelListener {
            Log.d(TAG, "dialog cancelled")
        }
        if (shouldShowCamera) {
            dialog.findViewById<Button>(R.id.take_photo)?.setOnClickListener {
                Log.d(TAG, "Take photo")
                dialog.dismiss()
                if (scanType == frontScanType) {
                    frontBackUploadViewModel.takePhotoFront(requireContext()) {
                        uploadFront(it, DocumentUploadParam.UploadMethod.MANUALCAPTURE)
                    }
                } else if (scanType == backScanType) {
                    frontBackUploadViewModel.takePhotoBack(requireContext()) {
                        uploadBack(it, DocumentUploadParam.UploadMethod.MANUALCAPTURE)
                    }
                }
            }
        } else {
            requireNotNull(dialog.findViewById(R.id.take_photo)).visibility = View.GONE
        }

        dialog.findViewById<Button>(R.id.choose_file)?.setOnClickListener {
            Log.d(TAG, "Choose a file")
            dialog.dismiss()
            if (scanType == frontScanType) {
                frontBackUploadViewModel.chooseImageFront {
                    uploadFront(it, DocumentUploadParam.UploadMethod.FILEUPLOAD)
                }
            } else if (scanType == backScanType) {
                frontBackUploadViewModel.chooseImageBack() {
                    uploadBack(it, DocumentUploadParam.UploadMethod.FILEUPLOAD)
                }
            }
        }
    }

    private fun observeForDocumentCaptureModels(
        onSuccess: (VerificationPageStaticContentDocumentCapturePage) -> Unit
    ) {
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = {
                onSuccess(it.documentCapture)
            },
            onFailure = {
                navigateToDefaultErrorFragment()
            }
        )
    }

    private fun uploadFront(frontUri: Uri, uploadMethod: DocumentUploadParam.UploadMethod) {
        observeForDocumentCaptureModels { documentCaptureModels ->
            frontBackUploadViewModel.uploadImageFront(
                frontUri,
                requireContext(),
                documentCaptureModels,
                uploadMethod
            )
        }
    }

    private fun uploadBack(backUri: Uri, uploadMethod: DocumentUploadParam.UploadMethod) {
        observeForDocumentCaptureModels { documentCaptureModels ->
            frontBackUploadViewModel.uploadImageBack(
                backUri,
                requireContext(),
                documentCaptureModels,
                uploadMethod
            )
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

    companion object {
        val TAG: String = this::class.java.simpleName
    }
}
