#!/usr/bin/env ruby

require 'open3'

def create_new_branch_name
    timestamp = Time.now.strftime("%Y-%m-%d-%H-%M-%S")
    "ci/updated-screenshots-#{timestamp}"
end

def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

def setup
    token = ENV['GITHUB_TOKEN']
    if token.nil?
        abort("Missing GITHUB_TOKEN environment variable.")
    end

    repo = ENV['GITHUB_REPOSITORY']
    if repo.nil?
        abort("Missing GITHUB_REPOSITORY environment variable.")
    end

    return token, repo
end

# ------------------------------

token, repo = setup

# Record the screenshots
record_command = './gradlew paymentsheet-example:executeScreenshotTests -Precord -Pandroid.testInstrumentationRunnerArguments.package=com.stripe.android.screenshot'
# We need to append these weird flags because of a Java 17 bug in Shot
# See: https://github.com/pedrovgs/Shot/issues/268
jdk_17_args = '-Dorg.gradle.jvmargs="--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.nio.channels=ALL-UNNAMED --add-exports java.base/sun.nio.ch=ALL-UNNAMED"'
system("#{record_command} #{jdk_17_args}")

diff_command = 'git status --porcelain'
stdout, _, _ = Open3.capture3(diff_command)

if stdout.empty?
    puts "No differences in screenshots found."
    exit true
end

# Commit the changes
new_branch_name = create_new_branch_name
execute_or_fail("git checkout -b #{new_branch_name}")
execute_or_fail("git add .")
execute_or_fail("git commit -m \"Update screenshots\"")

# Push the changes
push_url = "https://#{token}@github.com/#{repo}.git"
execute_or_fail("git push --force #{push_url}")

# Print the URL for the fixing pull request
diff_url = "https://github.com/#{repo}/compare/#{new_branch_name}?expand=1"
puts "Screenshot tests failed.\n\nMerge the screenshot diff here if it's an intentional change: #{diff_url}"

exit false
