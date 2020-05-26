package com.stripe.example.activity

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.stripe.example.module.BackendApiFactory
import com.stripe.example.service.BackendApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel(
    application: Application
) : AndroidViewModel(application) {
    protected val context: Context = application.applicationContext
    protected val workContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
    protected val backendApi: BackendApi = BackendApiFactory(application.applicationContext).create()

    override fun onCleared() {
        super.onCleared()
        workContext.cancelChildren()
    }
}
