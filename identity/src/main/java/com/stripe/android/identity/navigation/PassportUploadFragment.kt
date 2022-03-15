package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stripe.android.core.model.InternalStripeFile
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.PassportUploadFragmentBinding
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CAMERA
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.PassportUploadViewModel
import kotlinx.coroutines.launch

/**
 * Fragment to upload passport.
 */
internal class PassportUploadFragment(
    private val passportUploadViewModelFactory: ViewModelProvider.Factory,
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {

    lateinit var binding: PassportUploadFragmentBinding

    private val passportUploadViewModel: PassportUploadViewModel by viewModels {
        passportUploadViewModelFactory
    }

    private val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    private var shouldShowCamera: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passportUploadViewModel.registerActivityResultCaller(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireNotNull(arguments) {
            "Argument to PassportUploadFragment is null"
        }
        shouldShowCamera = args[ARG_SHOULD_SHOW_CAMERA] as Boolean

        binding = PassportUploadFragmentBinding.inflate(layoutInflater, container, false)

        binding.select.setOnClickListener {
            buildBottomSheetDialog().show()
        }

        binding.kontinue.isEnabled = false
        binding.kontinue.setText(getString(R.string.kontinue))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passportUploadViewModel.uploaded.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    showUploadDone(it.data)
                }
                Status.ERROR -> {
                    Log.e(TAG, "error uploading passport image: $it")
                    navigateToDefaultErrorFragment()
                }
                Status.LOADING -> {
                    showUploading()
                }
            }
        }
    }

    private fun buildBottomSheetDialog() = BottomSheetDialog(requireContext()).also { dialog ->
        dialog.setContentView(R.layout.get_local_image_fragment)
        dialog.setOnCancelListener {
            Log.d(TAG, "dialog cancelled")
        }
        if (shouldShowCamera) {
            dialog.findViewById<Button>(R.id.take_photo)?.setOnClickListener {
                dialog.dismiss()
                passportUploadViewModel.takePhoto(requireContext()) {
                    upload(it, DocumentUploadParam.UploadMethod.MANUALCAPTURE)
                }
            }
        } else {
            requireNotNull(dialog.findViewById(R.id.take_photo)).visibility = View.GONE
        }
        dialog.findViewById<Button>(R.id.choose_file)?.setOnClickListener {
            dialog.dismiss()
            passportUploadViewModel.chooseImage {
                upload(it, DocumentUploadParam.UploadMethod.FILEUPLOAD)
            }
        }
    }

    private fun upload(passport: Uri, uploadMethod: DocumentUploadParam.UploadMethod) {
        identityViewModel.observeForVerificationPage(
            this,
            onSuccess = { verificationPage ->
                passportUploadViewModel.uploadImage(
                    passport,
                    requireContext(),
                    verificationPage.documentCapture,
                    uploadMethod
                )
            },
            onFailure = {
                navigateToDefaultErrorFragment()
            }
        )
    }

    private fun showUploading() {
        binding.select.visibility = View.GONE
        binding.progressCircular.visibility = View.VISIBLE
        binding.finishedCheckMark.visibility = View.GONE
    }

    private fun showUploadDone(passportImage: Pair<InternalStripeFile, DocumentUploadParam.UploadMethod>?) {
        binding.select.visibility = View.GONE
        binding.progressCircular.visibility = View.GONE
        binding.finishedCheckMark.visibility = View.VISIBLE
        binding.kontinue.isEnabled = true
        binding.kontinue.setOnClickListener {
            binding.kontinue.toggleToLoading()
            lifecycleScope.launch {
                runCatching {
                    requireNotNull(passportImage)
                    postVerificationPageDataAndMaybeSubmit(
                        identityViewModel = identityViewModel,
                        collectedDataParam = CollectedDataParam(
                            idDocument = IdDocumentParam(
                                front = DocumentUploadParam(
                                    highResImage = requireNotNull(passportImage.first.id) {
                                        "front uploaded file id is null"
                                    },
                                    uploadMethod = passportImage.second
                                ),
                                type = IdDocumentParam.Type.PASSPORT
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

    private companion object {
        val TAG: String = PassportUploadFragment::class.java.simpleName
    }
}
