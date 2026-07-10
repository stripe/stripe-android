#!/usr/bin/env ruby

require 'json'
require 'open3'
require 'set'

require_relative 'common'
require_relative 'validate_version_number'

UNRELEASED_HEADER = "## XX.XX.XX - 20XX-XX-XX"
CHANGELOG_PR_PATTERN = /\[[\w]+\]\[(\d+)\]/

def generate_changelog_entries
    current_version = File.read("VERSION").strip
    release_tag = "v#{current_version}"

    execute_or_fail("git fetch --tags origin")
    unless local_git_tag_exists?(release_tag) || remote_git_tag_exists?(release_tag)
        raise "Release tag #{release_tag} not found locally or on origin"
    end
    unless local_git_tag_exists?(release_tag)
        execute_or_fail("git fetch origin tag #{release_tag}")
    end

    commits = commits_since_release(release_tag)
    if commits.empty?
        rputs "No commits since #{release_tag}; skipping changelog generation."
        return
    end

    existing_pr_numbers = changelog_pr_numbers_in_unreleased(current_version)
    commits = commits.reject { |commit| existing_pr_numbers.include?(commit[:pr_number]) }

    if commits.empty?
        rputs "All commits since #{release_tag} already have changelog entries; skipping changelog generation."
        return
    end

    commits = filter_changelog_commits(commits)
    if commits.empty?
        rputs "No commits remaining after applying changelog entry filters; skipping changelog generation."
        return
    end

    responses = generate_changelog_responses(commits)
    validate_changelog_entry_responses!(commits, responses)

    bump_type = read_changelog_bump_type()
    validate_bump_types!(bump_type, responses)

    entries_to_add = commits.zip(responses).filter_map do |commit, response|
        next unless response["include_changelog_entry"]

        response.merge("pr_number" => commit[:pr_number])
    end

    if entries_to_add.empty?
        rputs "No new changelog entries to add."
        return
    end

    updated_changelog = apply_changelog_updates(
        File.read("CHANGELOG.md"),
        current_version,
        entries_to_add
    )
    File.write("CHANGELOG.md", updated_changelog)
    rputs "Added #{entries_to_add.length} changelog #{entries_to_add.length == 1 ? 'entry' : 'entries'} to the unreleased section."
end

private def commits_since_release(release_tag)
    stdout, stderr, status = Open3.capture3("git", "log", "#{release_tag}..HEAD", "--format=%H")
    raise("Failed to list commits since #{release_tag}: #{stderr}") unless status.success?

    seen_pr_numbers = Set.new
    stdout.split("\n").filter_map do |hash|
        pr = pr_for_commit(hash)
        if pr.nil?
            puts "Skipping commit #{hash[0, 7]}: no associated pull request found."
            next
        end

        pr_number = pr.number
        if seen_pr_numbers.include?(pr_number)
            puts "Skipping commit #{hash[0, 7]}: PR ##{pr_number} already included."
            next
        end

        seen_pr_numbers.add(pr_number)
        {
            hash: hash,
            pr_number: pr_number,
            pr_url: pr.html_url,
        }
    end
end

private def pr_for_commit(sha)
    prs = octokit_client.commit_pulls("stripe/stripe-android", sha)
    return nil if prs.nil? || prs.empty?

    merged_prs = prs.select { |pr| pr.merged_at }
    (merged_prs.max_by(&:merged_at) || prs.first)
end

private def changelog_entry_filters_path
    File.join(File.dirname(__FILE__), "changelog_entry_filters.json")
end

private def read_changelog_entry_filters
    unless File.exist?(changelog_entry_filters_path)
        return { "labels" => [], "terms" => [] }
    end

    filters = JSON.parse(File.read(changelog_entry_filters_path))
    {
        "labels" => Array(filters["labels"]).map(&:to_s).reject(&:empty?),
        "terms" => Array(filters["terms"]).map(&:to_s).reject(&:empty?),
    }
end

private def filter_changelog_commits(commits)
    filters = read_changelog_entry_filters()
    label_filters = filters["labels"]
    term_filters = filters["terms"]

    if label_filters.empty? && term_filters.empty?
        return commits
    end

    commits.reject do |commit|
        pr = octokit_client.pull_request("stripe/stripe-android", commit[:pr_number])
        pr_labels = pr.labels.map(&:name)

        matching_label = label_filters.find { |label| pr_labels.include?(label) }
        if matching_label
            puts "Skipping PR ##{commit[:pr_number]}: matched filter label '#{matching_label}'."
            next true
        end

        pr_text = "#{pr.title}\n#{pr.body}".downcase
        matching_term = term_filters.find { |term| pr_text.include?(term.downcase) }
        if matching_term
            puts "Skipping PR ##{commit[:pr_number]}: matched filter term '#{matching_term}'."
            next true
        end

        false
    end
end

private def changelog_entry_generator_script
    File.join(File.dirname(__FILE__), "changelog_entry_generator.sh")
end

private def generate_changelog_responses(commits)
    script = changelog_entry_generator_script
    commits.map.with_index do |commit, index|
        puts "Generating changelog entry #{index + 1}/#{commits.length} for PR ##{commit[:pr_number]} (#{commit[:hash][0, 7]})..."
        stdout, stderr, status = Open3.capture3("bash", script, commit[:hash], commit[:pr_number].to_s)
        unless status.success?
            message = stderr.strip.empty? ? stdout.strip : stderr.strip
            raise "changelog_entry_generator.sh failed for commit #{commit[:hash][0, 7]}: #{message}"
        end

        parse_changelog_entry_json(stdout)
    end
end

private def parse_changelog_entry_json(stdout)
    trimmed = stdout.strip
    fenced_match = trimmed.match(/\A```(?:json)?\s*(\{.*\})\s*```\z/m)
    json_text = fenced_match ? fenced_match[1] : trimmed

    JSON.parse(json_text)
rescue JSON::ParserError => e
    raise "changelog_entry_generator.sh returned invalid JSON: #{e.message}\n#{stdout}"
end

private def changelog_pr_numbers_in_unreleased(current_version)
    content = File.read("CHANGELOG.md")
    section = unreleased_changelog_section(content, current_version)
    section.scan(CHANGELOG_PR_PATTERN).flatten.map(&:to_i).to_set
end

private def unreleased_changelog_section(content, current_version)
    bump_match = content.match(/^NEXT_VERSION_BUMP:\s*(?:PATCH|MINOR|MAJOR)\s*$/i)
    raise "Could not find NEXT_VERSION_BUMP in CHANGELOG.md" unless bump_match

    release_match = content.match(/^## #{Regexp.escape(current_version)} - /m)
    raise "Could not find released version #{current_version} in CHANGELOG.md" unless release_match

    content[bump_match.end(0)...release_match.begin(0)]
end

private def validate_changelog_entry_responses!(commits, responses)
    unless responses.is_a?(Array)
        raise "changelog entry generator must return one JSON object per commit"
    end

    if responses.length != commits.length
        raise "changelog entry generator returned #{responses.length} items for #{commits.length} commits"
    end

    responses.each_with_index do |response, index|
        unless response.is_a?(Hash)
            raise "changelog entry generator item #{index} must be a JSON object"
        end

        unless [true, false].include?(response["include_changelog_entry"])
            raise "changelog entry generator item #{index} is missing include_changelog_entry"
        end

        %w[bump_type].each do |field|
            if response[field].nil? || response[field].to_s.strip.empty?
                raise "changelog entry generator item #{index} is missing #{field}"
            end
        end

        if response["include_changelog_entry"]
            %w[section message].each do |field|
                if response[field].nil? || response[field].to_s.strip.empty?
                    raise "changelog entry generator item #{index} is missing #{field}"
                end
            end
        end
    end
end

private def validate_bump_types!(bump_type, responses)
    bump_types = responses.map { |response| response["bump_type"].downcase }
    case bump_type.upcase
    when "PATCH"
        invalid = bump_types.reject { |type| type == "patch" }
        unless invalid.empty?
            raise "NEXT_VERSION_BUMP is PATCH, but found non-patch changes: #{invalid.uniq.join(', ')}"
        end
    when "MINOR"
        if bump_types.include?("major")
            raise "NEXT_VERSION_BUMP is MINOR, but found major changes"
        end
        unless bump_types.include?("minor")
            raise "NEXT_VERSION_BUMP is MINOR, but no minor changes were found. Use PATCH or MAJOR instead."
        end
    when "MAJOR"
        unless bump_types.include?("major")
            raise "NEXT_VERSION_BUMP is MAJOR, but no major changes were found. Use MINOR or PATCH instead."
        end
    else
        raise "Unknown bump type: #{bump_type}"
    end
end

private def apply_changelog_updates(content, current_version, entries)
    region = unreleased_region(content, current_version)
    unreleased = region[:text].dup

    entries.group_by { |entry| entry["section"] }.each do |section, section_entries|
        unreleased = append_to_unreleased_section(
            unreleased,
            section,
            section_entries.map { |entry| entry["message"] }
        )
    end

    content[0...region[:start]] + unreleased + content[region[:end]..]
end

private def unreleased_region(content, current_version)
    header_match = content.match(/#{Regexp.escape(UNRELEASED_HEADER)}\s*\n/)
    raise "Could not find unreleased header #{UNRELEASED_HEADER} in CHANGELOG.md" unless header_match

    release_match = content.match(/^## #{Regexp.escape(current_version)} - /m)
    raise "Could not find released version #{current_version} in CHANGELOG.md" unless release_match

    start_pos = header_match.end(0)
    end_pos = release_match.begin(0)
    { start: start_pos, end: end_pos, text: content[start_pos...end_pos] }
end

private def append_to_unreleased_section(text, section, messages)
    header = "### #{section}"
    formatted_entries = messages.map { |message| format_changelog_entry(message) }.join

    unless text.include?(header)
        return text.rstrip + "\n\n#{header}\n#{formatted_entries}"
    end

    header_idx = text.index(header)
    after_header = header_idx + header.length
    remainder = text[after_header..]
    next_section_match = remainder.match(/\n### /)

    insert_pos = if next_section_match
        after_header + next_section_match.begin(0)
    else
        text.length
    end

    text[0...insert_pos] + formatted_entries + text[insert_pos..]
end

private def format_changelog_entry(message)
    line = message.strip
    line = "* #{line}" unless line.start_with?("*")
    "#{line}\n"
end
