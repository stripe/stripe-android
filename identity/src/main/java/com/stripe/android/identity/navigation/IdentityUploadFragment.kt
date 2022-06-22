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
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavArgument
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.databinding.IdentityUploadFragmentBinding
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.ARG_IS_NAVIGATED_UP_TO
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CHOOSE_PHOTO
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_TAKE_PHOTO
import com.stripe.android.identity.utils.isNavigatedUpTo
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.collectLatest
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

    @get:IdRes
    abstract val fragmentId: Int

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

    /**
     * Check how this fragment is navigated from and reset uploaded state when needed.
     *
     * The upload state should only be kept when scanning fails and user is redirected to this
     * fragment through CouldNotCaptureFragment, in which case it's possible that the front is
     * already scanned and uploaded, and this fragment should correctly updating the front uploaded UI.
     *
     * For all other cases the upload state should be reset in order to reupload both front and back.
     */
    private fun maybeResetUploadedState() {
        val isPreviousEntryCouldNotCapture =
            findNavController().previousBackStackEntry?.destination?.id == R.id.couldNotCaptureFragment

        if (findNavController().isNavigatedUpTo() || !isPreviousEntryCouldNotCapture) {
            identityViewModel.resetDocumentUploadedState()
        }

        // flip the argument to indicate it's no longer navigated through back pressed
        findNavController().currentDestination?.addArgument(
            ARG_IS_NAVIGATED_UP_TO,
            NavArgument.Builder()
                .setDefaultValue(false)
                .build()
        )
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        maybeResetUploadedState()
        collectUploadedStateAndUpdateUI()

        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                scanType = frontScanType,
                screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_FILE_UPLOAD
            )
        )
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

    private fun IdentityScanState.ScanType.toType(): CollectedDataParam.Type =
        when (this) {
            IdentityScanState.ScanType.ID_FRONT -> CollectedDataParam.Type.IDCARD
            IdentityScanState.ScanType.ID_BACK -> CollectedDataParam.Type.IDCARD
            IdentityScanState.ScanType.PASSPORT -> CollectedDataParam.Type.PASSPORT
            IdentityScanState.ScanType.DL_FRONT -> CollectedDataParam.Type.DRIVINGLICENSE
            IdentityScanState.ScanType.DL_BACK -> CollectedDataParam.Type.DRIVINGLICENSE
            else -> {
                throw IllegalArgumentException("Unknown type: $this")
            }
        }

    private fun collectUploadedStateAndUpdateUI() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                identityViewModel.documentUploadState.collectLatest { latestState ->
                    if (latestState.hasError()) {
                        Log.e(TAG, "Fail to upload files: ${latestState.getError()}")
                        navigateToDefaultErrorFragment()
                    } else {
                        if (latestState.isFrontHighResUploaded()) {
                            showFrontDone(latestState)
                        }
                        if (latestState.isBackHighResUploaded()) {
                            showBackDone()
                        }
                        if (latestState.isHighResUploaded()) {
                            showBothDone(latestState)
                        }
                    }
                }
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
        if (isFront) {
            showFrontUploading()
        } else {
            showBackUploading()
        }
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

    protected open fun showFrontDone(latestState: DocumentUploadState) {
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

    private fun showBothDone(latestState: DocumentUploadState) {
        binding.kontinue.isEnabled = true
        binding.kontinue.setOnClickListener {
            binding.kontinue.toggleToLoading()
            runCatching {
                val front =
                    requireNotNull(latestState.frontHighResResult.data)
                val back =
                    requireNotNull(latestState.backHighResResult.data)
                trySubmit(
                    CollectedDataParam(
                        idDocumentFront = DocumentUploadParam(
                            highResImage = requireNotNull(front.uploadedStripeFile.id) {
                                "front uploaded file id is null"
                            },
                            uploadMethod = requireNotNull(front.uploadMethod)
                        ),
                        idDocumentBack = DocumentUploadParam(
                            highResImage = requireNotNull(back.uploadedStripeFile.id) {
                                "back uploaded file id is null"
                            },
                            uploadMethod = requireNotNull(back.uploadMethod)
                        ),
                        idDocumentType = frontScanType.toType()
                    )
                )
            }.onFailure {
                Log.d(TAG, "fail to submit uploaded files: $it")
                navigateToDefaultErrorFragment()
            }
        }
    }

    protected fun trySubmit(collectedDataParam: CollectedDataParam) {
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = { verificationPage ->
                lifecycleScope.launch {
                    if (verificationPage.requireSelfie()) {
                        postVerificationPageDataAndMaybeSubmit(
                            identityViewModel = identityViewModel,
                            collectedDataParam = collectedDataParam,
                            clearDataParam = ClearDataParam.UPLOAD_TO_SELFIE,
                            fromFragment = fragmentId
                        ) {
                            findNavController().navigate(R.id.action_global_selfieFragment)
                        }
                    } else {
                        postVerificationPageDataAndMaybeSubmit(
                            identityViewModel = identityViewModel,
                            collectedDataParam = collectedDataParam,
                            clearDataParam = ClearDataParam.UPLOAD_TO_CONFIRM,
                            fromFragment = fragmentId
                        )
                    }
                }
            },
            onFailure = {
                Log.e(TAG, "Fail to observeForVerificationPage: $it")
                navigateToDefaultErrorFragment()
            }
        )
    }

    companion object {
        val TAG: String = IdentityUploadFragment::class.java.simpleName
    }
}
