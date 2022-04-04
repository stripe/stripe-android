package com.stripe.android.identity.viewmodel

import android.net.Uri
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

    fun loadUriIntoImageView(
        uri: Uri,
        imageView: ImageView
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            imageView.setImageURI(
                when (uri.scheme) {
                    "https" -> {
                        identityIO.createUriForFile(identityRepository.downloadFile(uri.toString()))
                    }
                    "http" -> {
                        throw IllegalArgumentException("Only https is supported for remote Uri")
                    }
                    else -> {
                        uri
                    }
                }
            )
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
}