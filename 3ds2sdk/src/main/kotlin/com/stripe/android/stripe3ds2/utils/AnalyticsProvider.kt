package com.stripe.android.stripe3ds2.utils

import java.util.ServiceLoader

internal class AnalyticsProvider private constructor() {
    private val loader: ServiceLoader<AnalyticsDelegate> = ServiceLoader.load(AnalyticsDelegate::class.java)

    fun serviceImpl(): AnalyticsDelegate? {
        if (loader.iterator().hasNext()) {
            return loader.iterator().next()
        }

        return null
    }

    companion object {
        private var provider: AnalyticsProvider? = null

        val instance: AnalyticsProvider
            get() {
                val provider = this.provider ?: AnalyticsProvider()

                if (this.provider ==  null) {
                    this.provider = provider
                }

                return provider
            }
    }
}
