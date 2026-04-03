package com.stripe.android.core.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.RestrictTo

private const val STORAGE_FILE_NAME = "stripe_shared_prefs"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SharedPreferencesStorage(
    private val context: Context,
    private val purpose: String
) : Storage {
    private val sharedPrefs: SharedPreferences? by lazy {
        context.getSharedPreferences(STORAGE_FILE_NAME, Context.MODE_PRIVATE)
    }

    override fun storeValue(key: String, value: String) = sharedPrefs?.run {
        with(edit()) {
            putString("${purpose}_$key", value)
            commit()
        }
    } ?: false.apply {
        Log.e(logTag, "Shared preferences is unavailable to store $value for $key")
    }

    override fun storeValue(key: String, value: Long) = sharedPrefs?.run {
        with(edit()) {
            putLong("${purpose}_$key", value)
            commit()
        }
    } ?: false.apply {
        Log.e(logTag, "Shared preferences is unavailable to store $value for $key")
    }

    override fun storeValue(key: String, value: Int) = sharedPrefs?.run {
        with(edit()) {
            putInt("${purpose}_$key", value)
            commit()
        }
    } ?: false.apply {
        Log.e(logTag, "Shared preferences is unavailable to store $value for $key")
    }

    override fun storeValue(key: String, value: Float) = sharedPrefs?.run {
        with(edit()) {
            putFloat("${purpose}_$key", value)
            commit()
        }
    } ?: false.apply {
        Log.e(logTag, "Shared preferences is unavailable to store $value for $key")
    }

    override fun storeValue(key: String, value: Boolean) = sharedPrefs?.run {
        with(edit()) {
            putBoolean("${purpose}_$key", value)
            commit()
        }
    } ?: false.apply {
        Log.e(logTag, "Shared preferences is unavailable to store $value for $key")
    }

    override fun getString(key: String, defaultValue: String): String {
        return try {
            sharedPrefs?.getString("${purpose}_$key", defaultValue) ?: defaultValue.apply {
                Log.e(logTag, "Unable to retrieve a String for $key")
            }
        } catch (t: Throwable) {
            when (t) {
                is ClassCastException -> Log.e(logTag, "$key is not a String", t)
                else -> Log.d(logTag, "Error retrieving String for $key", t)
            }
            defaultValue
        }
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return try {
            sharedPrefs?.getLong("${purpose}_$key", defaultValue) ?: defaultValue.apply {
                Log.e(logTag, "Unable to retrieve a Long for $key")
            }
        } catch (t: Throwable) {
            when (t) {
                is ClassCastException -> Log.e(logTag, "$key is not a Long", t)
                else -> Log.d(logTag, "Error retrieving Long for $key", t)
            }
            defaultValue
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return try {
            sharedPrefs?.getInt("${purpose}_$key", defaultValue) ?: defaultValue.apply {
                Log.e(logTag, "Unable to retrieve an Int for $key")
            }
        } catch (t: Throwable) {
            when (t) {
                is ClassCastException -> Log.e(logTag, "$key is not a Int", t)
                else -> Log.d(logTag, "Error retrieving Int for $key", t)
            }
            defaultValue
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            sharedPrefs?.getFloat("${purpose}_$key", defaultValue) ?: defaultValue.apply {
                Log.e(logTag, "Unable to retrieve a Float for $key")
            }
        } catch (t: Throwable) {
            when (t) {
                is ClassCastException -> Log.e(logTag, "$key is not a Float", t)
                else -> Log.d(logTag, "Error retrieving Float for $key", t)
            }
            defaultValue
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            sharedPrefs?.getBoolean("${purpose}_$key", defaultValue) ?: defaultValue.apply {
                Log.e(logTag, "Unable to retrieve a Boolean for $key")
            }
        } catch (t: Throwable) {
            when (t) {
                is ClassCastException -> Log.e(logTag, "$key is not a Boolean", t)
                else -> Log.d(logTag, "Error retrieving Boolean for $key", t)
            }
            defaultValue
        }
    }

    override fun remove(key: String): Boolean = sharedPrefs?.run {
        with(edit()) {
            remove(key)
            commit()
        }
    } ?: false.apply {
        Log.e(logTag, "Shared preferences is unavailable to remove values")
    }

    override fun clear(): Boolean = sharedPrefs?.run {
        with(edit()) {
            clear()
            commit()
        }
    } ?: false.apply {
        Log.e(logTag, "Shared preferences is unavailable to clear values")
    }

    private companion object {
        private val logTag: String = SharedPreferencesStorage::class.java.simpleName
    }
}
