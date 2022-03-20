package com.stripe.android.identity.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PairMediatorLiveDataTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val firstLiveData = MutableLiveData<Resource<String>>()
    private val secondLiveData = MutableLiveData<Resource<String>>()

    @Test
    fun `when initialized then emits loading`() {
        val pairMediatorLiveData = PairMediatorLiveData(firstLiveData, secondLiveData)
        // need a observer to trigger latest value.
        pairMediatorLiveData.observeForever {}
        assertThat(pairMediatorLiveData.value?.status).isEqualTo(Status.LOADING)
    }

    @Test
    fun `when one live data posts value then emits loading`() {
        val pairMediatorLiveData = PairMediatorLiveData(firstLiveData, secondLiveData)
        pairMediatorLiveData.observeForever {}
        firstLiveData.postValue(Resource.success("data"))
        assertThat(pairMediatorLiveData.value?.status).isEqualTo(Status.LOADING)
    }

    @Test
    fun `when one live data posts error then emits error`() {
        val pairMediatorLiveData = PairMediatorLiveData(firstLiveData, secondLiveData)
        pairMediatorLiveData.observeForever {}
        firstLiveData.postValue(Resource.error())
        assertThat(pairMediatorLiveData.value?.status).isEqualTo(Status.ERROR)
    }

    @Test
    fun `when both livedatas post value then emits success`() {
        val pairMediatorLiveData = PairMediatorLiveData(firstLiveData, secondLiveData)
        pairMediatorLiveData.observeForever {}
        val value1 = "value1"
        val value2 = "value2"
        firstLiveData.postValue(Resource.success(value1))
        secondLiveData.postValue(Resource.success(value2))
        assertThat(pairMediatorLiveData.value?.status).isEqualTo(Status.SUCCESS)
        assertThat(pairMediatorLiveData.value?.data).isEqualTo(Pair(value1, value2))
    }
}
