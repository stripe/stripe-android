#!/usr/bin/env ruby

require_relative 'common'

def generate_dokka()
    if (@is_older_version)
        rputs "Skipping updating dokka because this release is for an older version."
        return
    end

    dokka_change_description = "Generate dokka for #{@version}"

    begin
        execute_or_fail("rm -rf docs/")
        execute_or_fail("./gradlew clean")
        execute_or_fail("./gradlew dokkaHtmlMultiModule")

        if docs_did_not_change
            rputs "Skipping updating dokka because there are no dokka changes."
            return
        end

        switch_to_new_branch(dokka_branch_name, @deploy_branch)
        execute_or_fail("git add -A docs/*")
        execute_or_fail("git commit -m \"#{dokka_change_description}\" --no-verify")
    rescue
        execute("git restore --staged docs") # unstage any staged docs files
        execute("git clean -fd docs") # remove any newly added docs files
        execute("git restore docs") # revert any existing docs files
        raise
    end

    begin
        execute_or_fail("git push -u origin")

        if (@is_dry_run)
            user_message = "Verify that a draft PR was opened containing only docs updates."
        else
            user_message = "Send the dokka PR for review and enable auto-merge. Then, continue with the next steps."
        end

        create_pr(
            dokka_branch_name,
            dokka_change_description,
            dokka_change_description,
            user_message
        )
    rescue
        revert_dokka_changes
        raise
    end

    execute_or_fail("git checkout #{@deploy_branch}")
end

def revert_dokka_changes()
    delete_git_branch(dokka_branch_name, @deploy_branch)
end

private def dokka_branch_name
    "generateDokkaFor#{@version}"
end

private def docs_did_not_change
    stdout, stderr, status = Open3.capture3("git status --porcelain docs/*")

    return stdout.empty?
end
