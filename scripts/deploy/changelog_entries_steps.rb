#!/usr/bin/env ruby

require 'json'
require 'open3'
require 'set'

require_relative 'common'
require_relative 'validate_version_number'

UNRELEASED_HEADER = "## XX.XX.XX - 20XX-XX-XX"
CHANGELOG_PR_PATTERN = /\[[\w]+\]\[(\d+)\]/
CHANGELOG_LLM_MODEL = ENV.fetch("CHANGELOG_LLM_MODEL", "gpt-4o-mini")

CHANGELOG_LLM_SYSTEM_PROMPT = <<~PROMPT
  You generate changelog metadata for the stripe-android SDK release process.

  You will receive a JSON array of pull requests merged since the last release.
  Return exactly one response object per input item, in the same order.

  For each pull request, decide:
  1. Whether it should appear in the public CHANGELOG (`include_changelog_entry`)
  2. Which changelog section it belongs in (`section`)
  3. The changelog bullet text (`message`)
  4. The semantic version impact (`bump_type`: patch, minor, or major)

  Set `include_changelog_entry` to false for changes that should not be user-facing,
  such as CI, tests-only, refactors, dependency-only updates, internal tooling, or docs
  that do not affect SDK consumers.

  When `include_changelog_entry` is true:
  - `section` must be one of the existing SDK areas when possible, e.g. PaymentSheet,
    Payments, Identity, Connect, CryptoOnramp, AddressElement, EmbeddedPaymentElement,
    PaymentMethodMessagingElement, or another concise product/module name.
  - `message` must be the bullet body only, without a leading "*".
    Use this format: `[CHANGE_TYPE][PR_NUMBER](PR_URL) User-facing description.`
    `CHANGE_TYPE` must be one of: ADDED, CHANGED, FIXED, REMOVED, DEPRECATED.
    Write in past tense or declarative style, focused on developer-facing impact.
  - `bump_type` must be one of: patch, minor, major.
    patch: bug fixes and non-breaking internal improvements.
    minor: new backward-compatible APIs or features.
    major: breaking API changes or removals.

  When `include_changelog_entry` is false, still provide `section`, `message`, and `bump_type`.
  Use a reasonable section, a short internal summary in `message`, and the correct `bump_type`.

  Respond with a JSON array only. Each array item must be an object with these keys:
  - include_changelog_entry (boolean)
  - section (string)
  - message (string)
  - bump_type (string: patch, minor, or major)

  Do not include markdown fences or commentary.
PROMPT

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

    responses = generate_changelog_responses_with_llm(commits)
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

private def generate_changelog_responses_with_llm(commits)
    request = commits.map { |commit| changelog_llm_request_item(commit) }
    user_prompt = <<~PROMPT
      Generate changelog responses for these pull requests:

      #{JSON.pretty_generate(request)}
    PROMPT

    puts "Generating changelog entries with llm (#{CHANGELOG_LLM_MODEL}) for #{commits.length} pull #{commits.length == 1 ? 'request' : 'requests'}..."
    stdout, stderr, status = Open3.capture3(
        "llm",
        "-m", CHANGELOG_LLM_MODEL,
        "-s", CHANGELOG_LLM_SYSTEM_PROMPT,
        "--no-stream",
        "-n",
        user_prompt,
    )
    unless status.success?
        message = stderr.strip.empty? ? stdout.strip : stderr.strip
        raise "llm command failed: #{message}"
    end

    llm_response = parse_llm_json(stdout)
    normalize_llm_output(llm_response, commits.length)
end

private def changelog_llm_request_item(commit)
    pr = octokit_client.pull_request("stripe/stripe-android", commit[:pr_number])
    files = octokit_client.pull_request_files("stripe/stripe-android", commit[:pr_number])

    {
        "pr_url" => commit[:pr_url],
        "hash" => commit[:hash],
        "pr_number" => commit[:pr_number].to_s,
        "title" => pr.title,
        "body" => truncate_text(pr.body.to_s, 4000),
        "labels" => pr.labels.map(&:name),
        "changed_files" => files.map(&:filename).first(30),
    }
rescue StandardError => e
    puts "Warning: failed to fetch details for PR ##{commit[:pr_number]}: #{e.message}"
    {
        "pr_url" => commit[:pr_url],
        "hash" => commit[:hash],
        "pr_number" => commit[:pr_number].to_s,
        "fetch_error" => e.message,
    }
end

private def parse_llm_json(stdout)
    trimmed = stdout.strip
    fenced_match = trimmed.match(/\A```(?:json)?\s*(\[.*\])\s*```\z/m)
    json_text = fenced_match ? fenced_match[1] : trimmed

    JSON.parse(json_text)
rescue JSON::ParserError => e
    raise "llm returned invalid JSON: #{e.message}\n#{stdout}"
end

private def normalize_llm_output(llm_response, expected_count)
    items = if llm_response.is_a?(Array)
        llm_response
    elsif llm_response.is_a?(Hash) && llm_response["items"].is_a?(Array)
        llm_response["items"]
    else
        raise "Unexpected llm response shape: #{llm_response.inspect}"
    end

    if items.length != expected_count
        raise "llm returned #{items.length} items, expected #{expected_count}"
    end

    items
end

private def truncate_text(text, max_length)
    return "" if text.nil? || text.empty?
    return text if text.length <= max_length

    "#{text[0, max_length]}..."
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
        raise "llm must return a JSON array"
    end

    if responses.length != commits.length
        raise "llm returned #{responses.length} items for #{commits.length} commits"
    end

    responses.each_with_index do |response, index|
        unless response.is_a?(Hash)
            raise "llm item #{index} must be a JSON object"
        end

        unless [true, false].include?(response["include_changelog_entry"])
            raise "llm item #{index} is missing include_changelog_entry"
        end

        %w[bump_type].each do |field|
            if response[field].nil? || response[field].to_s.strip.empty?
                raise "llm item #{index} is missing #{field}"
            end
        end

        if response["include_changelog_entry"]
            %w[section message].each do |field|
                if response[field].nil? || response[field].to_s.strip.empty?
                    raise "llm item #{index} is missing #{field}"
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
