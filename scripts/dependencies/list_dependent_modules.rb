#!/usr/bin/env ruby

require 'open3'

def list_dependent_modules
    stdout, _, _ = Open3.capture3("./gradlew -q projects")
    lines = stdout.split("\n")
    lines = lines.select { |line| line.include?("--- Project ") }

    module_names = lines.map do |line|
        matches = line.match /\w+ ':(\S+)'/
        matches[1]
    end

    module_names = filter_module_names(module_names)

    module_names.select do |module_name|
        module_folder_path = module_name.sub(":", "/")
        module_build_file_path = "#{module_folder_path}/build.gradle"
        if RUBY_VERSION.split('.').first == "2"
            File.exists?(module_build_file_path)
        else
            File.exist?(module_build_file_path)
        end
    end
end

def filter_module_names(module_names)
  # Use the select method to filter out names containing "test"
  module_names.reject { |name| name.include?("test") }
end