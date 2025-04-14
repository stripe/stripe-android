#!/usr/bin/env ruby

require 'optparse'

require_relative 'common'

OptionParser.new do |opts|
  opts.on('--user USERNAME',
          'User to give deploy permissions to') do |t|
    @new_user = t
  end
end.parse!

required_passwords = [
    "bindings/gnupg/privkey",
    "bindings/gnupg/passphrase",
    "bindings/java-maven-api-token",
    "nexus-sonatype-login",
]

required_passwords.each do |password|
    execute("add-password-user -n #{@new_user} #{password}")
end
