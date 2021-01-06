import os, json, argparse, sys, datetime, re
from pyshexc.parser_impl.generate_shexj import generate
"""
Find 'shexset' in the given directory aliases on convert the listed files to RDF.
Complain about missing files.
"""

# Initiate the parser
parser = argparse.ArgumentParser()
parser.add_argument("dir", help="directory to work on")

# Read arguments from the command line
args = parser.parse_args()
dir = os.path.dirname(args.dir) + '/'
Ereg = re.compile(r"E\d+$")

with open(dir + 'shexset', 'r') as setfile:
    Eset = set()
    lines = setfile.readlines()
    for l in lines:
        m = re.fullmatch(Ereg, l.rstrip())
        if m:
            Eset.add(m.group(0))
        else:
            print("{} does not match: {}".format(l, m))

for E in Eset:
    print('-----processing {}'.format(E))
    generate(['-nj', '-f', 'ttl', dir + E])

