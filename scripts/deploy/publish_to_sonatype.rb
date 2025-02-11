#!/usr/bin/env ruby

require 'nokogiri'

require_relative 'common'
require_relative 'gnupg_utils'

def publish_to_sonatype
    # Must be run on the deploy branch, because it depends on changes made in
    # create_version_bump_pr (updating the VERSION file)
    execute_or_fail("git checkout #{@deploy_branch}")
    execute_or_fail("git pull")

    set_gpg_location()

    begin
        m2_settings = Nokogiri::XML(fetch_password("bindings/java-maven-api-token"))

        # Required in order to use xpath sanely (the commands below would
        # otherwise come up with no results because the top level `<settings>`
        # has an `xmlns`.
        m2_settings.remove_namespaces!

        # Note that we're looking for the "sonatype-nexus-staging" server here.
        # Currently hard-coded to the second <server> element in the array but
        # there's got to be a better way (I tried a few clever xpath tricks, but
        # Nokogiri didn't like them one bit)..
        server_element = m2_settings.xpath("//settings/servers/server")[1]

        nexus_user = server_element.xpath("//username").first.content
        nexus_pass = server_element.xpath("//password").first.content
        gnupg_key_id = fetch_password("bindings/gnupg/fingerprint").strip

        gradle_opts = \
        "-Dorg.gradle.project.NEXUS_USERNAME=#{nexus_user} " \
        "-Dorg.gradle.project.NEXUS_PASSWORD=#{nexus_pass} " \
        "-Dorg.gradle.project.signing.gnupg.keyName=#{gnupg_key_id}"

        gnupg_env do |env|
            env.update(
                "GRADLE_OPTS" => gradle_opts,

                # Ensure that /usr/bin is in the PATH. This is
                # where the Java executable is located on a Mac
                # OS machine.
                "PATH" => path_with("/usr/bin"),
            )

            # See https://github.com/gradle-nexus/publish-plugin for info on these gradle commands.
            if (@is_dry_run)
                publish_to_sonatype_commands = ['./gradlew', 'publishToSonatype', 'closeSonatypeStagingRepository', '--stacktrace']
            else
                publish_to_sonatype_commands = ['./gradlew', 'publishToSonatype', 'closeAndReleaseSonatypeStagingRepository', '--stacktrace']
            end

            Subprocess.check_call(
                publish_to_sonatype_commands,
                env: env
            )

            puts "Release succeeded!"

            if (@is_dry_run)
                rputs "At the open link, verify that a new release was added to the staging repo. You can log in to sonatype using your credentials found at `fetch-password bindings/java-maven-api-token`"
                open_url("https://oss.sonatype.org/#stagingRepositories")
                wait_for_user
            end
        ensure
            reset_gradle_properties()
        end
    end
end

private def set_gpg_location
    begin
        gpg_location = `which gpg`
    rescue
        rputs "Could not find gpg location via `which gpg`, do you have gpg installed?"
    end
    append_to_file("gradle.properties", "signing.gnupg.executable=#{gpg_location}")
end

private def reset_gradle_properties
    execute_or_fail("git checkout origin/master gradle.properties")
end

# Gets the contents of the PATH environment variable, but before it does
# ensures that it includes the given `path_to_include` path. So for example
# we might request $PATH, but make sure that it includes `/usr/bin'.
private def path_with(path_to_include)
    path = ENV["PATH"] || ""
    path = path.split(":")
    path << path_to_include unless path.include?(path_to_include)
    path.join(":")
end
