#!/bin/bash
rm -Rf build/*
rm -Rf target/*
# yarn install && yarn lint && yarn test && yarn build
yarn install && yarn build
