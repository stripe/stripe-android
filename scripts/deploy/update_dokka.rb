def generate_dokka(version)
    # switch to a branch, run
    run_target_library_git(['checkout', '-b', "generateDokkaFor#{version}"])
    puts '> Generating dokka. The diff will be displayed in your browser, where you will need to create a pull request'
    puts '> To abort, delete the commit message or ctrl-c this script.'
    # run the process to update kotlin doc
    # ./gradlew dokkaHtmlMultiModule
    Dir.chdir(target_library) do
    Subprocess.check_call(['rm', '-rf', 'docs/'])
    Subprocess.check_call(['./gradlew', 'clean'])
    Subprocess.check_call(['./gradlew', 'dokkaHtmlMultiModule'])
    end
    # create a pull request
    run_target_library_git(['add', 'docs/*'])
    run_target_library_git(['commit', '-avem' "Generate dokka for #{version}"])
    puts '> Pushing'
    run_target_library_git(['push', 'origin', 'HEAD'])
    # open the browser to create a pull request
    puts '> Opening github with Dokka changes. These changes should be merged only after the deployment is complete.'
    Subprocess.check_call(['open', "https://github.com/stripe/stripe-android/compare/generateDokkaFor#{version}"])
    # Return to master
    run_target_library_git(['checkout', 'master'])
end