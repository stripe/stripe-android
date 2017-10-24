require 'nokogiri'
require 'set'
require 'fileutils'
# Usage:
# $ ./scripts/pull_translations.rb `fetch-password PhraseApp-access-token`
# (running fetch-password from within this script causes strange dependency
# issues)
#
# Use this script to bring in new translations instead of pulling all the
# translations.
# This allows us to have translations in phraseapp without adding them to our
# codebase.
#
# It will:
# 1. determine which new keys have been added locally
# 2. pull translations from PhraseApp
# 3. generate new .strings files:
#    - any strings not included the default strings.xml are deleted
#    - any existing translations that have changed on PhraseApp will be updated
#    - any new local keys that have translations on PhraseApp will be inserted

def remove_translation_and_comment(node)
  if node.previous.previous.comment?
    if node.previous.previous.previous.text?
      node.previous.previous.previous.unlink
    end
    node.previous.previous.unlink
  end
  node.previous.unlink if node.previous.text?
  node.unlink
end

def remove_excess_strings(filename, included_strings, show_info)
  puts '▸ Removing unused translated strings'
  strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
  translation_nodes = strings_doc.xpath('//string').to_a
  all_strings = strings_doc.xpath('//string/@name').to_a.to_set(&:text)
  excess_strings = all_strings - included_strings
  translation_nodes.each do |translation|
    if excess_strings.include? translation['name']
      remove_translation_and_comment(translation)
      puts "Removed translation #{translation}" if show_info
    end
  end
  File.write(filename, strings_doc.to_xml)
end

Struct.new(
  'Translation',
  :translated_string,
  :description_comment,
  :string_node
)

def make_translations_hash(filename, included_strings)
  strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
  translation_nodes = strings_doc.xpath('//string').to_a
  translations_hash = {}
  translation_nodes.each do |translation|
    translation_comment = nil
    if translation.previous.previous.comment?
      translation_comment = translation.previous.previous
    end
    if included_strings.include? translation['name']
      translations_hash[translation['name']] = Struct::Translation.new(
        translation.content, translation_comment, translation
      )
    end
  end
  translations_hash
end

def replace_updated_translations(filename, translations_hash, show_info)
  puts '▸ Updating strings that have changed'
  strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
  translation_nodes = strings_doc.xpath('//string').to_a
  translation_nodes.each do |translation|
    next unless translations_hash.key?(translation['name'])
    key = translation['name']
    next unless !translation.previous.previous.nil? &&
                translation.previous.previous.comment?
    translation_comment = translation.previous.previous
    unless translation_comment.text.eql? translations_hash[key].description_comment.text
      puts "Updating translation comment for #{translation_comment}" if show_info
      translation_comment.replace(translations_hash[key].description_comment)
    end
    unless translation.content.eql? translations_hash[key].translated_string
      puts "Updating translation to #{translation}" if show_info
      translation.content = translations_hash[key].translated_string
    end
  end
  File.write(filename, strings_doc.to_xml)
end

def add_new_translations(filename, translations_hash, show_info)
  puts '▸ Adding new translations'
  strings_doc = File.open(filename) { |f| Nokogiri::XML(f) }
  translation_nodes = strings_doc.xpath('//string')
  existing_strings = strings_doc.xpath('//string/@name').to_a.to_set(&:text)
  new_strings = translations_hash.keys.to_set - existing_strings
  # Grab a new line node to use for formatting
  new_line_node = translation_nodes.first.next.dup
  # Move the last new line to the end
  last_newline_node = translation_nodes.last.next.unlink
  new_strings.each do |string|
    translation = translations_hash[string]
    unless translation.description_comment.nil?
      strings_doc.root.add_child(new_line_node.dup)
      strings_doc.root.add_child(translation.description_comment)
    end
    strings_doc.root.add_child(new_line_node.dup)
    strings_doc.root.add_child(translation.string_node)
    puts "Added translation for #{translation.string_node}" if show_info
  end
  strings_doc.root.add_child(last_newline_node)
  File.write(filename, strings_doc.to_xml)
end

def update_translations(default_strings_file, supported_languages, show_info=true)
  puts '▸ Building set of locally added strings'
  default_strings_doc = File.open(default_strings_file) { |f| Nokogiri::XML(f) }
  included_strings = default_strings_doc.xpath('//string/@name')
                                        .to_a.to_set(&:text)
  puts '▸ Downloading all translations from PhraseApp'
  `phraseapp pull -t #{ARGV[0]}`
  puts '▸ Removing translations for languages we do not yet support'
  language_dirs = Dir.glob 'stripe/res/values-*'
  language_dirs.each do |dir|
    unless supported_languages.include? dir.split('-')[1]
      puts 'Ignoring ' + dir + ' because it is not a supported language' if show_info
      FileUtils.rm_rf(dir) 
    end
  end

  supported_languages.each do |lang|
    puts '==================='
    puts 'Working on: ' + lang
    puts '==================='
    downloaded_filename = "stripe/res/values-#{lang}/strings-temp.xml" 
    translations_hash = make_translations_hash(
      downloaded_filename,
      included_strings
    )
    current_translations_filename = 'stripe/res/values-' + lang + '/strings.xml'
    remove_excess_strings(
      current_translations_filename,
      included_strings,
      show_info
    )
    replace_updated_translations(
      current_translations_filename, 
      translations_hash,
      show_info
    )
    add_new_translations(
      current_translations_filename,
      translations_hash,
      show_info
    )
    File.delete(downloaded_filename)
  end
end

DEFAULT_STRINGS_FILE = 'stripe/res/values/strings.xml'
langs = %w(de es fr it ja nl zh)

update_translations(DEFAULT_STRINGS_FILE, langs)
