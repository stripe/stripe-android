#!/usr/bin/env ruby

require 'open3'

def fetch_password(password)
    stdout, stderr, status = Open3.capture3("fetch-password --raw #{password}")

    stdout
end

def rputs(string)
    puts string.red
end

def wait_for_user
    rputs "Press enter to continue..."
    STDIN.gets
end

def execute(command)
    puts "Executing #{command}..."
    system(command)
end

def execute_or_fail(command)
    puts "Executing #{command}..."
    system(command) or raise "Failed to execute #{command}"
end
