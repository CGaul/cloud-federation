#!/bin/bash

mkdir logs

echo "Starting floodlight-controller-1..."
java -jar floodlight.jar -cf openvirtex-defaultctl_1.floodlight > ./logs/ctrl1.log 2>&1 &

echo "Starting floodlight-controller-2..."
java -jar floodlight.jar -cf openvirtex-defaultctl_1.floodlight > ./logs/ctrl2.log 2>&1 &

exit 0

