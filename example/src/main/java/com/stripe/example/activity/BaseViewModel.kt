package com.stripe.example.activity

import android.app.Application
import android.content.res.Resources
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
    protected val resources: Resources = application.resources
    protected val workContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
    protected val backendApi: BackendApi = BackendApiFactory(application).create()

    override fun onCleared() {
        super.onCleared()
        workContext.cancelChildren()
    }
}
