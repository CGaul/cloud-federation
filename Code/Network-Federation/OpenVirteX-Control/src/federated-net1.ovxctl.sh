#!/bin/bash

# Create Network with OpenFlowPort 10.000 (Floodlight-Controller listens on that port)
#with own virtual address 10.0.0.0
python ovxctl.py -n createNetwork tcp:192.168.150.11:10000 10.0.5.0 16

echo "Establishing GW-1 Switch..."
python ovxctl.py -n createSwitch 1 00:00:00:00:00:01:10:00 #:01

echo "Establishing GW-2 Switch..."
python ovxctl.py -n createSwitch 1 00:00:00:00:00:02:10:00 #:02

echo "Establishing internal Switches 1.1 & 2.1 ..."
python ovxctl.py -n createSwitch 1 00:00:00:00:00:01:11:00 #:03
python ovxctl.py -n createSwitch 1 00:00:00:00:00:02:11:00 #:04


# Create ports for each Switch. 
# Physical and virtual Port mapping is the same here, too - except the Gateway, 
# where Port 1 is the Port that will be connected to the foreign GW:

echo "Establishing Ports (1,2) for GW-1 Switch..."
python ovxctl.py -n createPort 1 00:00:00:00:00:01:10:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:01:10:00 2

echo "Establishing Ports (1,2) for GW-2 Switch..."
python ovxctl.py -n createPort 1 00:00:00:00:00:02:10:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:02:10:00 2

echo "Creating Ports (1,2,3) for SWITCH-1.1..."
python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 2
python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 3
#python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 4

echo "Creating Ports (1,2,3) for SWITCH-2.1..."
python ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 2
python ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 3
#python ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 4


# Connect Switches with each other:
# GW 1 <-> GW2:
echo "Connecting Link: (GW-1, Port 1) <-> (GW-2, Port 1)..."
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 1 00:a4:23:05:00:00:00:02 1 spf 1

# GW 1 <-> Switch 1.1:
echo "Connecting Link: (GW-1, Port 2) <-> (SWITCH-1.1, Port 3)..."
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 2 00:a4:23:05:00:00:00:03 3 spf 1

# GW 2 <-> Switch 2.1:
echo "Connecting Link: (GW-2, Port 2) <-> (SWITCH-2.1, Port 3)..."
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:02 2 00:a4:23:05:00:00:00:04 3 spf 1



# Connect Hosts with Switches:
# GW 1 ([Port 1: Host DHCP])
#echo "Connecting Host: hdhcp <-> GW..."
#python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:01:01

# Switch 1.1 ([Port 1: Host 1], [Port 2: Host 2]):
echo "Connecting Host: h1_1_1 <-> SWITCH-1.1..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 1 00:00:00:00:01:11
echo "Connecting Host: h1_1_2 <-> SWITCH-1.1..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 2 00:00:00:00:01:12

# Switch 2.1 ([Port 1: Host 1], [Port 2: Host 2]):
echo "Connecting Host: h2_1_1 <-> SWITCH-2.1..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:04 1 00:00:00:00:02:11
echo "Connecting Host: h2_1_2 <-> SWITCH-2.1..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:04 2 00:00:00:00:02:12