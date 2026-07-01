#!/bin/zsh

# fabric source is added as a submodule using command
# git submodule add --depth 1 https://github.com/FabricMC/fabric-api.git fabric_decompiled/src

# reloading
git submodule update --init --recursive --depth 1

cd src

# check out the matching fabric-api tag
git fetch origin tag "0.153.0+26.2" --no-tags
git checkout "0.153.0+26.2"

cd ..