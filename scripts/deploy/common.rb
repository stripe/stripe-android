#!/usr/bin/env ruby

require 'open3'

def fetch_password(password)
    stdout, stderr, status = Open3.capture3("fetch-password --raw #{password}")

    if !stderr.empty?
        raise("Failed to retrieve password: #{password}")
    end

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

def replace_in_file(filename, pattern, replacement)
  puts "> Updating #{filename}"
  content = File.read(filename)
  new_content = content.sub(pattern, replacement)
  File.write(filename, new_content)
end

def append_to_file(filename, new_content)
    puts "> Updating #{filename}"
    content = File.read(filename)
    File.write(filename, "#{content}\n#{new_content}\n")
end

def switch_to_release_branch()
    switch_to_new_branch(release_branch, @deploy_branch)
end

def ensure_directory_is_clean()
    stdout, stderr, status = Open3.capture3("git status --porcelain")

    if !stdout.empty?
        abort("You must have a clean working directory to continue.")
    end
end

def delete_git_branch(branch_to_delete, main_branch, repo: ".")
    # Ensure we are not on the same branch that we're trying to delete
    execute("git -C #{repo} checkout #{main_branch}")

    # Actually delete the branch
    execute("git -C #{repo} push origin --delete #{branch_to_delete}")
    execute("git -C #{repo} branch -D #{branch_to_delete}")
end

def switch_to_new_branch(branch_to_create, main_branch, repo: ".")
    # Ensure a different version of this branch doesn't already exist.
    delete_git_branch(branch_to_create, main_branch, repo: repo)

    execute_or_fail("git -C #{repo} checkout -b #{branch_to_create}")
    execute_or_fail("git -C #{repo} branch -u #{main_branch}")
end

def local_git_tag_exists?(tag, repo: ".")
    system("git -C #{repo} show-ref --verify --quiet refs/tags/#{tag}")
end

def remote_git_tag_exists?(tag, repo: ".", remote: "origin")
    stdout, stderr, status = Open3.capture3("git -C #{repo} ls-remote --tags #{remote} refs/tags/#{tag}")
    raise("Failed to query #{remote} for tag #{tag}: #{stderr}") unless status.success?

    !stdout.empty?
end

def git_commit_for_ref(ref, repo: ".")
    stdout, stderr, status = Open3.capture3("git", "-C", repo, "rev-list", "-n", "1", ref)
    raise("Failed to resolve #{ref}: #{stderr}") unless status.success?

    stdout.strip
end

def git_commit_exists?(commit, repo: ".")
    system("git", "-C", repo, "cat-file", "-e", "#{commit}^{commit}")
end

def release_tag_name
    "v#{@version}"
end

def create_pr(
    pr_branch,
    pr_title,
    pr_description,
    user_message
)
    response = github_login.create_pull_request(
        "stripe/stripe-android",
        @deploy_branch,
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
