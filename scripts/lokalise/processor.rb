class Processor

    class Action

        attr_accessor :type
        attr_accessor :key
        attr_accessor :existing_key

        def initialize(type, key, existing_key)
            @type = type
            @key = key
            @existing_key = existing_key
        end

        def description
            if @type == "create"
                "Create new Lokalise entry with key \"#{key_name}\""
            else
                "Update existing Lokalise entry \"#{ios_key_name}\" with new Android key \"#{key_name}\""
            end
        end

        def perform(lokalise_client)
            success = if @type == "create"
                lokalise_client.create_key(@key)
            else
                lokalise_client.update_key(@existing_key, @key)
            end

            message = success ? success_message : failure_message
        end

        private

        def success_message
            if @type == "create"
                "Created new Lokalise entry with key \"#{key_name}\""
            else
                "Updated existing Lokalise entry \"#{ios_key_name}\" with new Android key \"#{key_name}\""
            end
        end

        def failure_message
            if @type == "create"
                "Failed to create Lokalise entry with key \"#{key_name}\""
            else
                "Failed to update existing Lokalise entry \"#{ios_key_name}\" with new Android key \"#{key_name}\""
            end
        end

        def key_name
            @key[:key_name]
        end

        def ios_key_name
            @existing_key['key_name']['ios']
        end
    end

    def initialize(all_keys)
        @all_keys = all_keys
    end

    def determine_required_actions(local_keys)
        lokalise_android_keys = filter_android_key_names(@all_keys)
        unsynced_keys = []

        local_keys.each do |key|
            key_name = key[:key_name]
            value = key[:value]

            is_new_android_key = determine_if_new_key(key, lokalise_android_keys)

            if is_new_android_key
                existing_key = find_existing_key(value)

                if existing_key.nil? || key_name != existing_key[:key]
                    unsynced_keys << Action.new("create", key, nil)
                else
                    unsynced_keys << Action.new("update", key, existing_key)
                end
            else
                lokalise_entry = find_lokalise_entry_for_key(key_name)
                is_missing_filename = lokalise_entry['filenames']['android'].empty?
                needs_filename_update = lokalise_entry['filenames']['android'] != key[:filename]

                if is_missing_filename || needs_filename_update
                    unsynced_keys << Action.new("update", key, lokalise_entry)
                end
            end
        end

        unsynced_keys
    end

    private

    def determine_if_new_key(local_key, remote_keys)
        local_key_name = local_key[:key_name]
        local_filename = local_key[:filename]

        remote_keys.each do |remote_key|
            if remote_key[:key_name] == local_key_name
                return false
            end
        end

        return true
    end

    def filter_android_key_names(keys)
        android_keys = keys.select { |key| key['platforms'].include? 'android' }

        android_keys.map do |key|
            key_name = key['key_name']['android']
            filename = key['filenames']['android']

            {
                "key_name": key_name,
                "filename": filename
            }
        end
    end

    def find_existing_key(value)
        escaped_value = escape_for_lokalise(value)

        @all_keys.each do |key|
            translations = key['translations']

            translations.each do |translation|
                # If we find a translation that matches the key_object's value, then this is the key
                # that we need to update
                language = translation['language_iso']
                content = translation['translation']

                if language == 'en' && (content == value || content == escaped_value)
                    return key
                end
            end
        end

        return nil
    end

    def find_lokalise_entry_for_key(local_key)
        @all_keys.find { |key| key['key_name']['android'] == local_key }
    end

    def escape_for_lokalise(value)
        value
            # Wrap any placeholders in square brackets
            .gsub("%s", "[%s]")
            # Wrap any numbered placeholders such as ""%1$s" in square brackets
            .gsub(/[%]*[0-9]*[$][s]/) { |value| "[#{value}]" }
            # Remove escaping characters
            .gsub("\\", "")
    end
end
