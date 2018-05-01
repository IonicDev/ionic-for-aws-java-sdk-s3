#!/bin/bash

SKIPTEST=true

mvn -Dmaven.test.skip=${SKIPTEST} package
mvn -Dmaven.test.skip=${SKIPTEST} install

pushd awss3examples && mvn package appassembler:assemble

