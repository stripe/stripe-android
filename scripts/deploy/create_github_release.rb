#!/usr/bin/env ruby

require 'octokit'
require 'subprocess'

require_relative 'common'
require_relative 'gnupg_utils'

@release_url = nil

def create_github_release
    # Must be run on the deploy branch, because it depends on changes made in
    # create_version_bump_pr (updating CHANGELOG.md)
    execute_or_fail("git checkout #{@deploy_branch}")
    execute_or_fail("git pull")

    build_example_release_apk
    tag_release

    begin
        release_response = octokit_client.create_release(
          "stripe/stripe-android",
          "v#{@version}",
          name: "stripe-android v#{@version}",
          body: release_description,
          draft: @is_dry_run
        )

        @release_url = release_response.url
        upload_example_apk_to_release(@release_url)

        rputs "Created new release"
        open_url(release_response.html_url)

        if (@is_dry_run)
            rputs "Please verify that the release was opened + created properly. It should contain a changelog of the changes for this release."
            rputs "Since this is a dry run, you should see the release as a draft. It will be missing a tag + source code attachments."
            wait_for_user
        end
    rescue
        delete_release_tag
    end
end

def delete_github_release
   delete_release_tag

   if (@release_url != nil)
       puts "Deleting release..."
       deleting_release_succeeded = octokit_client.delete_release("#{@release_url}__")
       if (!deleting_release_succeeded)
           rputs "Deleting release failed! Please manually delete release."
       end
   end
end

private def tag_name
    "v#{@version}"
end

private def tag_release
    gnupg_env do |env|
        Subprocess.check_call(["git", "tag", "-s", "-u", "#{gnupg_key_id}", "#{tag_name}", "-m", "\"Version #{@version}\""], env: env)
    end

    # There's no way to create a "draft" tag, so we skip pushing tags if this is a dry run.
    if(!@is_dry_run)
        execute_or_fail("git push origin #{tag_name}")
    end
end

private def delete_release_tag
    execute("git tag -d #{tag_name}")
end

private def release_description
    changelog_entries = changelog_entries_for_version()

    release_body = <<~EOS
        #{changelog_entries}

        See [the changelog for more details](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md).
    EOS
end

private def changelog_entries_for_version
    changelog_contents = File.read("CHANGELOG.md")
    version_lines = changelog_contents.scan(/^(## ([0-9.]+) - [0-9\-]+)$/)

    # line_title is the full line like:
    #
    #     ## 1.2.3 - 2017-10-12
    #
    # line_version is the version in the line like "1.2.3".
    version_lines.each_with_index do |(line_title, line_version), i|
        next if line_version != @version

        # Start at the location of the title PLUS the length of the title so
        # that we can exclude it from the result.
        start_pos = changelog_contents.index(line_title) + line_title.length

        # Get the position of where the next entry starts so that we can scan just to
        # that.
        end_pos = -1 # By default, get everything to the end of the changelog file.
        if i != version_lines.count - 1
          end_pos = changelog_contents.index(version_lines[i + 1][0])
        end

        # Return what we found with all whitespace trimmed.
        return changelog_contents[start_pos...end_pos].strip
    end

    raise ArgumentError.new("Version #{@version} not found in changelog. Have you finished merging the version bump PR?")
end

private def build_example_release_apk
    gnupg_env do |env|
        Subprocess.check_call(
            ['./gradlew', ':paymentsheet-example:assembleRelease'],
            env: env
        )
    end
end

private def upload_example_apk_to_release(release_url)
    octokit_client.upload_asset(
        release_url,
        "../stripe-android/paymentsheet-example/build/outputs/apk/release/paymentsheet-example-release.apk",
        name: "paymentsheet-example-release-#{@version}.apk",
        content_type: "application/vnd.android.package-archive"
    )
end

private def octokit_client
  @octokit_client ||= begin
    # Fetch the per-user secret from password-vault
    token = fetch_password("bindings/gh-tokens/#{ENV['USER']}")
    if token.nil? || token == ""
      raise "Got empty Github token from password-vault"
    end

    Octokit::Client.new(access_token: token)
  end
end

# Gets the GnuPG key ID used to sign tagged commits.
private def gnupg_key_id
  fetch_password("bindings/gnupg/fingerprint").strip
end
