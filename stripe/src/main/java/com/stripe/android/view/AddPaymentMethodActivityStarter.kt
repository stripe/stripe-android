package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.LayoutRes
import com.stripe.android.ObjectBuilder
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.AddPaymentMethodActivityStarter.Args
import com.stripe.android.view.AddPaymentMethodActivityStarter.Companion.REQUEST_CODE
import com.stripe.android.view.AddPaymentMethodActivityStarter.Result.Companion.fromIntent
import java.util.Objects

/**
 * A class to start [AddPaymentMethodActivity]. Arguments for the activity can be
 * specified with [Args] and constructed with [Args.Builder].
 *
 * The result will be returned with request code [REQUEST_CODE].
 */
class AddPaymentMethodActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<AddPaymentMethodActivity, Args>(
    activity,
    AddPaymentMethodActivity::class.java,
    Args.DEFAULT,
    REQUEST_CODE
) {
    class Args private constructor(
        internal val shouldAttachToCustomer: Boolean,
        internal val shouldRequirePostalCode: Boolean,
        internal val isPaymentSessionActive: Boolean,
        internal val shouldInitCustomerSessionTokens: Boolean,
        internal val paymentMethodType: PaymentMethod.Type,
        internal val paymentConfiguration: PaymentConfiguration?,
        @LayoutRes internal val addPaymentMethodFooter: Int
    ) : ActivityStarter.Args {

        private constructor(parcel: Parcel) : this(
            shouldAttachToCustomer = parcel.readInt() == 1,
            shouldRequirePostalCode = parcel.readInt() == 1,
            isPaymentSessionActive = parcel.readInt() == 1,
            shouldInitCustomerSessionTokens = parcel.readInt() == 1,
            paymentMethodType = PaymentMethod.Type.valueOf(requireNotNull(parcel.readString())),
            paymentConfiguration = parcel.readParcelable(
                PaymentConfiguration::class.java.classLoader
            ),
            addPaymentMethodFooter = parcel.readInt()
        )

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(if (shouldAttachToCustomer) 1 else 0)
            dest.writeInt(if (shouldRequirePostalCode) 1 else 0)
            dest.writeInt(if (isPaymentSessionActive) 1 else 0)
            dest.writeInt(if (shouldInitCustomerSessionTokens) 1 else 0)
            dest.writeString(paymentMethodType.name)
            dest.writeParcelable(paymentConfiguration, 0)
            dest.writeInt(addPaymentMethodFooter)
        }

        override fun hashCode(): Int {
            return Objects.hash(shouldAttachToCustomer, shouldRequirePostalCode,
                isPaymentSessionActive, shouldInitCustomerSessionTokens, paymentMethodType,
                paymentConfiguration, addPaymentMethodFooter)
        }

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is Args -> typedEquals(other)
                else -> false
            }
        }

        private fun typedEquals(args: Args): Boolean {
            return shouldAttachToCustomer == args.shouldAttachToCustomer &&
                shouldRequirePostalCode == args.shouldRequirePostalCode &&
                isPaymentSessionActive == args.isPaymentSessionActive &&
                shouldInitCustomerSessionTokens == args.shouldInitCustomerSessionTokens &&
                paymentMethodType == args.paymentMethodType &&
                addPaymentMethodFooter == args.addPaymentMethodFooter &&
                paymentConfiguration == args.paymentConfiguration
        }

        class Builder : ObjectBuilder<Args> {
            private var shouldAttachToCustomer: Boolean = false
            private var shouldRequirePostalCode: Boolean = false
            private var isPaymentSessionActive = false
            private var shouldInitCustomerSessionTokens = true
            private var paymentMethodType: PaymentMethod.Type? = null
            private var paymentConfiguration: PaymentConfiguration? = null
            @LayoutRes
            private var addPaymentMethodFooter: Int = 0

            /**
             * If true, the created Payment Method will be attached to the current Customer
             * using an already-initialized [com.stripe.android.CustomerSession].
             */
            fun setShouldAttachToCustomer(shouldAttachToCustomer: Boolean): Builder {
                this.shouldAttachToCustomer = shouldAttachToCustomer
                return this
            }

            /**
             * If true, a postal code field will be shown and validated.
             * Currently, only US ZIP Codes are supported.
             */
            fun setShouldRequirePostalCode(shouldRequirePostalCode: Boolean): Builder {
                this.shouldRequirePostalCode = shouldRequirePostalCode
                return this
            }

            internal fun setIsPaymentSessionActive(isPaymentSessionActive: Boolean): Builder {
                this.isPaymentSessionActive = isPaymentSessionActive
                return this
            }

            internal fun setShouldInitCustomerSessionTokens(
                shouldInitCustomerSessionTokens: Boolean
            ): Builder {
                this.shouldInitCustomerSessionTokens = shouldInitCustomerSessionTokens
                return this
            }

            internal fun setPaymentMethodType(paymentMethodType: PaymentMethod.Type): Builder {
                this.paymentMethodType = paymentMethodType
                return this
            }

            internal fun setPaymentConfiguration(
                paymentConfiguration: PaymentConfiguration?
            ): Builder {
                this.paymentConfiguration = paymentConfiguration
                return this
            }

            fun setAddPaymentMethodFooter(@LayoutRes addPaymentMethodFooter: Int): Builder {
                this.addPaymentMethodFooter = addPaymentMethodFooter
                return this
            }

            override fun build(): Args {
                return Args(
                    shouldAttachToCustomer = shouldAttachToCustomer,
                    shouldRequirePostalCode = shouldRequirePostalCode,
                    isPaymentSessionActive = isPaymentSessionActive,
                    shouldInitCustomerSessionTokens = shouldInitCustomerSessionTokens,
                    paymentMethodType = paymentMethodType ?: PaymentMethod.Type.Card,
                    paymentConfiguration = paymentConfiguration,
                    addPaymentMethodFooter = addPaymentMethodFooter
                )
            }
        }

        companion object {
            internal val DEFAULT = Builder().build()

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
     * The result of a [AddPaymentMethodActivity].
     *
     * Retrieve in `#onActivityResult()` using [fromIntent].
     */
    class Result internal constructor(
        val paymentMethod: PaymentMethod
    ) : ActivityStarter.Result {
        private constructor(parcel: Parcel) : this(
            paymentMethod = requireNotNull(
                parcel.readParcelable(PaymentMethod::class.java.classLoader)
            )
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
        }

        override fun hashCode(): Int {
            return Objects.hash(paymentMethod)
        }

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is Result -> typedEquals(other)
                else -> false
            }
        }

        private fun typedEquals(other: Result): Boolean {
            return paymentMethod == other.paymentMethod
        }

        companion object {
            /**
             * @return the [Result] object from the given `Intent`
             */
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
        const val REQUEST_CODE: Int = 6001
    }
}
