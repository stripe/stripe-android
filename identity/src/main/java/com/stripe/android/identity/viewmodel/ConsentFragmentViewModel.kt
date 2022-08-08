package com.stripe.android.identity.viewmodel

import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.injection.IOContext
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.IdentityIO
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class ConsentFragmentViewModel(
    private val identityIO: IdentityIO,
    private val identityRepository: IdentityRepository,
    @IOContext private val workContext: CoroutineContext
) : ViewModel() {

    private fun Uri.isRemote() = this.scheme == "https" || this.scheme == "http"

    fun loadUriIntoImageView(
        logoUri: Uri,
        imageView: ImageView
    ) {
        viewModelScope.launch(workContext) {
            runCatching {
                imageView.setImageURI(
                    if (logoUri.isRemote()) {
                        identityIO.createUriForFile(
                            identityRepository.downloadFile(logoUri.toString())
                        )
                    } else {
                        logoUri
                    }
                )
            }.onFailure {
                Log.e(TAG, "Failed to set logoUri at $logoUri: $it")
            }
        }
    }

    internal class ConsentFragmentViewModelFactory @Inject constructor(
        private val identityIO: IdentityIO,
        private val identityRepository: IdentityRepository,
        @IOContext private val workContext: CoroutineContext
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConsentFragmentViewModel(
                identityIO,
                identityRepository,
                workContext
            ) as T
        }
    }

    private companion object {
        private val TAG: String = ConsentFragmentViewModel::class.java.simpleName
    }
}
