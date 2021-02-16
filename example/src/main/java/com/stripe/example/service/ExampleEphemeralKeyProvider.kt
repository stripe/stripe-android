package com.stripe.example.service

import android.content.Context
import androidx.annotation.Size
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import com.stripe.example.Settings
import com.stripe.example.module.BackendApiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * An implementation of [EphemeralKeyProvider] that can be used to generate
 * ephemeral keys on the backend.
 */
internal class ExampleEphemeralKeyProvider(
    backendUrl: String,
    private val workContext: CoroutineContext
) : EphemeralKeyProvider {
    constructor(context: Context) : this(
        Settings(context).backendUrl,
        Dispatchers.IO
    )

    private val backendApi = BackendApiFactory(backendUrl).create()

    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        CoroutineScope(workContext).launch {
            val response =
                kotlin.runCatching {
                    backendApi
                        .createEphemeralKey(hashMapOf("api_version" to apiVersion))
                        .string()
                }

            withContext(Dispatchers.Main) {
                response.fold(
                    onSuccess = {
                        keyUpdateListener.onKeyUpdate(it)
                    },
                    onFailure = {
                        keyUpdateListener
                            .onKeyUpdateFailure(0, it.message.orEmpty())
                    }
                )
            }
        }
    }
}
