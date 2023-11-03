#!/usr/bin/env ruby

def generate_dependencies(lines)
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
