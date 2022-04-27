package com.stripe.android.identity.viewmodel

import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.IdentityIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class ConsentFragmentViewModel(
    private val identityIO: IdentityIO,
    private val identityRepository: IdentityRepository
) : ViewModel() {

    private fun Uri.isRemote() = this.scheme == "https" || this.scheme == "http"

    fun loadUriIntoImageView(
        logoUri: Uri,
        imageView: ImageView
    ) {
        viewModelScope.launch(Dispatchers.IO) {
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

    internal class ConsentFragmentViewModelFactory(
        private val identityIO: IdentityIO,
        private val identityRepository: IdentityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConsentFragmentViewModel(
                identityIO,
                identityRepository
            ) as T
        }
    }

    private companion object {
        private val TAG: String = ConsentFragmentViewModel::class.java.simpleName
    }
}
