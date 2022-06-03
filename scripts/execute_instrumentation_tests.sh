# Uncomment if tests are working locally but not in the github action. Keeping logcat output makes
# github perform slow (and can crash your browser) because of all of the logs and isn't necessary
# for debugging all issues.
# adb logcat -c
# adb logcat &

# Exclude any modules with screenshot tests here. Then run them with the screenshot test package excluded.
# Don't forget to add your module to actions/upload-artifact@v2 task.
./gradlew connectedAndroidTest -x :paymentsheet-example:connectedAndroidTest -x :paymentsheet:connectedAndroidTest

./gradlew :paymentsheet:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.notPackage=com.stripe.android.paymentsheet.screenshot
