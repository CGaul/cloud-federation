#!/bin/bash

echo "Killing currently running instances of floodlight-controllers..."
kill `ps ax|grep floodlight|grep -v grep|awk '{print $1}'`
