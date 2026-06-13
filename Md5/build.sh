#!/bin/bash

mkdir -p build/classes
javac -d build/classes src/IfxHash.java
jar cf build/ifxhash.jar -C build/classes .

