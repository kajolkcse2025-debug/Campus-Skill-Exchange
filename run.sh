#!/bin/sh
# Compile and launch the Campus Skill Exchange desktop app.
set -e
mkdir -p out
javac -d out -nowarn $(find src -name "*.java")
java -cp out Main "$@"
