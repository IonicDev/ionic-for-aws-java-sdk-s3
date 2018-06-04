#!/bin/bash

SKIPTEST=true

pushd ionics3
mvn -Dmaven.test.skip=${SKIPTEST} package install
popd

pushd awss3examples
mvn clean package appassembler:assemble
popd
