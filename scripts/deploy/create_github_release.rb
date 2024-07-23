#!/usr/bin/env ruby

require 'octokit'
require 'subprocess'

require_relative 'common'
require_relative 'gnupg_utils'

@release_url = nil

# TODO: need to add step to make ramdisk executable. chmod 755 ramdisk
def create_github_release
    # execute_or_fail("git checkout #{@branch}")
    tag_release

    # TODO: generate assets and attach them to release -- before tagging? idk
    release_response = octokit_client.create_release(
      "stripe/stripe-android",
      "v#{@version}",
      name: "stripe-android v#{@version}",
      body: release_description,
      draft: @is_dry_run
    )

    release_url = release_response[url]
end

def delete_github_release
   delete_release_tag

   if (@release_url != nil)
       rputs "Deleting release..."
       octokit_client.delete_release(@release_url)
   end
end

private def tag_name
    "v#{@version}"
end

private def tag_release
    gnupg_env do |env|
        Subprocess.check_call(["git", "tag", "-s", "-u", "#{gnupg_key_id}", "#{tag_name}", "-m", "\"Version #{@version}\""], env: env)
    end

    if(!@is_dry_run)
        # There's no way to create a "draft" tag, so we skip pushing tags if this is a dry run.
        execute_or_fail("git push --tags")
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
        start_pos = contents.index(line_title) + line_title.length

        # Get the position of where the next entry starts so that we can scan just to
        # that.
        end_pos = -1 # By default, get everything to the end of the changelog file.
        if i != version_lines.count - 1
          end_pos = contents.index(version_lines[i + 1][0])
        end

        # Return what we found with all whitespace trimmed.
        return contents[start_pos...end_pos].strip
    end

    # TODO: this is failing because I haven't yet moved over the changelog update into this branch
    raise ArgumentError.new("version #{@version} not found in changelog")
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
