package com.stripe.android.connections.di

/**
 * Global instance that holds application component with entire DI graph.
 */
object ComponentHolder {
  val components = mutableSetOf<Any>()

  inline fun <reified T> component(): T = components
    .filterIsInstance<T>()
    .single()
}