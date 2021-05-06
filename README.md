[<img width="250" height="119" src="https://raw.githubusercontent.com/stripe/stripe-android/master/assets/stripe_logo_slate_small.png"/>](https://stripe.com/docs/mobile/android)

# Stripe Android SDK

[![CI](https://github.com/stripe/stripe-android/workflows/CI/badge.svg)](https://github.com/stripe/stripe-android/actions?query=workflow%3ACI)
[![GitHub release](https://img.shields.io/github/release/stripe/stripe-android.svg?maxAge=60)](https://github.com/stripe/stripe-android/releases)
[![License](https://img.shields.io/github/license/stripe/stripe-android)](https://github.com/stripe/stripe-android/blob/master/LICENSE)

The Stripe Android SDK makes it quick and easy to build an excellent payment experience in your Android app. We provide powerful and customizable UI elements that can be used out-of-the-box to collect your users' payment details. We also expose the low-level APIs that power those UIs so that you can build fully custom experiences. 

Get started with our [📚 integration guides](https://stripe.com/docs/payments/accept-a-payment?platform=android) and [example projects](#examples), or [📘 browse the SDK reference](https://stripe.dev/stripe-android/).

> Updating to a newer version of the SDK? See our [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) and [changelog](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md).


Table of contents
=================

<!--ts-->
   * [Features](#features)
   * [Releases](#releases)
   * [Installation](#installation)
      * [Requirements](#requirements)
      * [Configuration](#configuration)
      * [Proguard](#proguard)
   * [Getting Started](#getting-started)
   * [Examples](#examples)
<!--te-->

## Features

**Simplified Security**: Use the SDK to collect credit card numbers and remain [PCI compliant](https://stripe.com/docs/security#pci-dss-guidelines). This means sensitive data is sent directly to Stripe instead of passing through your server. For more information, see our [Integration Security Guide](https://stripe.com/docs/security).

**SCA-Ready**: The SDK automatically performs native [3D Secure authentication](https://stripe.com/docs/payments/3d-secure) to comply with [Strong Customer Authentication](https://stripe.com/docs/strong-customer-authentication) regulation in Europe.

**Native UI**: We provide native screens and elements to collect payment. For example, [PaymentSheet](https://stripe.com/payments/accept-a-payment?platform=android) is a prebuilt UI that combines all the steps required to pay - collecting payment details, billing details, and confirming the payment  - into a single sheet that displays on top of your app.

<img src="https://raw.githubusercontent.com/stripe/stripe-android/michelleb/mc-update-readme/assets/payment_sheet_complete.png"/>

**Google Pay**: Stripe is fully compatible with [Google Pay](https://stripe.com/docs/google-pay).

**Stripe API**: We provide [low-level APIs](https://stripe.dev/stripe-android/stripe/com.stripe.android/-stripe/index.html) that correspond to objects and methods in the Stripe API. You can build your own entirely custom UI on top of this layer.

## Releases
* The [changelog](CHANGELOG.md) provides a summary of changes in each release.
* The [migration guide](MIGRATING.md) provides instructions on upgrading from older versions.

## Installation

### Requirements

* Android 5.0 (API level 21) and above
* [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 3.5.1
* [Gradle](https://gradle.org/releases/) 5.4.1+
* [AndroidX](https://developer.android.com/jetpack/androidx/) (as of v11.0.0)

### Configuration

Add `stripe-android` to your `build.gradle` dependencies.

```
dependencies {
    implementation 'com.stripe:stripe-android:16.7.1'
}
```

### Proguard

The Stripe Android SDK will configure your app's Proguard rules using [proguard-rules.txt](stripe/proguard-rules.txt).

## Getting Started

Get started with our [📚 integration guides](https://stripe.com/docs/payments) and [example projects](#examples), or [📘 browse the SDK reference](https://stripe.dev/stripe-android/).

The `PaymentSheet` class is the entry-point to the pre-built UI. It must be instantiated with a [Stripe publishable key](https://stripe.com/docs/keys) and application context.

When testing, you can use a test publishable key. Remember to replace the test key with your live key in production. You can view your API keys in the [Stripe Dashboard](https://dashboard.stripe.com/apikeys).

```kotlin
PaymentConfiguration.init(getApplicationContext(), "pk_test_...");
```

## Examples
- The [example project](https://github.com/stripe/stripe-android/tree/master/example) demonstrates:
    - using our prebuilt ui (single-step and multi-step)
    - how to use the Stripe class's synchronous and asynchronous methods
    - how to use the CardInputWidget.


