package com.stripe.android.identity.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status

/**
 * A [MediatorLiveData] that wraps two [LiveData] with [Resource].
 *
 * Posts [Status.LOADING] when the two LiveData are still loading.
 * Posts [Status.ERROR] when either of the LiveData returns error.
 * Posts [Status.SUCCESS] with a [Pair] holding both when both [LiveData] returns a value.
 */
internal class PairMediatorLiveData<DataType>(
    firstLiveData: LiveData<Resource<DataType>>,
    secondLiveData: LiveData<Resource<DataType>>
) : MediatorLiveData<Resource<Pair<DataType, DataType>>>() {
    private var firstValue: DataType? = null
    private var secondValue: DataType? = null

    init {
        postValue(Resource.loading())
        addSource(firstLiveData) {
            when (it.status) {
                Status.SUCCESS -> {
                    firstValue = it.data
                    postValueWhenBothUploaded()
                }
                Status.ERROR -> {
                    postValue(Resource.error("$firstLiveData posts ERROR."))
                }
                Status.LOADING -> {} // no-op
                Status.IDLE -> {} // no-op
            }
        }
        addSource(secondLiveData) {
            when (it.status) {
                Status.SUCCESS -> {
                    secondValue = it.data
                    postValueWhenBothUploaded()
                }
                Status.ERROR -> {
                    postValue(Resource.error("$secondLiveData posts ERROR."))
                }
                Status.LOADING -> {} // no-op
                Status.IDLE -> {} // no-op
            }
        }
    }

    private fun postValueWhenBothUploaded() {
        firstValue?.let { firstValue ->
            secondValue?.let { secondValue ->
                postValue(Resource.success(Pair(firstValue, secondValue)))
            }
        }
    }
}
