__author__ = 'Constantin'

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.node import RemoteController


NODES = {
    'GW':       {'dpid': '000000000000010%s'},
    'SWITCH1':  {'dpid': '000000000000020%s'},
    'SWITCH2':  {'dpid': '000000000000030%s'},
    'SWITCH3':  {'dpid': '000000000000040%s'},
    }

HOSTS = {
    #To each Switch, a dict of host-ip tuples is mapped:
    'SWITCH1': {'h_S1_1': '10.0.0.1', 'h_S1_2': '10.0.0.2'},
    'SWITCH2': {'h_S1_3': '10.0.0.3'},
    'SWITCH3': {'h_S1_4': '10.0.0.4'},
}


class Cloud1Topo(Topo):
    def __init__(self):

        # Add default members to class.
        super(Cloud1Topo, self).__init__()

        # Add core switches
        self.cores = {}
        for switch in NODES:
            self.cores[switch] = self.addSwitch(switch, dpid=(NODES[switch]['dpid'] % '0'))

            # Add hosts and links:
            assert(isinstance(HOSTS[switch], list))
            for host, ip in HOSTS[switch].items(): #Iterate over all host-ip tuples per Switch:
                assert(isinstance(host, dict))
                self.addHost(host, ip=ip)
                self.addLink(host, self.cores[switch])
    
        # Connect core switches
        self.addLink(self.cores['GW'], self.cores['SWITCH1'])
        self.addLink(self.cores['SWITCH1'], self.cores['SWITCH2'])
        self.addLink(self.cores['SWITCH2'], self.cores['SWITCH3'])



if __name__ == '__main__':
    topo = Cloud1Topo()
    net = Mininet(topo, autoSetMacs=True, xterms=False, controller=RemoteController)
    net.addController('c', ip='127.0.0.1')
    print "\nHosts configured with IPs, switches pointing to OpenVirteX at 127.0.0.1 port 6633\n"
    net.start()
    CLI(net)
    net.stop()