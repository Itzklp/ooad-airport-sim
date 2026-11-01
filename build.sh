#!/bin/sh
# Compiles the program, then the JUnit tests, then runs every test.
# Needs JDK 17 or later on the PATH. Run from this folder: sh build.sh
set -e
JUNIT=lib/junit-platform-console-standalone-1.10.5.jar
SEP=:
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac   # Java on Windows separates paths with ;
rm -rf out
javac --release 17 -d out src/sim/*.java src/airport/*.java src/results/*.java src/app/*.java
javac --release 17 -d out -cp "out$SEP$JUNIT" test/sim/*.java test/airport/*.java test/results/*.java test/app/*.java
java -jar "$JUNIT" execute -cp out --scan-classpath --disable-banner
