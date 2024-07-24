#!/usr/bin/env ruby

require_relative 'common'

def generate_dokka()
    dokka_change_description = "Generate dokka for #{@version}"

    begin
        switch_to_new_branch(dokka_branch_name)
        execute_or_fail("rm -rf docs/")
        execute_or_fail("./gradlew clean")
        execute_or_fail("./gradlew dokkaHtmlMultiModule")

        execute_or_fail("git add docs/*")
        execute_or_fail("git commit -m \"#{dokka_change_description}\"")
    rescue
        execute("git restore --staged docs") # unstage any staged docs files
        execute("git clean -fd docs") # remove any newly added docs files
        execute("git restore docs") # revert any existing docs files
        raise
    end

    begin
        execute_or_fail("git push -u origin")

        create_pr(
            dokka_branch_name,
            dokka_change_description,
            dokka_change_description,
            "Send the dokka PR for review and enable auto-merge. Then, continue with the next steps."
        )
    rescue
        revert_dokka_changes
        raise
    end

    execute_or_fail("git checkout #{@deploy_branch}")
end

def revert_dokka_changes()
    delete_git_branch(dokka_branch_name)
end

private def dokka_branch_name
    "generateDokkaFor#{@version}"
end
