#!/bin/bash

# Compile

javac -d build/classes -cp lib/* src/main/*.java

# Build jar

echo "Building jar build/ifxhash.jar"

jar --create \
    --file build/ifxhash.jar \
    --manifest MANIFEST.MF \
    judr.properties \
    -C build/classes .

# Install

echo "Installing library to $INFORMIXDIR/extend/krakatoa/ifxhash.jar"

cp build/ifxhash.jar $INFORMIXDIR/extend/krakatoa/
