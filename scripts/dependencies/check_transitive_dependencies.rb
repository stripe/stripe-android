#!/usr/bin/env ruby

require_relative 'check_module_dependencies'
require_relative 'list_dependent_modules'

modules = list_dependent_modules
has_changes = false

modules.each do |module_name|
    has_changes = check_module_dependencies(module_name)
    if has_changes
        break
    end
end

exit has_changes
