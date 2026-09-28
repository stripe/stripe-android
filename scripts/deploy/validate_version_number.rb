#!/usr/bin/env ruby

require 'octokit'

def validate_version_number_format(version_number)
  part_names = ['major', 'minor', 'patch']
  parts = version_number.split('.')

  unless parts.length() == 3
    abort("Invalid version number. It should consist of a major, minor, and patch number.")
  end

  parts.each_with_index do | part, index |
    if part.start_with?('0') && part.length() > 1
      part_name = part_names[index]
      abort("Invalid version number: #{part_name} number can\'t begin with 0.")
    end
  end
end

def target_version_is_newer(target_version, current_version)
  target_major, target_minor, target_patch = target_version.split('.').map(&:to_i)
  # We tag version numbers to start with "v", so we need to remove that before comparing to the
  # target version.
  current_major, current_minor, current_patch = current_version[1..].split('.').map(&:to_i)

  if target_major < current_major
    false
  elsif target_major > current_major
    true
  elsif target_minor < current_minor
    false
  elsif target_minor > current_minor
    true
  else
    target_patch > current_patch
  end
end

def get_current_version()
  latest_version = Octokit.latest_release("stripe/stripe-android")
  latest_version.tag_name
end

def validate_target_version_is_newer(target_version)
    current_version = get_current_version()
    if !target_version_is_newer(target_version, current_version)
        raise "Expected target version #{target_version} to be newer than #{current_version}. If
         the new version number is intentionally older, pass the `--release-older-version` flag to
         the deploy script."
    end
end

def read_changelog_bump_type()
  changelog_path = File.join(File.dirname(__FILE__), '..', '..', 'CHANGELOG.md')
  content = File.read(changelog_path)

  match = content.match(/^NEXT_VERSION_BUMP:\s*(PATCH|MINOR|MAJOR)\s*$/i)
  if match
    match[1].upcase
  else
    "PATCH"
  end
end

def compute_next_version(current_version, bump_type)
  # current_version starts with "v", e.g. "v23.4.0"
  major, minor, patch = current_version[1..].split('.').map(&:to_i)

  case bump_type
  when "MAJOR"
    "#{major + 1}.0.0"
  when "MINOR"
    "#{major}.#{minor + 1}.0"
  when "PATCH"
    "#{major}.#{minor}.#{patch + 1}"
  else
    abort("Unknown bump type: #{bump_type}")
  end
end

def infer_version_from_changelog()
  bump_type = read_changelog_bump_type()
  current_version = get_current_version()
  next_version = compute_next_version(current_version, bump_type)
  rputs "Inferred next version: #{next_version} (#{bump_type} bump from #{current_version})"
  next_version
end

def validate_version_matches_changelog(version)
  bump_type = read_changelog_bump_type()
  current_version = get_current_version()
  expected_version = compute_next_version(current_version, bump_type)

  if version != expected_version
    rputs "Warning: CHANGELOG.md specifies a #{bump_type} bump (expected #{expected_version}), but you specified #{version}."
    rputs "Do you want to proceed with #{version} anyway? (y/n)"
    response = STDIN.gets.strip.downcase
    unless response == 'y' || response == 'yes'
      abort("Aborting. Update CHANGELOG.md's NEXT_VERSION_BUMP or use --version #{expected_version}.")
    end
  end
end

def validate_version_number()
    validate_version_number_format(@version)
    validate_version_matches_changelog(@version)
    if (!@is_older_version)
        validate_target_version_is_newer(@version)
    end
end
