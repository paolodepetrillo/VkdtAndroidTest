#!/bin/bash
shopt -s globstar

ZIPFILE=$1
ROOT=$2

cd $ROOT
rm $ZIPFILE
zip -9r $ZIPFILE \
  bin/data \
  bin/default*
cd src/pipe
zip -9r $ZIPFILE \
  modules/*/params \
  modules/*/connectors \
  modules/*/*.ui \
  modules/*/ptooltips \
  modules/*/ctooltips \
  modules/*/readme.md \
  modules/*/*.so \
  modules/**.spv
