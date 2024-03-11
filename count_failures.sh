#!/bin/bash
count=0

for i in {1..60}
do
  ./gradlew paymentsheet:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.stripe.android.paymentsheet.PaymentSheetTest > /dev/null 2>&1
  if [ $? -eq 0 ]
  then
    echo "Attempt $i passed"
  else
    ((count++))
    echo "Attempt $i failed"
  fi
done

echo "Number of failures: $count"
