#!/usr/bin/env ruby

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
    file_path = "#{folder}/dependencies.txt"

    _, _, _ = execute_or_fail("mkdir -p #{folder}")
    _, _, _ = execute_or_fail("./gradlew #{module_name}:dependencies > #{file_path}")
end

puts "✅ Updated dependency files"
