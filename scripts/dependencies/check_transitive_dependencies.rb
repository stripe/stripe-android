#!/usr/bin/env ruby

require_relative 'generate_dependencies'
require_relative 'list_dependent_modules'

require 'fileutils'
require 'open3'

def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

modules = list_dependent_modules

modules.each do |module_name|
    folder = "#{module_name}/dependencies"
    original_path = "#{folder}/dependencies.txt"
    updated_path = "#{folder}/new_dependencies.txt"

    execute_or_fail("./gradlew #{module_name}:dependencies > #{updated_path}")

    output = File.open(updated_path).readlines.map(&:chomp)
    dependencies = generate_dependencies(output)

    # Override the file content with the filtered output
    File.write(updated_path, dependencies)

    unless File.file?(original_path)
        abort("No dependencies file found for \"#{module_name}\". Run `ruby scripts/dependencies/update_transitive_dependencies.rb` to generate it.")
    end

    original = File.open(original_path).read
    updated = File.open(updated_path).read

    unchanged = FileUtils.compare_file(original_path, updated_path)
    File.delete(updated_path)

    if unchanged
        abort("Dependencies for #{module_name} have changed. Make sure this is an intended or harmless change; if so, run `ruby scripts/dependencies/update_transitive_dependencies.rb` to generate it.")
    end
end
