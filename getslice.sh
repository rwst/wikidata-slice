FILE=shexset/$1/shexset
if [ ! -f "$FILE" ]; then
    echo "$FILE does not exist."
else
    cd shexset/$1
    rm -f E*
    for i in `cat shexset`
    do
        wget https://www.wikidata.org/wiki/Special:EntitySchemaText/$i
        sleep 2s
    done
fi
