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
            # TODO: Enable these
            success = if @type == "create"
                true
                # lokalise_client.create_key(@key)
            else
                false
                # lokalise_client.update_key(@existing_key, @key)
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
            @existing_key['key_name']['android']
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

            is_new_android_key = !lokalise_android_keys.include?(key_name)

            if is_new_android_key
                existing_key = find_existing_key(value)

                if existing_key.nil?
                    unsynced_keys << Action.new("create", key, nil)
                else
                    unsynced_keys << Action.new("update", key, existing_key)
                end
            end
        end

        unsynced_keys
    end

    private

    def filter_android_key_names(keys)
        android_keys = keys.select { |key| key['platforms'].include? 'android' }
        android_keys.map { |key| key['key_name']['android'] }
    end

    def find_existing_key(value)
        @all_keys.each do |key|
            translations = key['translations']

            translations.each do |translation|
                # If we find a translation that matches the key_object's value, then this is the key
                # that we need to update
                if translation['language_iso'] == 'en' && translation['translation'] == value
                    return key
                end
            end
        end
        return nil
    end
end
