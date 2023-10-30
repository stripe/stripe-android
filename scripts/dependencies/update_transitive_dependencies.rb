#!/usr/bin/env ruby

require 'open3'

require_relative 'generate_dependencies'
require_relative 'list_dependent_modules'

def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

modules = list_dependent_modules

modules.each do |module_name|
    folder = "#{module_name}/dependencies"
    file_path = "#{folder}/dependencies.txt"

    unless File.file?(file_path)
        abort("⛔️ No dependencies file found for \"#{module_name}\". Run `ruby scripts/dependencies/update_transitive_dependencies.rb` to generate it.")
    end

    _, _, _ = execute_or_fail("mkdir -p #{folder}")
    dependencies = generate_dependencies(module_name)

    File.write(file_path, dependencies)
end
