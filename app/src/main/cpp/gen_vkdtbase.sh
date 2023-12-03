#!/bin/bash
shopt -s globstar

ZIPFILE=$1
ROOT=$2
MODS=src/pipe/modules/*

cd $ROOT
rm $ZIPFILE
zip -9r $ZIPFILE \
  bin/data \
  bin/default* \
  $MODS/params \
  $MODS/connectors \
  $MODS/*.ui \
  $MODS/ptooltips \
  $MODS/ctooltips \
  $MODS/readme.md \
  $MODS/*.so \
  src/pipe/modules/**.spv
