[<img width="250" height="119" src="https://raw.githubusercontent.com/stripe/stripe-android/master/assets/stripe_logo_slate_small.png"/>](https://stripe.com/docs/mobile/android)

# Stripe Android SDK

[![Build Status](https://api.travis-ci.org/stripe/stripe-android.svg?branch=master)](https://travis-ci.org/stripe/stripe-android)
[![GitHub release](https://img.shields.io/github/release/stripe/stripe-android.svg?maxAge=60)](https://github.com/stripe/stripe-android/releases)
[![License](https://img.shields.io/github/license/stripe/stripe-android)](https://github.com/stripe/stripe-android/blob/master/LICENSE)

The Stripe Android SDK makes it quick and easy to build an excellent payment experience in your Android app. We provide powerful and customizable UI elements that can be used out-of-the-box to collect your users' payment details. We also expose the low-level APIs that power those UIs so that you can build fully custom experiences. 

Get started with our [📚 integration guides](https://stripe.com/docs/payments) and [example projects](#examples), or [📘 browse the SDK reference](https://stripe.dev/stripe-android/).

> Updating to a newer version of the SDK? See our [migration guide](https://github.com/stripe/stripe-android/blob/master/MIGRATING.md) and [changelog](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md).

> If you are building an Android application that charges a credit card, you should use the Stripe Android SDK to make sure you don't pass credit card information to your server (and, so, are PCI compliant).

Table of contents
=================

<!--ts-->
   * [Installation](#installation)
      * [Requirements](#requirements)
      * [Configuration](#configuration)
      * [Releases](#releases)
      * [Proguard](#proguard)
   * [Features](#features)
   * [Usage](#releases)
      * [Getting Started](#getting-started)
      * [Using CardInputWidget](#using-cardinputwidget)
      * [Using CardMultilineWidget](#using-cardmultilinewidget)
      * [Client-side Card Validation](#client-side-card-validation)
   * [Examples](#examples)
<!--te-->

## Installation

### Requirements

* Android 4.4 (API level 19) and above
* [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 3.5.1
* [Gradle](https://gradle.org/releases/) 5.4.1+
* [AndroidX](https://developer.android.com/jetpack/androidx/) (as of v11.0.0)

### Configuration

Add `stripe-android` to your `build.gradle` dependencies.

```
dependencies {
    implementation 'com.stripe:stripe-android:12.3.0'
}
```

### Releases
* The [changelog](CHANGELOG.md) provides a summary of changes in each release.
* The [migration guide](MIGRATING.md) provides instructions on upgrading from older versions.

### Proguard

The Stripe Android SDK will configure your app's Proguard rules using [proguard-rules.txt](stripe/proguard-rules.txt).

## Features

**Simplified Security**: Use the SDK to collect credit card numbers and remain [PCI compliant](https://stripe.com/docs/security#pci-dss-guidelines). This means sensitive data is sent directly to Stripe instead of passing through your server. For more information, see our [Integration Security Guide](https://stripe.com/docs/security).

**Google Pay**: Stripe is fully compatible with [Google Pay](https://stripe.com/docs/google-pay).

**Stripe API**: We provide [low-level APIs](https://stripe.dev/stripe-android/com/stripe/android/Stripe.html) that correspond to objects and methods in the Stripe API. You can build your own entirely custom UI on top of this layer.

**Native UI**: We provide native screens and elements to collect payment and shipping details. For example, [CardInputWidget](https://stripe.dev/stripe-android/com/stripe/android/view/CardInputWidget.html) is a view that collects and validates card details. You can use these individually, or take all of the prebuilt UI in one flow by following the [Basic Integration guide](https://stripe.com/docs/mobile/android/basic).

<img width="270" height="555" src="https://raw.githubusercontent.com/stripe/stripe-android/master/assets/card_input.gif"/>

## Usage

### Getting Started

Get started with our [📚 integration guides](https://stripe.com/docs/payments) and [example projects](#examples), or [ browse the SDK reference](https://stripe.dev/stripe-android/).

The `Stripe` class is the entry-point to the Stripe SDK. It must be instantiated with a [Stripe publishable key](https://stripe.com/docs/keys).

When testing, you can use a test publishable key. Remember to replace the test key with your live key in production. You can view your API keys in the [Stripe Dashboard](https://dashboard.stripe.com/apikeys).

```java
new Stripe(context, "pk_test_yourkey");
```

### Using CardInputWidget

<img width="270" height="555" src="https://raw.githubusercontent.com/stripe/stripe-android/master/assets/card_input.gif"/>

You can add a single-line widget to your apps that easily handles the UI states for collecting card data.

First, add the `CardInputWidget` to your layout.

```xml
<com.stripe.android.view.CardInputWidget
    android:id="@+id/card_input_widget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    />
```

Note: The minimum width for this widget is 320dp. The widget also requires an ID to ensure proper layout on rotation, so if you don't do this, an ID is assigned when the object is instantiated.

If the customer's input is valid, [CardInputWidget#getCard()](https://stripe.dev/stripe-android/com/stripe/android/view/CardInputWidget.html#getCard--) will return a [Card](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html) instance; otherwise, it will return `null`. 

```java
final Card cardToSave = cardInputWidget.getCard();

if (cardToSave == null) {
    mErrorDialogHandler.showError("Invalid Card Data");
    return;
}
```

### Using CardMultilineWidget

<img width="270" height="555" src="https://raw.githubusercontent.com/stripe/stripe-android/master/assets/card_multiline.gif"/>

You can add a Material-style multiline widget to your apps that handles card data collection as well. This can be added in a layout similar to the `CardInputWidget`.

```xml
<com.stripe.android.view.CardMultilineWidget
    android:id="@+id/card_multiline_widget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:shouldShowPostalCode="true"
    />
```

Note: A `CardMultilineWidget` can only be added in the view of an `Activity` whose `Theme` descends from an `AppCompat` theme.

In order to use the `app:shouldShowPostalCode` tag, you'll need to enable the app XML namespace somewhere in the layout.

Note: We currently only support US ZIP in the postal code field.

```xml
xmlns:app="http://schemas.android.com/apk/res-auto"
```

If the customer's input is valid, [CardMultilineWidget#getCard()](https://stripe.dev/stripe-android/com/stripe/android/view/CardMultilineWidget.html#getCard--) will return a [Card](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html) instance; otherwise, it will return `null`. 

```java
final Card cardToSave = cardMultilineWidget.getCard();

if (cardToSave == null) {
    mErrorDialogHandler.showError("Invalid Card Data");
    return;
}
```

If the returned `Card` is `null`, error states will show on the fields that need to be fixed.

### Client-side Card Validation

The [Card](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html) object allows you to validate user input before you send the information to the Stripe API.

- [Card#validateNumber()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateNumber--) - Checks that the number is formatted correctly and passes the [Luhn check](http://en.wikipedia.org/wiki/Luhn_algorithm).
- [Card#validateExpiryDate()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateExpiryDate--) - Checks whether or not the expiration date represents an actual month in the future.
- [Card#validateCVC()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateCVC--) - Checks whether or not the supplied number could be a valid verification code.
- [Card#validateCard()](https://stripe.dev/stripe-android/com/stripe/android/model/Card.html#validateCard--) - Convenience method to validate card number, expiry date and CVC.


## Examples

- [Accept a card payment](https://github.com/stripe-samples/accept-a-card-payment) (PaymentIntents API)
- [Save a card without payment](https://github.com/stripe-samples/mobile-saving-card-without-payment) (SetupIntents API)
- [Accept a card payment](https://github.com/stripe-samples/card-payment-charges-api) (Charges API)
- The [stripe-samples/sample-store-android](https://github.com/stripe-samples/sample-store-android) repo demonstrates how to build a payment flow using our prebuilt UI components ([Basic Integration](https://stripe.com/docs/mobile/android/basic))
- The [example project](https://github.com/stripe/stripe-android/tree/master/example) demonstrates using our API bindings and UI components, including how to create Tokens, Sources, and Payment Methods; how to use the Stripe class's synchronous and asynchronous methods; and how to use the CardInputWidget.
