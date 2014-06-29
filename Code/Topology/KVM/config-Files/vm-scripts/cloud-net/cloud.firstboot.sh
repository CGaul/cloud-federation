# This script will run the first time the virtual machine boots
# It will be run as root.


#Install mininet with -nfv dependencies, namely:
#OpenFlow reference switch & Open vSwitch
#In /opt/mininet

git clone git://github.com/mininet/mininet
mininet/util/install.sh -s /opt/mininet -nfv

#(from http://mininet.org/download/)
