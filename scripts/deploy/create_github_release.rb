#!/usr/bin/env ruby

private def get_changelog_entries_for_version
    changelog_contents = File.read("CHANGELOG.md")
    version_lines = changelog_contents.scan(/^(## ([0-9.]+) - [0-9\-]+)$/)

    # line_title is the full line like:
    #
    #     ## 1.2.3 - 2017-10-12
    #
    # line_version is the version in the line like "1.2.3".
    res.each_with_index do |(line_title, line_version), i|
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

    raise ArgumentError.new("version #{@version} not found in changelog")
end
