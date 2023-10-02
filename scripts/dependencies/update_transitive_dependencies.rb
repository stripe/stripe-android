#!/usr/bin/env ruby

require_relative 'generate_dependencies'
require_relative 'list_dependent_modules'

def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

modules = list_dependent_modules

modules.each do |module_name|
    folder = "#{module_name}/dependencies"
    file_path = "#{folder}/dependencies.txt"

    _, _, _ = execute_or_fail("mkdir -p #{folder}")
    execute_or_fail("./gradlew #{module_name}:dependencies > #{file_path}")

    output = File.open(file_path).readlines.map(&:chomp)
    dependencies = generate_dependencies(output)

    # Override the file content with the filtered output
    File.write(file_path, dependencies)
end

puts "✅ Updated dependency files"
