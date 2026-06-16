#!/bin/bash

# Compile

javac --release 11 -d build/classes src/main/*.java

# Build jar

echo "Building jar build/ifxhash.jar"

jar --create \
    --file build/UUIDGenerator.jar \
    --manifest MANIFEST.MF \
    judr.properties \
    -C build/classes .

# Install

echo "Installing library to $INFORMIXDIR/extend/krakatoa/UUIDGenerator.jar"

cp build/UUIDGenerator.jar $INFORMIXDIR/extend/krakatoa/
