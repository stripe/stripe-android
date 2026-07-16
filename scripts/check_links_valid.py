#!/bin/python
import re
import glob
import os
import sys
import urllib.request
from urllib.error import HTTPError
from urllib.error import URLError
from concurrent.futures import ThreadPoolExecutor, as_completed

root_dir="docs"
readme="README.md"

# Compile regexes once instead of on every file read (was happening per-call before)
_regexStripe = re.compile(r'(https://stripe.com[^\\)|"|\\ |<]*)')
_regexGithub = re.compile(r'(https://github.com[^\\)|"|\\ |<]*)')

def request(url):
    try:
        print("requesting... ---" + url + "--- ")
        response = urllib.request.urlopen(url, timeout=10)
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
    regexStripe = _regexStripe
    regexGithub = _regexGithub
    allUrls = []
    isFile = os.path.isfile(filename)
    if(isFile):
        # Read whole file at once and run regex over full text instead of
        # line-by-line (fewer regex invocations, faster I/O)
        with open(filename) as file:
            content = file.read()
            allUrls.extend(regexStripe.findall(content))
            allUrls.extend(regexGithub.findall(content))
    return allUrls

def findStripeLinks(root_dir):
    urlSet=set()
    # Use a thread pool to read/scan files concurrently (I/O bound glob walk)
    filenames = list(glob.iglob(root_dir + '**/**', recursive=True))
    with ThreadPoolExecutor(max_workers=32) as executor:
        futures = [executor.submit(findStripeLinksInFile, filename) for filename in filenames]
        for future in as_completed(futures):
            urls = future.result()
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
# This is the real bottleneck: each request() call blocks on network I/O.
# Run them concurrently instead of sequentially.
urlDNE = []
with ThreadPoolExecutor(max_workers=8) as executor:
    future_to_url = {executor.submit(request, url): url for url in urlSet}
    for future in as_completed(future_to_url):
        url = future_to_url[future]
        if(not future.result()):
            urlDNE.append(url)

print(urlDNE)
sys.exit(1)
