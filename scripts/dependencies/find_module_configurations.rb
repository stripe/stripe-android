def find_module_configurations(module_name)
    output, stderr, status = Open3.capture3("./gradlew #{module_name}:printConfigurations --no-configuration-cache 2>&1")

    unless status.success?
        puts "Warning: Failed to get configurations for #{module_name}"
        return {}
    end

    output = output.force_encoding('UTF-8').scrub

    configurations = {}
    output.lines.each do |line|
        if match = line.match(/^\w+ReleaseRuntimeClasspath$/)
            config_name = match[0]
            flavor_name = config_name.sub("ReleaseRuntimeClasspath", "")
            configurations[flavor_name] = config_name
        end
    end

    unless configurations.empty?
        return configurations
    end

    return  { "" => "releaseRuntimeClasspath" }
end
