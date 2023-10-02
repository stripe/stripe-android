#!/usr/bin/env ruby

require 'fileutils'

def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

modules = [
    'identity',
    'link',
    'paymentsheet',
    'payments-core',
    'payments-ui-core',
    'stripe-core',
    'stripe-ui-core',
    'camera-core',
    'financial-connections',
    'stripecardscan',
]

modules.each do |module_name|
    folder = "#{module_name}/dependencies"
    original_path = "#{folder}/dependencies.txt"
    updated_path = "#{folder}/new_dependencies.txt"

    _, _, _ = execute_or_fail("./gradlew #{module_name}:dependencies > #{updated_path}")

    unless File.file?(original_path)
        abort("No dependencies file found for \"#{module_name}\". Run `ruby scripts/update_transitive_dependencies.rb` to generate it.")
    end

    original = File.open(original_path).read
    updated = File.open(updated_path).read

    unchanged = FileUtils.compare_file(original_path, updated_path)

    if unchanged
        abort("Dependencies for #{module_name} have changed. Make sure this is an intended or harmless change; if so, run `ruby scripts/update_transitive_dependencies.rb` to generate it.")
    end
end
