#!/bin/bash

# Create Network with OpenFlowPort 10.000 (Floodlight-Controller listens on that port)
#with own virtual address 10.0.0.0
python ovxctl.py -n createNetwork tcp:localhost:20000 10.0.3.0 16

# For each physical Switch create a virtual switch:
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:21:00
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:22:00
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:23:00
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:24:00


# Create ports for each Switch. Physical and virtual Port mapping is the same here, too:
# Switch 1 (:21:00):
python ovxctl.py -n createPort 2 00:00:00:00:00:00:21:00 1

# Switch 2 (:22:00):
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 1
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 2
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 3
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 4

# Switch 3 (:23:00):
python ovxctl.py -n createPort 2 00:00:00:00:00:00:23:00 1
python ovxctl.py -n createPort 2 00:00:00:00:00:00:23:00 2
python ovxctl.py -n createPort 2 00:00:00:00:00:00:23:00 3

# Switch 4 (:24:00):
python ovxctl.py -n createPort 2 00:00:00:00:00:00:24:00 1
python ovxctl.py -n createPort 2 00:00:00:00:00:00:24:00 2

echo "Please type the first 4 blocks of the virtual switch DPID (i.e. 00:a4:23:05):"
read

# Connect Switches with each other:
# Switch 1 <-> Switch 2:
python ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:01 1 00:a4:23:05:00:00:00:02 3 spf 1

# Switch 2 <-> Switch 3:
python ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:02 4 00:a4:23:05:00:00:00:03 2 spf 1

# Switch 3 <-> Switch 4:
python ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:03 3 00:a4:23:05:00:00:00:04 2 spf 1


# Connect Hosts with Switches:
# Switch 2 ([Port 1: Host 1], [Port 2: Host 2]):
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 1 00:00:00:00:00:21
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:22

# Switch 3 ([Port 1: Host 3]):
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 1 00:00:00:00:00:23

# Switch 4 ([Port 1: Host 4]):
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 1 00:00:00:00:00:24
