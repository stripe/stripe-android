#!/usr/bin/env ruby

require 'open3'

diff_command = 'git status --porcelain paymentsheet-example/screenshots/debug'
stdout, _, _ = Open3.capture3(diff_command)

if stdout.empty?
    puts "No differences in screenshots found."
    exit true
end

artifacts_link = "#{BITRISE_BUILD_URL}?tab=artifacts"
puts "Screenshot tests failed.\n\n#{stdout}\n\nUpdated screenshots here:"
puts artifacts_link
exit false
