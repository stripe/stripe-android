class DiffProvider

    def determine_unsynced_keys(key_objects, remote_android_keys)
        unsynced_keys = []
        key_objects.each do |key|
            key_name = key[:key_name]

            if !remote_android_keys.include?(key_name)
                unsynced_keys << key
            end
        end
        unsynced_keys
    end

    def find_existing_key(all_keys, key_object)
        value = key_object[:value]
        all_keys.each do |key|
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
