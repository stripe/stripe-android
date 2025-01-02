package com.stripe.android.financialconnections.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import javax.inject.Singleton

/**
 * A singleton holder for the [FinancialConnectionsSingletonSharedComponent], ensuring it is initialized only once and
 * shared across activities.
 */
internal object FinancialConnectionsSingletonSharedComponentHolder {

    @Volatile
    private var component: FinancialConnectionsSingletonSharedComponent? = null

    fun getComponent(application: Application): FinancialConnectionsSingletonSharedComponent {
        return component ?: synchronized(this) {
            component ?: buildComponent(application).also { component = it }
        }
    }

    private fun buildComponent(application: Application): FinancialConnectionsSingletonSharedComponent {
        return DaggerFinancialConnectionsSingletonSharedComponent.builder()
            .application(application)
            .build()
    }
}

@Singleton
@Component(modules = [FinancialConnectionsSingletonSharedModule::class])
internal interface FinancialConnectionsSingletonSharedComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: Application): Builder

        fun build(): FinancialConnectionsSingletonSharedComponent
    }
}

@Module
internal class FinancialConnectionsSingletonSharedModule
