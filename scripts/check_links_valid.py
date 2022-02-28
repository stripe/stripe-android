#!/bin/python
import re
import glob
import os
import sys
import urllib.request
from urllib.error import HTTPError
from urllib.error import URLError

root_dir="docs"
readme="README.md"

def request(url):
    try:
        print("requesting... ---" + url + "--- ")
        response = urllib.request.urlopen(url)
        if(response):
            return response.getcode() == 200
        else:
            print("No response\n")
            return False
    except HTTPError as e:
        print("HTTPError \n")
        return False
    except URLError as e:
        print("URLError\n")
        return False
    print("\n")
    return False

def findStripeLinksInFile(filename):
    regexStripe = r'(https://stripe.com[^\\)|"|\\ |<]*)'
    regexGithub = r'(https://github.com[^\\)|"|\\ |<]*)'
    allUrls = []
    isFile = os.path.isfile(filename)
    if(isFile):
        with open(filename) as file:
            for line in file:
                allUrls.extend(re.findall(regexStripe, line))
                allUrls.extend(re.findall(regexGithub, line))
    return allUrls


def findStripeLinks(root_dir):
    urlSet=set()
    for filename in glob.iglob(root_dir + '**/**', recursive=True):
        urls = findStripeLinksInFile(filename)
        if(urls):
            for url in urls:
                urlSet.add(url)

    return urlSet

##------
# Find the stripe links in the documentation directory
urlSet = findStripeLinks(root_dir)
# Find the stripe links in the Readme
for urlLinkFromReadme in findStripeLinksInFile("README.md"):
  urlSet.add(urlLinkFromReadme)

# For each link verify it does not 404 when requested.
urlDNE = []
for url in urlSet:
    if(not request(url)):
        urlDNE.append(url)
print(urlDNE)
sys.exit(1)
