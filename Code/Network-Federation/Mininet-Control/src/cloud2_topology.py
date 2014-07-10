__author__ = 'Constantin'

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.node import RemoteController
from mininet.log import setLogLevel


NODES = {
    'GW':       {'dpid': '000000000000210%s'},
    'SWITCH1':  {'dpid': '000000000000220%s'},
    'SWITCH2':  {'dpid': '000000000000230%s'},
    'SWITCH3':  {'dpid': '000000000000240%s'},
    }

HOSTS = {
    #To each Switch, a dict of host-ip tuples is mapped:
    'SWITCH1': {'h2.1.1': '10.0.2.1', 'h2.1.2': '10.0.2.2'},
    'SWITCH2': {'h2.2.1': '10.0.2.3'},
    'SWITCH3': {'h2.3.1': '10.0.2.4'},
}


class Cloud1Topo(Topo):
    def __init__(self):

        # Add default members to class.
        super(Cloud1Topo, self).__init__()

        # Add core switches
        self.cores = {}
        for switch in NODES:
            self.cores[switch] = self.addSwitch(switch, dpid=(NODES[switch]['dpid'] % '0'))

            # Add hosts and respective link, if switch has hosts:
            if switch in HOSTS:
                assert(isinstance(HOSTS[switch], dict))
                for host, ip in HOSTS[switch].items(): #Iterate over all host-ip tuples per Switch:
                    self.addHost(host, ip=ip)
                    self.addLink(host, self.cores[switch])

        # Connect core switches
        self.addLink(self.cores['GW'], self.cores['SWITCH1'])
        self.addLink(self.cores['SWITCH1'], self.cores['SWITCH2'])
        self.addLink(self.cores['SWITCH2'], self.cores['SWITCH3'])



if __name__ == '__main__':
    topo = Cloud1Topo()
    #net = Mininet(topo, autoSetMacs=True, xterms=False, controller=RemoteController)
    #net.addController('c', ip='127.0.0.1')
    net = Mininet(topo, autoSetMacs=True, xterms=False)
    #print "\nHosts configured with IPs, switches pointing to OpenVirteX at 127.0.0.1 port 6633\n"
    setLogLevel('info')
    net.start()
    net.pingAll()
    CLI(net)
    net.stop()
