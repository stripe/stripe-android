#!/usr/bin/env ruby

require 'open3'

def fetch_password(password)
    stdout, stderr, status = Open3.capture3("fetch-password --raw #{password}")

    stdout
end

def rputs(string)
    puts string.red
end

def wait_for_user
    rputs "Press enter to continue..."
    STDIN.gets
end

def execute(command)
    puts "Executing #{command}..."
    system(command)
end

def execute_or_fail(command)
    puts "Executing #{command}..."
    system(command) or raise "Failed to execute #{command}"
end

def open_url(url)
    puts "Opening url #{url}"
    `open #{url}`
end

def delete_git_branch(branch_name)
    # Ensure we are not on the same branch that we're trying to delete
    execute("git checkout #{@deploy_branch}")

    # Actually delete the branch
    execute("git push origin --delete #{branch_name}")
    execute("git branch -D #{branch_name}")
end

def switch_to_new_branch(branch_name)
    # Ensure a different version of this branch doesn't already exist.
    delete_git_branch(branch_name)

    execute_or_fail("git checkout -b #{branch_name} -u #{@deploy_branch}")
end

def create_pr(
    pr_branch,
    pr_title,
    pr_description,
    user_message
)
    response = github_login.create_pull_request(
        "stripe/stripe-android",
        "master",
        pr_branch,
        pr_title,
        pr_description,
        draft: @is_dry_run,
    )

    pr_url = response.html_url
    open_url(pr_url)

    rputs user_message
    wait_for_user
end

private def github_login
  token = `fetch-password -q bindings/gh-tokens/$USER`
  if $?.exitstatus != 0
    rputs "Couldn't fetch GitHub token"
    exit(1)
  end
  client = Octokit::Client.new(access_token: token)
  abort('Invalid GitHub token. Follow the wiki instructions for setting up a GitHub token.') unless client.login
  client
end
