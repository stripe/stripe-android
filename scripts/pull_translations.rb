require 'nokogiri'
require 'set'
require 'fileutils'
# Usage:
# $ ./scripts/pull_translations.rb `fetch-password PhraseApp-access-token`
# (running fetch-password from within this script causes strange dependency issues)
#
# Use this script to bring in new translations instead of pulling all the translations. This
# allows us to have translations in phraseapp without adding them to our codebase.
#
# It will:
# 1. determine which new keys have been added locally
# 2. pull translations from PhraseApp
# 3. generate new .strings files:
#    - any strings not included the default strings.xml are deleted
#    - any existing translations that have changed on PhraseApp will be updated
#    - any new local keys that have translations on PhraseApp will be inserted


def remove_excess_strings(filename, included_strings)
	puts "▸ Removing translations for unused strings"
	strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
	translation_nodes = strings_doc.xpath('//string').to_a
	all_strings = strings_doc.xpath('//string/@name').to_a.to_set {|node_attr| node_attr.text}
 	excess_strings = all_strings - included_strings
 	for translation in translation_nodes
 		if excess_strings.include? translation['name']
 			if translation.previous.previous.comment?
 				if translation.previous.previous.previous.text?
 					translation.previous.previous.previous.unlink
 				end
 				translation.previous.previous.unlink
 			end
 			if translation.previous.text?
 				translation.previous.unlink
 			end
 			translation.unlink
 		end
 	end
	File.write(filename, strings_doc.to_xml)
end

Struct.new("Translation", :translated_string, :description_comment, :string_node)

def make_translations_hash(filename, included_strings)
	strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
	translation_nodes = strings_doc.xpath('//string').to_a
	translations_hash = {}
	for translation in translation_nodes
		translation_comment = nil
		if translation.previous.previous.comment?
			translation_comment = translation.previous.previous
		end
		if included_strings.include? translation['name']
			translations_hash[translation['name']] = Struct::Translation.new(translation.content, translation_comment, translation)
		else
			puts 'not adding' + translation['name']
		end
	end
	return translations_hash
end

def replace_updated_translations(filename, translations_hash)
	puts "▸ Updating strings that have changed"
	strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
	translation_nodes = strings_doc.xpath('//string').to_a
	for translation in translation_nodes
		translation_comment = nil
		if translations_hash.key?(translation['name'])
			key = translation['name']
			if !translation.previous.previous.nil? and translation.previous.previous.comment?
				translation_comment = translation.previous.previous
				if not translation_comment.text.eql? translations_hash[key].description_comment.text
					translation_comment.replace(translations_hash[key].description_comment)
				end
				if not translation.content.eql? translations_hash[key].translated_string
					translation.content = translations_hash[key].translated_string
				end
			end
		end
	end
	File.write(filename, strings_doc.to_xml)
end

def add_new_translations(filename, translations_hash)
	puts "▸ Adding new translations"
	strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
	translation_nodes = strings_doc.xpath('//string')
	existing_strings = strings_doc.xpath('//string/@name').to_a.to_set {|node_attr| node_attr.text}
 	new_strings = translations_hash.keys.to_set - existing_strings
 	new_line_node = translation_nodes.first.next.dup #Grab a new line node to use for formatting
 	last_newline_node = translation_nodes.last.next.unlink # Move the last new line to the end
 	for string in new_strings
 		translation = translations_hash[string]
 		if !translation.description_comment.nil?
 			strings_doc.root.add_child(new_line_node.dup)
	 		strings_doc.root.add_child(translation.description_comment)
 		end
 		strings_doc.root.add_child(new_line_node.dup)
 		strings_doc.root.add_child(translation.string_node)
 	end
 	strings_doc.root.add_child(last_newline_node)
	File.write(filename, strings_doc.to_xml)
end

DEFAULT_STRINGS_FILE = "stripe/res/values/strings.xml"
@langs = ["de", "es", "fr", "it", "ja", "nl", "zh"]

puts "▸ Building set of locally added strings"
default_strings_doc = File.open(DEFAULT_STRINGS_FILE) { |f| Nokogiri::XML(f) }
included_strings = default_strings_doc.xpath('//string/@name').to_a.to_set {|node_attr| node_attr.text}

puts "▸ Downloading all translations from PhraseApp"
`phraseapp pull -t #{ARGV[0]}`

puts "▸ Removing translations for languages we do not yet support"
language_dirs = Dir.glob 'stripe/res/values-*'
for dir in language_dirs
	if not @langs.include? dir.split('-')[1]
		FileUtils.rm_rf(dir) 
	end
end

for lang in @langs
	puts "=================== "
	puts "Working on: " + lang
	puts "=================== "
	downloaded_filename = 'stripe/res/values-'+lang+'/strings-temp.xml'
	translations_hash = make_translations_hash(downloaded_filename, included_strings)
	current_translations_filename = 'stripe/res/values-'+lang+'/strings.xml'
	remove_excess_strings(current_translations_filename, included_strings)
	replace_updated_translations(current_translations_filename, translations_hash)
	add_new_translations(current_translations_filename, translations_hash)
	File.delete(downloaded_filename)
end 
