#!/usr/bin/env ruby

def generate_dependencies(lines)
    result = []

    lines.each do |line|
        is_header = line.strip.start_with?("Project ':")
        starts_with_plus = line.strip.include?("+--- ")
        starts_with_minus = line.strip.include?("\\---")

        if (is_header || starts_with_plus || starts_with_minus) && !line.empty?
            result << line
        end
    end

    return result.join("\n")
end
