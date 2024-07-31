#!/usr/bin/env ruby

require_relative 'common'

def update_read_me()
  replace_in_file("README.md",
      /implementation 'com.stripe:stripe-android:[.\d]+'/,
      "implementation 'com.stripe:stripe-android:#{@version}'",
  )
  execute_or_fail("git add README.md")
 end

def update_stripe_sdk_version()
  replace_in_file("stripe-core/src/main/java/com/stripe/android/core/version/StripeSdkVersion.kt",
      /const val VERSION_NAME = "[.\d]+"/,
      %Q{const val VERSION_NAME = "#{@version}"},
  )
  execute_or_fail("git add stripe-core/src/main/java/com/stripe/android/core/version/StripeSdkVersion.kt")
end

def update_gradle_properties()
  replace_in_file("gradle.properties",
      /VERSION_NAME=[.\d]+/,
      "VERSION_NAME=#{@version}",
  )
  execute_or_fail("git add gradle.properties")
end

def update_version()
    replace_in_file("VERSION",
        /.*/,
        @version
    )
    execute_or_fail("git add VERSION")
end

def update_changelog()
    changelog_contents = File.read("CHANGELOG.md")

    if (!changelog_contents.include? @version)
        date = Time.now.strftime("%Y-%m-%d")

        replace_in_file(
            "CHANGELOG.md",
            /## XX.XX.XX - 20XX-XX-XX/,
            "## XX.XX.XX - 20XX-XX-XX\n\n## #{@version} - #{date}"
        )
    end

    execute_or_fail("git add CHANGELOG.md")
end
