package com.stripe.android.viewmodel.common

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

internal class NotNullMutableLiveData<T>(value: T) : MutableLiveData<T>(value) {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(): T = super.getValue() as T
    override fun setValue(value: T) = super.setValue(value)
    override fun postValue(value: T) = super.postValue(value)
}

internal class NotNullMediatorLiveData<T>(private val initValue: T) : MediatorLiveData<T>() {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(): T = super.getValue().let { it as T } ?: initValue
    override fun setValue(value: T) = super.setValue(value)
    override fun postValue(value: T) = super.postValue(value)

    init {
        postValue(initValue)
    }
}