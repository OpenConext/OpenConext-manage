#!/bin/bash
rm -Rf build/*
rm -Rf target/*
# yarn install && yarn lint && yarn test && yarn build
yarn install && CI=true yarn test && yarn build
