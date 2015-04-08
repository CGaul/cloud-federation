## Code Repository
This is the code repository for the master thesis on the "Design and Implementation of a Cloud-Federation Agent for Software Defined Networking" from the Berlin Institute of Technology, by Constantin Gaul as part of the M.Sc. in Computer Engineering.

### Repository Contents:
* Cloud-Federation directory
    * The main codeline as a Scala / Akka implementation of an agent-based Cloud-Stack and a Federation-Broker 
    (read more about deployment in the directory's README.md)
* Monitoring directory
    * Monitoring scripts for the Testbed VMs: 
    If tmuxinator is installed, the citmaster-cmd.yml may be used to set up the whole testbed in one command.
    More on this can be found in the testbed's README.md
* Network-Federation directory
    * Contains all scripts, settings and python code for Floodlight, Mininet and OpenVirteX deployment that is used
    in the virtual machines of the testbed.
* Testbed-Installer directory
    * Contains all shell scripts that were used to set up the testbed. 
    The installation script could be used for the KVM/libvirt and OpenVSwitch environment under ubuntu, 
    however no guarantees are made about changes on the host system that could influence the working state or
    cause other serious networking problems. 
    THIS INSTALLER SHOULD NOT BE RUN ON A UBUNTU THAT IS REQUIRED FOR OTHER TASKS!