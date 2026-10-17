#!/usr/bin/env ruby

require 'open3'

require_relative 'find_module_configurations'
require_relative 'generate_dependencies'
require_relative 'list_dependent_modules'

def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

modules = list_dependent_modules

modules.each do |module_name|
    folder = "#{module_name}/dependencies"

    _, _, _ = execute_or_fail("mkdir -p #{folder}")

    configurations = find_module_configurations(module_name)

    configurations.each do |flavor, configuration|
        dependencies = generate_dependencies(module_name, configuration)

        flavor_path = flavor.empty? ? "" : "#{flavor}-"
        file_path = "#{folder}/#{flavor_path}dependencies.txt"

        File.write(file_path, dependencies)
    end
end
