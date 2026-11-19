#!/usr/bin/env ruby

require 'open3'

require_relative 'common'

def ensure_clean_repo()
  repo_dir = File.basename(Dir.getwd)
  if repo_dir != "stripe-android"
    abort("You must run this script from 'stripe-android'.")
  end

  ensure_directory_is_clean()
end

def pull_latest()
    execute_or_fail("git checkout #{@deploy_branch}")
    execute_or_fail("git pull")
end

def revert_version_bump_changes()
    delete_git_branch(release_branch, @deploy_branch)
end

def fetch_release_translations()
    api_token = fetch_password('lokalise-api-token-manual').strip

    puts "Fetching latest translations from Lokalise..."
    scripts_dir = File.expand_path('..', __dir__)
    system({ 'LOKALISE_API_TOKEN' => api_token }, './localize.sh', chdir: scripts_dir) or
        raise "Failed to fetch translations from Lokalise"

    execute_or_fail("git add -- ':(glob)*/res/values*/strings.xml'")
end

def create_version_bump_pr()
    switch_to_release_branch()
    fetch_release_translations()
    update_read_me()
    update_stripe_sdk_version()
    update_gradle_properties()
    update_changelog()
    update_version()
    update_3ds2_version()
    execute_or_fail("rm -rf docs/")
    execute_or_fail("./gradlew :dokkaGenerate")
    execute_or_fail("git add -A docs/")
    execute_or_fail("git commit -m \"Bump version to #{@version}\"")

    begin
        execute_or_fail("git push -u origin")
    
        pr_description = create_pr_description()
        if (@is_dry_run)
          user_message = "Verify that a draft PR containing version number bumps, updated translations, and Dokka documentation was opened."
        else
          user_message = "Verify that a PR containing version number bumps, updated translations, and Dokka documentation was opened. Press enter once you've confirmed the PR was created."
        end

        create_pr(
           release_branch,
           "Bump version to #{@version}",
           pr_description,
           user_message
        )
    rescue
        revert_version_bump_changes()
        execute("git checkout #{@deploy_branch}")
        raise
    end
end

private def release_branch
    "release/#{@version}"
end

private def create_pr_description()
    template = File.read(File.join(File.dirname(__FILE__), '../../../.github/pull_request_template.md'))

    <<~EOS
    Bump version to `#{@version}`, fetch the latest translations from Lokalise, and regenerate Dokka documentation.

    #{template}
    EOS
end
