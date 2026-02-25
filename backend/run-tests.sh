#!/bin/bash
# Script to run tests with Java 17

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn clean test
