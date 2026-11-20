#!/usr/bin/env ruby

require 'open3'

diff_command = 'git status --porcelain */dependencies/*dependencies.txt'
stdout, _, _ = Open3.capture3(diff_command)

if stdout.empty?
    puts "No differences in dependencies found."
    exit true
end

puts "Dependencies check failed.\n\n#{stdout}\n\nSee the dependency baseline changes in the build's user files."
puts "\n\nYou can also regenerate them locally via `ruby scripts/dependencies/update_transitive_dependencies.rb`"
exit false
