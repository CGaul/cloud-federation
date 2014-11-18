#!/bin/bash
LOGPATH=$1
LATEST_LOG=$(ls -t ${LOGPATH} | head -1)

tail -n 50 -f ${LATEST_LOG}
