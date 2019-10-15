package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.stripe.android.ObjectBuilder
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivityStarter.Args
import com.stripe.android.view.PaymentMethodsActivityStarter.Companion.REQUEST_CODE
import com.stripe.android.view.PaymentMethodsActivityStarter.Result
import com.stripe.android.view.PaymentMethodsActivityStarter.Result.Companion.fromIntent

/**
 * A class to start [PaymentMethodsActivity]. Arguments for the activity can be specified
 * with [Args] and constructed with [Args.Builder].
 *
 * The result data is a [Result] instance, obtained using [Result.fromIntent]}.
 * The result will be returned with request code [REQUEST_CODE].
 */
class PaymentMethodsActivityStarter : ActivityStarter<PaymentMethodsActivity, Args> {

    constructor(activity: Activity) : super(
        activity,
        PaymentMethodsActivity::class.java,
        Args.DEFAULT,
        REQUEST_CODE
    )

    constructor(fragment: Fragment) : super(
        fragment,
        PaymentMethodsActivity::class.java,
        Args.DEFAULT,
        REQUEST_CODE
    )

    data class Args internal constructor(
        internal val initialPaymentMethodId: String?,
        val shouldRequirePostalCode: Boolean,
        @LayoutRes val addPaymentMethodFooter: Int,
        internal val isPaymentSessionActive: Boolean,
        internal val paymentMethodTypes: List<PaymentMethod.Type>,
        internal val paymentConfiguration: PaymentConfiguration?
    ) : ActivityStarter.Args {
        private constructor(parcel: Parcel) : this(
            initialPaymentMethodId = parcel.readString(),
            shouldRequirePostalCode = parcel.readInt() == 1,
            addPaymentMethodFooter = parcel.readInt(),
            isPaymentSessionActive = parcel.readInt() == 1,
            paymentMethodTypes = ((0 until parcel.readInt()).mapNotNull {
                PaymentMethod.Type.valueOf(requireNotNull(parcel.readString()))
            }),
            paymentConfiguration = parcel.readParcelable(
                PaymentConfiguration::class.java.classLoader
            )
        )

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(initialPaymentMethodId)
            dest.writeInt(if (shouldRequirePostalCode) 1 else 0)
            dest.writeInt(addPaymentMethodFooter)
            dest.writeInt(if (isPaymentSessionActive) 1 else 0)
            dest.writeInt(paymentMethodTypes.size)
            paymentMethodTypes.forEach { dest.writeString(it.name) }
            dest.writeParcelable(paymentConfiguration, 0)
        }

        class Builder : ObjectBuilder<Args> {
            private var initialPaymentMethodId: String? = null
            private var shouldRequirePostalCode = false
            private var isPaymentSessionActive = false
            private var paymentMethodTypes: List<PaymentMethod.Type>? = null
            private var paymentConfiguration: PaymentConfiguration? = null
            @LayoutRes
            private var addPaymentMethodFooter: Int = 0

            fun setInitialPaymentMethodId(initialPaymentMethodId: String?): Builder {
                this.initialPaymentMethodId = initialPaymentMethodId
                return this
            }

            fun setShouldRequirePostalCode(shouldRequirePostalCode: Boolean): Builder {
                this.shouldRequirePostalCode = shouldRequirePostalCode
                return this
            }

            fun setIsPaymentSessionActive(isPaymentSessionActive: Boolean): Builder {
                this.isPaymentSessionActive = isPaymentSessionActive
                return this
            }

            fun setPaymentConfiguration(
                paymentConfiguration: PaymentConfiguration?
            ): Builder {
                this.paymentConfiguration = paymentConfiguration
                return this
            }

            internal fun setPaymentMethodTypes(
                paymentMethodTypes: List<PaymentMethod.Type>
            ): Builder {
                this.paymentMethodTypes = paymentMethodTypes
                return this
            }

            fun setAddPaymentMethodFooter(@LayoutRes addPaymentMethodFooter: Int): Builder {
                this.addPaymentMethodFooter = addPaymentMethodFooter
                return this
            }

            override fun build(): Args {
                return Args(
                    initialPaymentMethodId = initialPaymentMethodId,
                    shouldRequirePostalCode = shouldRequirePostalCode,
                    isPaymentSessionActive = isPaymentSessionActive,
                    paymentMethodTypes = paymentMethodTypes ?: listOf(PaymentMethod.Type.Card),
                    paymentConfiguration = paymentConfiguration,
                    addPaymentMethodFooter = addPaymentMethodFooter
                )
            }
        }

        companion object {
            internal val DEFAULT = Builder().build()

            @JvmStatic
            fun create(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.EXTRA))
            }

            @JvmField
            val CREATOR: Parcelable.Creator<Args> = object : Parcelable.Creator<Args> {
                override fun createFromParcel(parcel: Parcel): Args {
                    return Args(parcel)
                }

                override fun newArray(size: Int): Array<Args?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    /**
     * The result of a [PaymentMethodsActivity].
     *
     * Retrieve in `#onActivityResult()` using [fromIntent].
     */
    data class Result internal constructor(
        @JvmField val paymentMethod: PaymentMethod,
        private val useGooglePay: Boolean = false
    ) : ActivityStarter.Result {
        private constructor(parcel: Parcel) : this(
            paymentMethod = requireNotNull(
                parcel.readParcelable(PaymentMethod::class.java.classLoader)
            ),
            useGooglePay = parcel.readInt() == 1
        )

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(ActivityStarter.Result.EXTRA, this)
            return bundle
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(paymentMethod, flags)
            dest.writeInt(if (useGooglePay) 1 else 0)
        }

        companion object {
            /**
             * @return the [Result] object from the given `Intent`
             */
            @JvmStatic
            fun fromIntent(intent: Intent): Result? {
                return intent.getParcelableExtra(ActivityStarter.Result.EXTRA)
            }

            @JvmField
            val CREATOR: Parcelable.Creator<Result> = object : Parcelable.Creator<Result> {
                override fun createFromParcel(parcel: Parcel): Result {
                    return Result(parcel)
                }

                override fun newArray(size: Int): Array<Result?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 6000
    }
}
