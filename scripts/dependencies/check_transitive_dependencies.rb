#!/usr/bin/env ruby

require 'open3'

diff_command = 'git status --porcelain */dependencies/dependencies.txt'
stdout, _, _ = Open3.capture3(diff_command)

if stdout.empty?
    puts "No differences in dependencies found."
    exit true
end

bitrise_build_url = ENV["BITRISE_BUILD_URL"]
artifacts_link = "#{bitrise_build_url}?tab=artifacts"
puts "Dependencies check failed.\n\n#{stdout}\n\nUpdated dependencies here:"
puts artifacts_link
puts "\n\nYou can also regenerate them locally via `ruby scripts/dependencies/update_transitive_dependencies.rb`"
exit false
