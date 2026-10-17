#!/usr/bin/env ruby

require 'optparse'

require_relative 'common'
require_relative 'validate_version_number'

def parse_release_options!(flow_name:, version_required: false)
    @flow_name = flow_name
    @step_index = 1
    @is_dry_run = false
    @deploy_branch = 'master'
    @is_older_version = false

    OptionParser.new do |opts|
        opts.on('--version VERSION',
                'Version to release (e.g. 21.2.0)') do |t|
            @version = t
        end

        opts.on('--continue-from NUMBER',
                'Continue from a specified step within this release flow') do |t|
            @step_index = t.to_i
        end

        opts.on('--dry-run', "Don't do a real deployment, but test what would happen if you did") do |t|
            @is_dry_run = t
        end

        opts.on('--branch BRANCH', "Branch to deploy from") do |t|
            @deploy_branch = t
        end

        opts.on('--release-older-version', "Indicates you are not releasing the newest version of stripe-android.") do |t|
            @is_older_version = t
        end
    end.parse!

    if @version.nil?
        if version_required
            raise ArgumentError, "--version is required for #{flow_name}"
        end
        @version = infer_version_from_changelog()
    end
end

def execute_steps(steps, before_resume: nil)
    step_count = steps.length
    step_index = @step_index

    if step_index < 1 || step_index > step_count
        raise ArgumentError, "--continue-from must be between 1 and #{step_count}"
    end

    begin
        if step_index > 1
            before_resume.call(step_index) if before_resume
            steps = steps.drop(step_index - 1)
            rputs "Continuing #{@flow_name} from step #{step_index}: #{steps.first.name}"
        end

        steps.each do |step|
            rputs "# #{step.name} (step #{step_index}/#{step_count})"
            step.call
            step_index += 1
        end
    rescue SystemExit, Interrupt
        raise
    rescue Exception
        command = "./scripts/deploy/#{File.basename($PROGRAM_NAME)}"
        rputs "Restart #{@flow_name} with `#{command} --continue-from #{step_index} --version #{@version}` to re-run from this step."
        raise
    end
end
