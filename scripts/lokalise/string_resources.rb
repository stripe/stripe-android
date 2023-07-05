require 'rexml/document'

include REXML

class StringResources

    def fetch
        project_root = File.dirname(Dir.pwd)
        modules = [
            'identity',
            'link',
            'paymentsheet',
            'payments-core',
            'payments-ui-core',
            'stripe-core',
            'stripe-ui-core',

    #         These following modules don't use Lokalise
    #         'camera-core',
    #         'financial-connections',
    #         'stripecardscan',
        ]

        files = modules.map { |mod| "#{project_root}/#{mod}/res/values/strings.xml" }

        key_objects = []

        files.each_with_index.each do |filepath, index|
            file = File.open(filepath).readlines.map(&:chomp).map(&:strip)
            xml_file = File.new(filepath)
            xml_doc = Document.new(xml_file)

            filename = "#{modules[index]}/strings.xml"

            xml_doc.elements.each('resources/string') do |element|
                # Get the main element information
                key_name = element.attributes['name']
                value = element.text

                # Get the line number with this shitty workaround because REXML changes the quotes from
                # double to single quotes.
                line_number = -1

                # One key_name could be a substring of another key_name, so we need to match against
                # this text instead.
                text = "name=\"#{key_name}\""

                file.each_with_index do |line, index|
                    if line.include?(text)
                        line_number = index
                        break
                    end
                end

                if line_number == -1
                    abort("Some weird failure. Bother tillh about it.")
                end

                # Once we have the line of the string, its description is the comment in the line above.
                description = file[line_number - 1]
                if description.start_with?('<!--') && description.end_with?('-->')
                    # Remove the leading <!-- and trailing --> from description
                    description = description.delete_prefix('<!--').delete_suffix('-->').strip
                end

                key_object = {
                    "key_name": key_name,
                    "value": value,
                    "filename": filename,
                    "description": description,
                }

                key_objects << key_object
            end
        end

        key_objects
    end
end
