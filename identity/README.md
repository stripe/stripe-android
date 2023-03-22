# Stripe Android SDK Identity module
The Stripe Identity Android SDK makes it quick and easy to verify your user's identity in your Android app. We provide a prebuilt UI to collect your user's ID documents, match photo ID with selfies, and validate ID numbers.

> To get access to the Identity Android SDK, visit the [Identity Settings](https://dashboard.stripe.com/settings/identity) page and click **Enable**.

# Table of contents

<!--ts-->
* [Features](#features)
* [Requirements](#requirements)
* [Getting started](#getting-started)
  * [Integration](#integration)
  * [Example](#example)

<!--te-->

## Features

**Simplified security**: We've made it simple for you to securely collect your user's personally identifiable information (PII) such as identity document images. Sensitive PII data is sent directly to Stripe Identity instead of passing through your server. For more information, see our [integration security guide](https://stripe.com/docs/security).

**Automatic document capture**: We automatically capture images of the front and back of government-issued photo ID to ensure a clear and readable image.

**Selfie matching**: We capture photos of your user's face and review it to confirm that the photo ID belongs to them. For more information, see our guide on [adding selfie checks](https://stripe.com/docs/identity/selfie).

**Identity information collection**: We collect name, date of birth, and government ID number to validate that it is real.

**Prebuilt UI**: We provide [`IdentityVerificationSheet`](https://stripe.dev/stripe-android/identity/com.stripe.android.identity/-identity-verification-sheet/index.html), a prebuilt UI that combines all the steps required to collect ID documents, selfies, and ID numbers into a single sheet that displays on top of your app.

**Automated verification**: Stripe Identity's automated verification technology looks for patterns to help determine if an ID document is real or fake and uses distinctive physiological characteristics of faces to match your users' selfies to photos on their ID document. Collected identity information is checked against a global set of databases to confirm that it exists. Learn more about the [verification checks supported by Stripe Identity](https://stripe.com/docs/identity/verification-checks), [accessing verification results](https://stripe.com/docs/identity/access-verification-results), or our integration guide on [handling verification outcomes](https://stripe.com/docs/identity/handle-verification-outcomes).

## Requirements

If you intend to use this SDK with Stripe's Identity service, you must not modify this SDK. Using a modified version of this SDK with Stripe's Identity service, without Stripe's written authorization, is a breach of your agreement with Stripe and may result in your Stripe account being shut down.

## Getting started

### Integration

Get started with Stripe Identity's [Android integration guide](https://stripe.com/docs/identity/verify-identity-documents?platform=android) and [example project](../identity-example), or [📘 browse the SDK reference](https://stripe.dev/stripe-android/identity/index.html) for fine-grained documentation of all the classes and methods in the SDK.

### Use TFLite in Google play to reduce binary size

Identity Android SDK uses a portable TFLite runtime to execute machine learning models, if your application is released through Google play, you could instead use the Google play runtime, this would reduce the SDK size by ~1.2mb.

To do so, configure your app's dependency on stripe identity as follows.
```
    implementation("com.stripe:identity:x.y.z") {
      exclude group: 'com.stripe', module: 'ml-core-default' // exclude the default tflite runtime
    }
    implementation("com.stripe:ml-core-googleplay:x.y.z") // include the google play tflite runtime
```

### Example

[identity-example](../identity-example) – This example demonstrates how to capture your users' ID documents on Android and securely send them to Stripe Identity for identity verification.
