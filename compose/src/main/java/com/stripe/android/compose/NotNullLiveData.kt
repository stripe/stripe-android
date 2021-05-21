package com.stripe.android.compose

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

internal class NotNullMutableLiveData<T>(value: T) : MutableLiveData<T>(value) {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(): T = super.getValue() as T
    override fun setValue(value: T) = super.setValue(value)
    override fun postValue(value: T) = super.postValue(value)
}
