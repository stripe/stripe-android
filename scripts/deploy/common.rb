#!/usr/bin/env ruby

def rputs(string)
    puts string.red
end

def wait_for_user
    rputs "Press enter to continue..."
    STDIN.gets
end

def execute_or_fail(command)
    puts "Executing #{command}..."
    system(command) or raise "Failed to execute #{command}"
end
