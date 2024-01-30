#!/usr/bin/env ruby

require 'open3'

def generate_dependencies(module_name)
    output, _, _ = Open3.capture3("./gradlew #{module_name}:dependencies --configuration releaseRuntimeClasspath")
    lines = output.lines.map(&:chomp)

    result = []

    lines.each do |line|
        starts_with_plus = line.strip.include?("+--- ")
        starts_with_minus = line.strip.include?("\\---")

        if (starts_with_plus || starts_with_minus) && !line.empty?
            result << line
        end
    end

    return result.join("\n")
end
