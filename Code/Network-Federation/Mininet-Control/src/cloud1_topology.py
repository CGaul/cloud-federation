__author__ = 'Constantin'

#Mininet API imports:
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.link import Intf
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.log import setLogLevel

#Python system imports:
import re
import logging


OFC_IP = '192.168.150.10'   #The IP-Addr of the OpenFlow Controller, used for this mininet (OpenVirteX here).
OFC_PORT = 6633             #The Port of the OpenFlow Controller, used for this mininet (OpenVirteX here)
NET_IP = '10.0.1.0'         #The base IP-Network range, used for this mininet

#Define all hosts inside this mininet here via ip (as an offset of NET_IP) and MAC-Addr here:
HOSTS = {
    'h1_1_1': {'ip': '+1', 'mac': '00:00:00:00:00:11'},
    'h1_1_2': {'ip': '+2', 'mac': '00:00:00:00:00:12'},
    'h1_2_1': {'ip': '+3', 'mac': '00:00:00:00:00:13'},
    'h1_3_1': {'ip': '+4', 'mac': '00:00:00:00:00:14'},
    }

#Define all Switches inside this mininet via DPID, links between Switches and links to Hosts here:
SWITCHES = {
    'GW':       {'dpid': '00:00:00:00:00:00:11:00',
                 'links': ['SWITCH1'], 'hosts': []},
    'SWITCH1':  {'dpid': '00:00:00:00:00:00:12:00',
                 'links': ['SWITCH2'], 'hosts': ['h1_1_1', 'h1_1_2']},
    'SWITCH2':  {'dpid': '00:00:00:00:00:00:13:00',
                 'links': ['SWITCH3'], 'hosts': ['h1_2_1']},
    'SWITCH3':  {'dpid': '00:00:00:00:00:00:14:00',
                 'links': [], 'hosts': ['h1_3_1']},
    }


class Cloud1Topo(Topo):
    def __init__(self):

        # Add default members to class.
        super(Cloud1Topo, self).__init__()

        # Add core switches
        self.cores = {}
        for switch in SWITCHES:
            switch_dpid = SWITCHES[switch]['dpid']
            clear_dpid = translate_dpid(SWITCHES[switch]['dpid'])
            switch_hosts = SWITCHES[switch]['hosts']

            print("Adding Switch to network: "+ switch +" (dpid: "+ switch_dpid +")...")
            self.cores[switch] = self.addSwitch(switch, dpid=clear_dpid)

            # Add hosts and respective link, if switch has hosts:
            if len(switch_hosts) >= 1:
                assert(isinstance(switch_hosts, list))
                for host in switch_hosts: #Iterate over all hosts per Switch:
                    ip = translate_hostip(HOSTS[host]['ip'])
                    mac = HOSTS[host]['mac']
                    clear_mac = translate_dpid(mac)
                    print("Adding Host to Switch "+ switch +": "+ host +" (ip: "+ ip +", mac: "+ mac +")...")
                    self.addHost(host, ip=ip, mac=clear_mac)
                    self.addLink(host, self.cores[switch])

        # Connect core switches
        print("Adding Bi-directional Links to Switches...")
        self.addLink(self.cores['GW'], self.cores['SWITCH1'])
        self.addLink(self.cores['SWITCH1'], self.cores['SWITCH2'])
        self.addLink(self.cores['SWITCH2'], self.cores['SWITCH3'])

# TODO: fix this link automatization:
#        for switch in SWITCHES:
#            switch_links = SWITCHES[switch]['links']
#            assert(isinstance(switch_hosts, list))
#            for linkedSwitch in switch_links: #Iterate over all Switch Links per Switch:
#                print ("Connecting Switch "+ switch +" with Switch "+ linkedSwitch +"...")
#                # Connect core switches
#                self.addLink(self.cores[switch], self.cores[linkedSwitch])


def check_configuration():
    #Check NET_IP:
    try:
        assert(isinstance(NET_IP, str))
        #Check if NET_IP is correct:
        correct_netip = re.findall( r'[0-9]+(?:\.[0-9]+){3}', NET_IP)
        if len(correct_netip) == 0:
            logging.error("NET_IP was not specified correctly. Current NET_IP is: "+ NET_IP)

    except AssertionError:
        logging.error("NET_IP should be a String!")


def translate_hostip(ip_input):
    assert(isinstance(ip_input, str))

    #Find out via regex, if ip_input is a valid ip or an ip offset:
    found_ip = re.findall( r'[0-9]+(?:\.[0-9]+){3}', ip_input)
    if len(found_ip) == 0:
        #Treat the IP-input as an ip_offset from the NET_IP base addr:
        clear_offset = ip_input.translate(None, "+")
        base_netip = NET_IP[0:7]
        host_ip = base_netip + clear_offset
    else: #If an ip could be extracted via .findall:
        host_ip = found_ip[0] #Only the first IP match is of importance:

    return host_ip

def translate_dpid(readable_dpid):
    assert(isinstance(readable_dpid, str))
    clear_dpid = readable_dpid.translate(None, ':')
    return clear_dpid


if __name__ == '__main__':
    check_configuration()
    topo = Cloud1Topo()

    #Create Mininet with manual Remote Controller (OpenVirteX):
    ovxController= RemoteController("ovxController", ip=OFC_IP)
    net = Mininet(topo, autoSetMacs=True, xterms=False, controller=None)
    net.addController('ovxController', controller=RemoteController, ip=OFC_IP, port=OFC_PORT)

    print("Adding Gateway-Node...")
    gwNode = net.getNodeByName('GW')
    print("Adding interface 'eth0' to "+ str(gwNode) +"...")
    Intf('eth0', node=gwNode)

    print("Adding dhcp-host...")
    h_dhcp = net.addHost('h_dhcp', ip='0.0.0.0', mac=translate_dpid('00:00:00:00:00:10'))
    print("Connecting dhcp-Host with Gateway Node...")
    net.addLink(h_dhcp, gwNode)
    #print("Preparing dhcp-Host as a DHCP-Server for the Network...")
    #h_dhcp.cmdPrint('dhclient '+h_dhcp.defaultIntf().name)

    print("\nHosts configured with IPs, switches pointing to OpenVirteX at: "+ OFC_IP +" port: "+ str(OFC_PORT) +"\n")

    setLogLevel('info')
    net.start()
    CLI(net)
    net.stop()
