#!/bin/bash

echo "Deleting target-dirs..."
rm -r target
rm -r */target

echo "Deleting .idea dir..."
rm -r .idea

echo "Deleting project/project dir..."
rm -r project/project
