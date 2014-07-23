#!/bin/bash

# Create Network with OpenFlowPort 10.000 (Floodlight-Controller listens on that port)
#with own virtual address 10.0.0.0
python ovxctl.py -n createNetwork tcp:192.168.150.11:10000 10.0.1.0 16

# For each physical Switch create a virtual switch:
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:11:00
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:12:00
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:13:00
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:14:00


# Create ports for each Switch. Physical and virtual Port mapping is the same here, too:
# Switch 1 (:11:00):
python ovxctl.py -n createPort 1 00:00:00:00:00:00:11:00 1

# Switch 2 (:12:00):
python ovxctl.py -n createPort 1 00:00:00:00:00:00:12:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:12:00 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:12:00 3
python ovxctl.py -n createPort 1 00:00:00:00:00:00:12:00 4

# Switch 3 (:13:00):
python ovxctl.py -n createPort 1 00:00:00:00:00:00:13:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:13:00 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:13:00 3

# Switch 4 (:14:00):
python ovxctl.py -n createPort 1 00:00:00:00:00:00:14:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:14:00 2


#echo "Please type the first 4 blocks of the virtual switch DPID (i.e. 00:af:23:05):"
#read

# Connect Switches with each other:
# Switch 1 <-> Switch 2:
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 1 00:a4:23:05:00:00:00:02 3 spf 1

# Switch 2 <-> Switch 3:
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:02 4 00:a4:23:05:00:00:00:03 2 spf 1

# Switch 3 <-> Switch 4:
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:03 3 00:a4:23:05:00:00:00:04 2 spf 1


# Connect Hosts with Switches:
# Switch 2 ([Port 1: Host 1], [Port 2: Host 2]):
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 1 00:00:00:00:00:11
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:12

# Switch 3 ([Port 1: Host 3]):
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 1 00:00:00:00:00:13

# Switch 4 ([Port 1: Host 4]):
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:04 1 00:00:00:00:00:14
