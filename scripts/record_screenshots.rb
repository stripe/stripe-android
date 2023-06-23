#!/usr/bin/env ruby

require 'open3'

def current_branch
    pr_branch = ENV['PR_BRANCH']
    if pr_branch.nil? || pr_branch.empty?
        'master'
    else
        pr_branch
    end
end

def create_new_branch_name(current_branch)
    timestamp = Time.now.strftime("%Y-%m-%d-%H-%M-%S")
    "ci/updated-screenshots-for-#{current_branch}-#{timestamp}"
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

    branch = current_branch

    return token, repo, branch
end

# ------------------------------

token, repo, base_branch = setup

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
new_branch_name = create_new_branch_name(base_branch)
execute_or_fail("git checkout -b #{new_branch_name}")
execute_or_fail("git add .")
execute_or_fail("git commit -m \"Update screenshots\"")

# Push the changes
push_url = "https://#{token}@github.com/#{repo}.git"
execute_or_fail("git push --force #{push_url}")

# Comment on original pull request
diff_url = "https://github.com/#{repo}/compare/#{base_branch}...#{new_branch_name}"

if base_branch == "master"
    # If this is a push to master, output the diff URL for later usage
    puts "Screenshot tests failed.\n\nMerge the screenshot diff here if it's an intentional change: #{diff_url}"
else
    # If this is a pull request, comment on it
    comment = "Screenshot tests failed.\n\n[See differences](#{diff_url})\n\nMerge the branch if it's an intentional change."
    system("::set-output name=PR_COMMENT::\"#{comment}\"")
end
