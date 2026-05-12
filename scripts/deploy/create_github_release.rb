#!/usr/bin/env ruby

require_relative 'common'

@release_url = nil

def create_github_release
    tag_exists_locally = local_git_tag_exists?(release_tag_name)

    unless tag_exists_locally || @is_dry_run
        raise "Release tag #{release_tag_name} does not exist locally. Run `git fetch origin --tags` before creating the GitHub release."
    end

    if @is_dry_run && !tag_exists_locally
        rputs "Dry run: local release tag #{release_tag_name} is unavailable. Creating a draft GitHub release without validating a checked-out tag first."
    end

    begin
        release_response = octokit_client.create_release(
          "stripe/stripe-android",
          release_tag_name,
          name: "stripe-android v#{@version}",
          body: release_description,
          draft: @is_dry_run
        )

        @release_url = release_response.url

        rputs "Created new release"
        open_url(release_response.html_url)

        if (@is_dry_run)
            if tag_exists_locally
                rputs "Please verify that deploy release created a draft GitHub release for #{release_tag_name}. It should contain the changelog entries and point at the existing signed tag."
            else
                rputs "Please verify that deploy release created a draft GitHub release for #{release_tag_name}. Because the release source was unavailable, it may be missing tag or source attachments."
            end
            wait_for_user
        end
    rescue StandardError => e
        rputs "Failed to create GitHub release for #{release_tag_name}: #{e.class}: #{e.message}"
        raise
    end
end

def delete_github_release
   if (@release_url != nil)
       puts "Deleting release..."
       deleting_release_succeeded = octokit_client.delete_release("#{@release_url}__")
       if (!deleting_release_succeeded)
           rputs "Deleting release failed! Please manually delete release."
       end
   end
end

private def release_description
    changelog_entries = changelog_entries_for_version()

    release_body = <<~EOS
        #{changelog_entries}

        See [the changelog for more details](https://github.com/stripe/stripe-android/blob/master/CHANGELOG.md).
    EOS
rescue ArgumentError => e
    raise unless @is_dry_run

    rputs "Dry run: #{e.message}"

    <<~EOS
        Dry run: changelog entries for #{@version} are not available in the current checkout.

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
