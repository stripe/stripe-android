package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.IdentityUploadFragmentBinding
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CHOOSE_PHOTO
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_TAKE_PHOTO
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Fragment to upload front and back of a document.
 *
 */
internal abstract class IdentityUploadFragment(
    private val identityUploadViewModelFactory: ViewModelProvider.Factory,
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {

    @get:StringRes
    abstract val titleRes: Int

    @get:StringRes
    abstract val contextRes: Int

    @get:StringRes
    abstract val frontTextRes: Int

    @get:StringRes
    open var backTextRes: Int? = null

    @get:StringRes
    abstract val frontCheckMarkContentDescription: Int

    @get:StringRes
    open var backCheckMarkContentDescription: Int? = null

    abstract val frontScanType: IdentityScanState.ScanType

    open var backScanType: IdentityScanState.ScanType? = null

    lateinit var binding: IdentityUploadFragmentBinding

    private var shouldShowTakePhoto: Boolean = false

    private var shouldShowChoosePhoto: Boolean = false

    private val identityUploadViewModel: IdentityUploadViewModel by viewModels { identityUploadViewModelFactory }

    protected val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        identityUploadViewModel.registerActivityResultCaller(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireNotNull(arguments) {
            "Argument to FrontBackUploadFragment is null"
        }
        shouldShowTakePhoto = args[ARG_SHOULD_SHOW_TAKE_PHOTO] as Boolean
        shouldShowChoosePhoto = args[ARG_SHOULD_SHOW_CHOOSE_PHOTO] as Boolean

        binding = IdentityUploadFragmentBinding.inflate(layoutInflater, container, false)
        binding.titleText.text = getString(titleRes)
        binding.contentText.text = getString(contextRes)

        binding.labelFront.text = getString(frontTextRes)
        binding.finishedCheckMarkFront.contentDescription =
            getString(frontCheckMarkContentDescription)
        binding.selectFront.setOnClickListener {
            buildDialog(frontScanType).show()
        }

        checkBackFields(
            nonNullBlock = { backText, backCheckMarkContentDescriptionText, backScanType ->
                binding.labelBack.text = backText
                binding.finishedCheckMarkBack.contentDescription =
                    backCheckMarkContentDescriptionText
                binding.selectBack.setOnClickListener {
                    buildDialog(backScanType).show()
                }
            },
            nullBlock = {
                binding.separator.visibility = View.GONE
                binding.backUpload.visibility = View.GONE
            }
        )

        binding.kontinue.isEnabled = false
        binding.kontinue.setText(getString(R.string.kontinue))
        return binding.root
    }

    private fun checkBackFields(
        nonNullBlock: (String, String, IdentityScanState.ScanType) -> Unit,
        nullBlock: () -> Unit
    ) {
        runCatching {
            nonNullBlock(
                getString(requireNotNull(backTextRes)),
                getString(requireNotNull(backCheckMarkContentDescription)),
                requireNotNull(backScanType)
            )
        }.onFailure {
            nullBlock()
        }
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

    protected fun observeForFrontUploaded() {
        identityViewModel.frontHighResUploaded.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    showFrontDone(it.data)
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
    }

    protected fun observeForBackUploaded() {
        identityViewModel.backHighResUploaded.observe(viewLifecycleOwner) {
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
    }

    protected fun enableKontinueWhenBothUploaded() {
        identityViewModel.highResUploaded.observe(viewLifecycleOwner) { frontBackPair ->
            when (frontBackPair.status) {
                Status.SUCCESS -> {
                    binding.kontinue.isEnabled = true
                    binding.kontinue.setOnClickListener {
                        binding.kontinue.toggleToLoading()
                        lifecycleScope.launch {
                            runCatching {
                                requireNotNull(frontBackPair.data)
                                val front = frontBackPair.data.first
                                val back = frontBackPair.data.second

                                postVerificationPageDataAndMaybeSubmit(
                                    identityViewModel = identityViewModel,
                                    collectedDataParam = CollectedDataParam(
                                        idDocument = IdDocumentParam(
                                            front = DocumentUploadParam(
                                                highResImage = requireNotNull(front.uploadedStripeFile.id) {
                                                    "front uploaded file id is null"
                                                },
                                                uploadMethod = front.uploadMethod
                                            ),
                                            back = DocumentUploadParam(
                                                highResImage = requireNotNull(back.uploadedStripeFile.id) {
                                                    "back uploaded file id is null"
                                                },
                                                uploadMethod = back.uploadMethod
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
                Status.ERROR -> {
                    Log.d(TAG, "highResUploaded posts an Error: $frontBackPair")
                    navigateToDefaultErrorFragment()
                }
                Status.LOADING -> {} // no-op
            }
        }
    }

    private fun getTitleFromScanType(scanType: IdentityScanState.ScanType): String {
        return when (scanType) {
            IdentityScanState.ScanType.ID_FRONT -> {
                getString(R.string.upload_dialog_title_id_front)
            }
            IdentityScanState.ScanType.ID_BACK -> {
                getString(R.string.upload_dialog_title_id_back)
            }
            IdentityScanState.ScanType.DL_FRONT -> {
                getString(R.string.upload_dialog_title_dl_front)
            }
            IdentityScanState.ScanType.DL_BACK -> {
                getString(R.string.upload_dialog_title_dl_back)
            }
            IdentityScanState.ScanType.PASSPORT -> {
                getString(R.string.upload_dialog_title_passport)
            }
            else -> {
                throw java.lang.IllegalArgumentException("invalid scan type: $scanType")
            }
        }
    }

    private fun buildDialog(
        scanType: IdentityScanState.ScanType
    ) = AppCompatDialog(requireContext()).also { dialog ->
        dialog.setContentView(R.layout.get_local_image_dialog)
        dialog.setTitle(getTitleFromScanType(scanType))
        if (shouldShowTakePhoto) {
            dialog.findViewById<Button>(R.id.take_photo)?.setOnClickListener {
                if (scanType == frontScanType) {
                    identityUploadViewModel.takePhotoFront(requireContext()) {
                        uploadResult(
                            uri = it,
                            uploadMethod = DocumentUploadParam.UploadMethod.MANUALCAPTURE,
                            isFront = true
                        )
                    }
                } else if (scanType == backScanType) {
                    identityUploadViewModel.takePhotoBack(requireContext()) {
                        uploadResult(
                            uri = it,
                            uploadMethod = DocumentUploadParam.UploadMethod.MANUALCAPTURE,
                            isFront = false
                        )
                    }
                }
                dialog.dismiss()
            }
        } else {
            requireNotNull(dialog.findViewById(R.id.take_photo)).visibility = View.GONE
        }

        if (shouldShowChoosePhoto) {
            dialog.findViewById<Button>(R.id.choose_file)?.setOnClickListener {
                if (scanType == frontScanType) {
                    identityUploadViewModel.chooseImageFront {
                        uploadResult(
                            uri = it,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD,
                            isFront = true
                        )
                    }
                } else if (scanType == backScanType) {
                    identityUploadViewModel.chooseImageBack {
                        uploadResult(
                            uri = it,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD,
                            isFront = false
                        )
                    }
                }
                dialog.dismiss()
            }
        } else {
            requireNotNull(dialog.findViewById(R.id.choose_file)).visibility = View.GONE
        }
    }

    private fun observeForDocCapturePage(
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

    private fun uploadResult(
        uri: Uri,
        uploadMethod: DocumentUploadParam.UploadMethod,
        isFront: Boolean
    ) {
        observeForDocCapturePage { docCapturePage ->
            identityViewModel.uploadManualResult(
                uri = uri,
                isFront = isFront,
                docCapturePage = docCapturePage,
                uploadMethod = uploadMethod
            )
        }
    }

    private fun showFrontUploading() {
        binding.selectFront.visibility = View.GONE
        binding.progressCircularFront.visibility = View.VISIBLE
        binding.finishedCheckMarkFront.visibility = View.GONE
    }

    protected open fun showFrontDone(frontResult: IdentityViewModel.UploadedResult?) {
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
        val TAG: String = IdentityUploadFragment::class.java.simpleName
    }
}
