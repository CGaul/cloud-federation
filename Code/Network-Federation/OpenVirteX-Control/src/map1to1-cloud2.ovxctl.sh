#!/bin/bash

# Create Network with OpenFlowPort 10.000 (Floodlight-Controller listens on that port)
#with own virtual address 10.0.0.0
python ovxctl.py -n createNetwork tcp:192.168.150.12:10000 10.0.3.0 16

# For each physical Switch create a virtual switch:
# Home Gateway Switch:
echo "Creating Gateway-1 Switch..."
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:21:00

# Local (inner) Switches:
echo "Creating internal Switches 2-4..."
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:22:00
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:23:00
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:24:00

# Foreign Gateway Switch:
echo "Creating foreign Gateway-2 Switch..."
python ovxctl.py -n createSwitch 2 00:00:00:00:00:00:11:00



# Create ports for each Switch. 
# Physical and virtual Port mapping is the same here, too - except the Gateway, 
# where Port 1 is the Port that will be connected to the foreign GW:

# Home Gateway Switch (:11:00):
echo "Creating Ports (1,2) for GW-1 Switch..."
python ovxctl.py -n createPort 2 00:00:00:00:00:00:21:00 1
python ovxctl.py -n createPort 2 00:00:00:00:00:00:21:00 2

# Foreign Gateway Switch (:21:00):
echo "Creating Ports (1) for foreign GW-2 Switch..."
python ovxctl.py -n createPort 2 00:00:00:00:00:00:11:00 1

# Switch 2 (:22:00):
echo "Creating Ports (1,2,3,4) for SWITCH-2..."
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 1
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 2
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 3
python ovxctl.py -n createPort 2 00:00:00:00:00:00:22:00 4

# Switch 3 (:23:00):
echo "Creating Ports (1,2,3) for SWITCH-3..."
python ovxctl.py -n createPort 2 00:00:00:00:00:00:23:00 1
python ovxctl.py -n createPort 2 00:00:00:00:00:00:23:00 2
python ovxctl.py -n createPort 2 00:00:00:00:00:00:23:00 3

# Switch 4 (:24:00):
echo "Creating Ports (1,2) for SWITCH-4..."
python ovxctl.py -n createPort 2 00:00:00:00:00:00:24:00 1
python ovxctl.py -n createPort 2 00:00:00:00:00:00:24:00 2



# Connect Switches with each other:
# GW 1 <-> GW2:
echo "Connecting Link: (GW-1, Port 1) <-> (GW-2, Port 1)..."
python ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:01 1 00:a4:23:05:00:00:00:05 1 spf 1

# GW 1 <-> Switch 2:
echo "Connecting Link: (GW-1, Port 2) <-> (SWITCH-2, Port 3)..."
python ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:01 2 00:a4:23:05:00:00:00:02 3 spf 1

# Switch 2 <-> Switch 3:
echo "Connecting Link: (SWITCH-2, Port 4) <-> (SWITCH-3, Port 2)..."
python ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:02 4 00:a4:23:05:00:00:00:03 2 spf 1

# Switch 3 <-> Switch 4:
echo "Connecting Link: (SWITCH-3, Port 3) <-> (SWITCH-4, Port 2)..."
python ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:03 3 00:a4:23:05:00:00:00:04 2 spf 1


# Connect Hosts with Switches:
# Switch 2 ([Port 1: Host 1], [Port 2: Host 2]):
echo "Connecting Host: h2_1_1 <-> SWITCH-2..."
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 1 00:00:00:00:00:21
echo "Connecting Host: h2_1_2 <-> SWITCH-2..."
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:22

# Switch 3 ([Port 1: Host 3]):
echo "Connecting Host: h2_2_1 <-> SWITCH-3..."
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 1 00:00:00:00:00:23

# Switch 4 ([Port 1: Host 4]):
echo "Connecting Host: h2_3_1 <-> SWITCH-4..."
python ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 1 00:00:00:00:00:24
