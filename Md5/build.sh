#!/bin/bash

# Compile

javac -d build/classes -cp lib/* src/main/*.java

# Build jar

jar --create \
    --file build/ifxhash.jar \
    --manifest MANIFEST.MF \
    -C build/classes .

