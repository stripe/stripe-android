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

    module_names.select do |module_name|
        module_folder_path = module_name.sub(":", "/")
        module_build_file_path = "#{module_folder_path}/build.gradle"
        File.exists?(module_build_file_path)
    end
end
