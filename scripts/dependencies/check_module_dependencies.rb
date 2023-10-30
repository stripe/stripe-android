#!/usr/bin/env ruby

require 'open3'

require_relative 'generate_dependencies'

def check_module_dependencies(module_name)
    folder = "#{module_name}/dependencies"
    file_path = "#{folder}/dependencies.txt"

    unless File.file?(file_path)
        puts "⛔️ No dependencies file found for \"#{module_name}\". Run `ruby scripts/dependencies/update_transitive_dependencies.rb` to generate it."
        exit false
    end

    current_dependencies = File.read(file_path)
    new_dependencies = generate_dependencies(module_name)

    current_dependencies != new_dependencies
end
