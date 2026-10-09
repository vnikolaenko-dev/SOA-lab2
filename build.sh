#!/usr/bin/env bash

set -eu

rm -rf dist
mkdir -p dist

mvn -B -f collection-service/pom.xml -DskipTests package
mvn -B -f starship-service/pom.xml -DskipTests package

cp collection-service/target/*.war dist/
cp starship-service/target/*.war dist/