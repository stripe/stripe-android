#!/usr/bin/env ruby

require 'subprocess'

require_relative 'common'

# Takes a block and yields the absolute path of a ramdisk.
# The ramdisk is automatically deleted after use.
def ramdisk_env
  # If we already have an env, just reuse it
  if @ramdisk_path
    yield(@ramdisk_path)
    return
  end

  ramdisk_utility = File.expand_path('bin/ramdisk', __dir__)
  execute_or_fail("chmod 755 #{ramdisk_utility}")
  ramdisk_name = "stripe_android_deploy_#{Time.now.to_i}"

  # Create a ramdisk
  Subprocess.check_call([ramdisk_utility, 'create', ramdisk_name], env: ENV.to_h.merge(
    "STRIPE_RAMDISK_SIZE_MB" => "50",
  ))
  ramdisk_path = @ramdisk_path = Subprocess.check_output([ramdisk_utility, 'path', ramdisk_name]).strip

  begin
    yield(ramdisk_path)
  ensure
    # Tear down the ramdisk
    @ramdisk_path = nil
    begin
      Subprocess.check_call([ramdisk_utility, 'destroy', ramdisk_name])
    rescue Subprocess::NonZeroExit
      $stderr.puts("Unable to automatically delete ramdisk, you will need to do this manually")
      $stderr.puts("Figure out why it broke, and run 'hdiutil unmount #{ramdisk_path}'")
      # Continue without re-raising as not deleting this won't break
      # the rest of this program.
    end
  end
end

# Takes a block and yields the environment hash to use with
# commands that need to use GnuPG.
#
# This can be preloaded (as in bin/upload_bindings) so that it only
# needs to happen once; it's a relatively noisy and slow operation
# to set the whole thing up.
def gnupg_env
  # If we already have an env, just reuse it
  if @gnupg_env
    yield(@gnupg_env)
    return
  end

  ramdisk_env do |path|
    gnupg_path = File.expand_path('gnupg', path)
    FileUtils.mkdir(gnupg_path)
    FileUtils.chmod(0o700, gnupg_path)

    env = @gnupg_env = ENV.to_hash.update('GNUPGHOME' => gnupg_path)

    begin
      # Set up the GPG agent configuration to allow passphrases
      agent_conf = File.expand_path("gpg-agent.conf", gnupg_path)
      File.open(agent_conf, "w") do |f|
        f.puts("allow-preset-passphrase")
      end

      # Copy the GPG files onto the ramdisk
      pubkey = fetch_password("bindings/gnupg/pubkey")
      privkey = fetch_password("bindings/gnupg/privkey")
      rputs "importing keys"
      import = Subprocess.popen(['gpg', '--import', '--batch'],
        stdin: Subprocess::PIPE,
        env: env)
      import.communicate(pubkey + privkey)

      rputs "verifying we have signing key"
      # Make sure we have the signing key loaded
      Subprocess.check_call(['gpg', '--list-keys', gnupg_key_id],
        env: env)

      rputs "gnupg key id: #{gnupg_key_id}"

      # Determine the keygrip
      output = Subprocess.check_output(%W{gpg --list-secret-keys --batch --with-colons #{gnupg_key_id}},
        env: env)
      lines = output.split("\n").select {|l| l =~ /\A(?:fpr|grp)/}
      keygrips = lines.map {|l| l.split(":")[9]}.each_slice(2)
      keygrip = keygrips.find {|fpr, _| fpr == gnupg_key_id}.last

      rputs "secret key: #{output}"

      gpg_version = Subprocess.check_output(['gpg', '--version'], env: env)
      rputs "gpg version: #{gpg_version}"

      # Set up the passphrase for automatic operation
      passphrase = fetch_password("bindings/gnupg/passphrase").strip

      rputs "passphrase: #{passphrase}"

      # This presets the passphrase without checking any return values.
      # It could fail but probably won't?
      socket_path = File.expand_path('S.gpg-agent', gnupg_path)
      UNIXSocket.open(socket_path) do |s|
        s.recv(4096) # Eat the initial message from the socket
        s.puts("PRESET_PASSPHRASE #{keygrip} -1 #{passphrase.unpack1('H*')}")
      end

      yield(env)
    ensure
      # Stop the GPG agent so we can delete the ramdisk
      Subprocess.call(['gpgconf', '--kill', 'gpg-agent'],
        env: env)
      @gnupg_env = nil
    end
  end
end
