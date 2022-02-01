# stripe-3ds2-android
Stripe's Android 3DS2 SDK **internal-only**

## Preparing a new release
1. Update the [changelog](https://git.corp.stripe.com/stripe-internal/android/blob/master/3ds2sdk/CHANGELOG.md) with version name, release date, and notable changes
2. Bump the version name and code:
    - [VERSION](https://git.corp.stripe.com/stripe-internal/android/blob/master/3ds2sdk/VERSION)
      ```
      // before
      5.1.1


      // after
      5.2.0
      ```

    - [gradle.properties](https://git.corp.stripe.com/stripe-internal/android/blob/master/3ds2sdk/gradle.properties)
      ```
      // before
      # Update SdkVersion when publishing a new release
      VERSION_NAME=5.1.1


      // after
      # Update SdkVersion when publishing a new release
      VERSION_NAME=5.2.0
      ```
      
    - [SdkVersion.kt](https://git.corp.stripe.com/stripe-internal/android/blob/master/3ds2sdk/src/main/kotlin/com/stripe/android/stripe3ds2/SdkVersion.kt)
      ```
      // before
      internal object SdkVersion {
          // update these values when publishing a new release
          internal const val VERSION_NAME = "5.1.1"
          internal const val VERSION_CODE = 8
      }


      // after
      internal object SdkVersion {
          // update these values when publishing a new release
          internal const val VERSION_NAME = "5.2.0"
          internal const val VERSION_CODE = 9
      }
      ```

## Testing
To test changes to this project with `android` locally:
1. Bump the version number in `gradle.properties` in the 3ds2sdk module:
```
// before
VERSION_NAME=2.0.0

// after
VERSION_NAME=2.0.1
```

2. Execute `./gradlew :3ds2sdk:publishMavenAarPublicationToMavenLocal` from `android` to publish locally.
3. Update the `android` version in `:3ds2referenceapp`'s `build.gradle`
```
dependencies {
    // old version: implementation "com.stripe:stripe-3ds2-android:2.0.0"
    // new version:
    implementation "com.stripe:stripe-3ds2-android:2.0.1"
```

## Publishing
To publish a new version of `stripe-3ds2-android`

1. [Prepare a new release](#preparing-a-new-release) and commit changes
2. Create a [new GitHub release](https://git.corp.stripe.com/stripe-internal/android/releases/new)
    - The tag should be `3ds2sdk/[version_name]` (e.g. `3ds2sdk/v5.2.0`)
    - The release name should be "3DS2 SDK `version_name`" (e.g. "3DS2 SDK v5.2.0")
3. Navigate to the local [bindings](https://git.corp.stripe.com/stripe-internal/bindings) repository directory
4. Run `sc-2fa`
5. Run `bin/upload_bindings --finalize android_3ds2_sdk`
6. After the script completes, verify that the new version has been released on [Sonatype](https://oss.sonatype.org/#nexus-search;quick~stripe-3ds2-android)
7. Update [Android SDK Binary Size](https://confluence.corp.stripe.com/display/MOBILE/Android+SDK+Binary+Size) with the release date and binary size
