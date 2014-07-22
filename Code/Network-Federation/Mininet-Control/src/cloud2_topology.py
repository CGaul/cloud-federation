__author__ = 'Constantin'

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.node import RemoteController
from mininet.log import setLogLevel


HOSTS = {
    'h2_1_1': {'ip': '10.0.2.1', 'mac': '00:00:00:00:00:00:00:21'},
    'h2_1_2': {'ip': '10.0.2.2', 'mac': '00:00:00:00:00:00:00:22'},
    'h2_2_1': {'ip': '10.0.2.3', 'mac': '00:00:00:00:00:00:00:23'},
    'h2_3_1': {'ip': '10.0.2.4', 'mac': '00:00:00:00:00:00:00:24'},
    }

SWITCHES = {
    'GW':       {'dpid': '00:00:00:00:00:00:21:00', 'hosts': []},
    'SWITCH1':  {'dpid': '00:00:00:00:00:00:22:00', 'hosts': ['h2_1_1', 'h2_1_2']},
    'SWITCH2':  {'dpid': '00:00:00:00:00:00:23:00', 'hosts': ['h2_2_1']},
    'SWITCH3':  {'dpid': '00:00:00:00:00:00:24:00', 'hosts': ['h2_3_1']},
    }


class Cloud2Topo(Topo):
    def __init__(self):
        # Add default members to class.
        super(Cloud2Topo, self).__init__()

        # Add core switches
        self.cores = {}
        for switch in SWITCHES:
            switch_dpid = translate_dpid(SWITCHES[switch]['dpid'])
            switch_hosts = SWITCHES[switch]['hosts']
            print("Adding Switch to network: "+ switch +" (dpid: "+ switch_dpid +")...")
            self.cores[switch] = self.addSwitch(switch, dpid=switch_dpid)

            # Add hosts and respective link, if switch has hosts:
            if len(switch_hosts) >= 1:
                assert(isinstance(switch_hosts, list))
                for host in switch_hosts: #Iterate over all hosts per Switch:
                    ip = HOSTS[host]['ip']
                    mac = HOSTS[host]['mac']
                    mac = translate_dpid(mac)
                    print("Adding Host to Switch "+ switch +": "+ host +" (ip: "+ ip +", mac: "+ mac +")...")
                    self.addHost(host, ip=ip, mac=mac)
                    self.addLink(host, self.cores[switch])

        # Connect core switches
        self.addLink(self.cores['GW'], self.cores['SWITCH1'])
        self.addLink(self.cores['SWITCH1'], self.cores['SWITCH2'])
        self.addLink(self.cores['SWITCH2'], self.cores['SWITCH3'])


def translate_dpid(readable_dpid):
    assert(isinstance(readable_dpid, str))
    clear_dpid = readable_dpid.translate(None, ':')
    return clear_dpid


if __name__ == '__main__':
    topo = Cloud2Topo()

    #Create Mininet with manual Remote Controller (OpenVirteX):
    ovxController= RemoteController("ovxController", ip='192.168.150.10')
    net = Mininet(topo, autoSetMacs=True, xterms=False, controller=None)
    net.addController('ovxController', controller=RemoteController, ip='192.168.150.10', port=6633)

    print ("\nHosts configured with IPs, switches pointing to OpenVirteX at 192.168.150.10 port 6633\n")

    setLogLevel('info')
    net.start()
    #net.pingAll()
    CLI(net)
    net.stop()
