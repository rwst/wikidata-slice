import os, json, argparse, sys, datetime, re
from pyshexc.parser_impl.generate_shexj import parse
from antlr4 import InputStream
from jsonasobj import as_json
from rdflib import Graph, RDF, RDFS
from rdflib.term import URIRef, Literal
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
WIKIBASE = 'http://www.wikidata.org/wiki/EntitySchema:'

# read shexset file in dir and fill Edict
Edict = dict()
with open(dir + 'shexset', 'r') as setfile:
    Eset = set()
    lines = setfile.readlines()
    for l in lines:
        m = re.fullmatch(Ereg, l.rstrip())
        if m:
            E = m.group(0)
            with open(dir + E, 'r') as f:
                Edict[E] = f.read()
        else:
            print("{} does not match: {}".format(l, m))

# make sure BASE is set to http://www.wikidata.org/wiki/EntitySchema:Exyz in each Exyz file
for E,schema in Edict.items():
    newschema = ''
    base_is_set = False
    for line in schema.split('\n'):
        if line.startswith('BASE '):
            line = 'BASE <' + WIKIBASE + E + '>'
            base_is_set = True
        newschema = newschema + line + '\n'
    Edict[E] = newschema if base_is_set else 'BASE <' + WIKIBASE + E + '>\n' + newschema

# convert
for E,schema in Edict.items():
    print('-----processing {}:{}'.format(E, schema), file=sys.stderr)
    shexj = parse(InputStream(schema))
    shexj['@context'] = "http://www.w3.org/ns/shex.jsonld"
    shexj['@id'] = WIKIBASE + E
    g = Graph().parse(data=as_json(shexj, indent=None), format="json-ld")

    # put original ShExC code as rdfs:comment into schema
    g.add( (URIRef(WIKIBASE+E), RDFS.comment, Literal(schema)) )
    # write to N3
    g.serialize(open(dir + E + '.n3', "wb"), format='n3')



