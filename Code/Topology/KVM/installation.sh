#!/bin/bash

#########################################################################
## Automatic Installer for the Master-Thesis Cloud Federation Testbed. ##
## This installer will configure KVM and initialize the VMs,           ##
## assigned to the testbeds for the cooperation with Huawei.           ##
#########################################################################

#author:    Constantin Gaul
#version:   0.20
#date:      June, 28th. 2014

## Imports: ##
##############
. ./auxillaryFunctions.sh --source-only


######################################
##         Global Constants:        ##
######################################

DATE_NOW=$(date +%F_%I.%M.%S)
CURR_USER=$(id -un)
findCurrentDir CURR_DIR #Variable assignment per reference
findHomeDir HOME_DIR    #Variable assignment per reference

PROGRESS_FILE=${CURR_DIR}/progress.log
LOG_FILE=${CURR_DIR}/installation.log

CONFIGFILE_PATH=${CURR_DIR}/config-Files
RESOURCE_PATH=${CURR_DIR}/resources
VM_IMAGE_PATH=~/.vmbuilder/VMs
TEMPLATE_PATH=~/.vmbuilder/Cloud-Templates/


#Config- &Resource- File paths for each VM:
CLOUDNET_NAME="cloud-net"
CLOUDNET_CONF_PATH=${CONFIGFILE_PATH}/vm-scripts/${CLOUDNET_NAME}


#Whiptail Shell GUI:
GUI_MAXHWIDTH=80
GUI_MAXHEIGHT=20



######################################
##       Function Definitions:      ##
######################################

printHelp(){
	echo "Cloud Federation installation script v.0.2 (2014 Jun 28):"
	echo "This installation script will install all necessary tools for a KVM-hypervisor Server,"
	echo "including the initilialization of all VMs, needed for a Cloud-Federation Testbed of generic size."
	echo ""
	echo "Basic Usage:"
	echo "\"sudo ./installation [ARGS (optional)]\""
	echo ""
	echo "Arguments (optional):"
	echo -e "-e | --eth		    The ethernet interface, which will be used"
	echo -e 			"\t\t	to configure the virtual bridge on."
	echo -e 			"\t\t	IMPORTANT NOTE: It is not recommended to use"
	echo -e 			"\t\t	the same eth-interface as the ssh management interface!"
	echo -e 			"\t\t	If some bridge settings have to be manually adjusted,"
	echo -e 			"\t\t	this could cause an ongoing access loss via ssh."
	echo -e 			"\t\t	placeholders have to be used for the respective fields: "
	echo -e 			"\t\t	\"FIRSTBOOT\", \"FIRSTLOGIN\", \"IPADDR\", \"GATEWAY\". "
	echo -e "-i | --ipAddr		The IP-Address that will be assigned to the"
	echo -e 			"\t\t	virtual bridge (on the eth-interface, defined via \"-e\")."
	echo -e "-y | --yestoAll  	Assume Yes to all queries and do not prompt."
	echo
	echo -e "-h | --help		Prints this help."

	exit 0
}


######################################
##     Argument Variable setup:     ##
######################################

### Default Variables: ###
ethX=""
ipAddr=""
yestoAll=false

### Command argument evaluation: ###
while [ $# -gt 0 ]; do 	# Until you run out of parameters
	case "$1" in
	#Optional Parameters:
		-[e]|--eth)	        ethX="$2" ;;
		-[i]|--ipAddr)		ipAddr="$2" ;;
		-[y]|--yestoAll)	yestoAll=true ;;
	#Printing the Help:
		-[hH]|-help|--help)	printHelp ;;
	   #*)					configFile="$1"
	esac
	shift	# Check next set of parameters.
done

## Program call-evaluation: ##
##############################

### Only runnable as root: ###
#----------------------------#

if ! [ "${CURR_USER}" = "root" ]; then
	whiptail --title "Execute installer with sudo rights!" --msgbox "Application must be run as root! Current user is \"${CURR_USER}\". Aborting script..." 10 ${GUI_MAXHWIDTH}
	exit 1
fi

### Only runnable when relevant Parameters assigned: ###
#------------------------------------------------------#

#If the parameters ethX and ipAddr are not set as script params, ask user about those values here:
if [[ "${ethX}" == "" || "${ipAddr}" == "" ]]; then
	ethX=$(whiptail --title "Define bridge's eth-Interface" \
					--backtitle "Installer Parameter Setup" \
					--inputbox \
	"In the setup process, a br0 will be defined in /etc/network/interfaces.\
	This virtual bridge needs to mapped to a physical eth-Interface that is\
	correctly linked into a given ethernet. \n\n\
	Which interface do you want to choose? (F.e. \"eth0\")" \
					15 ${GUI_MAXHWIDTH} 3>&1 1>&2 2>&3)
	if [[ "${ethX}" == "" ]]; then
		whiptail --title "Wrong User input." --msgbox "ethernet-Interface still undefined. Aborting script..." 10 ${GUI_MAXHWIDTH}
		exit 2
	fi
	ipAddr=$(whiptail --title "Define bridge's IP-Address" \
					  --backtitle "Installer Parameter Setup" \
					  --inputbox \
	"In the setup process, a br0 will be defined in /etc/network/interfaces.\
	This virtual bridge will be configured with a static IP-Address.\n\n\
	Type the static IP-Addr for br0 (F.e. \"192.168.1.100\")" \
				    15 ${GUI_MAXHWIDTH} 3>&1 1>&2 2>&3)
	if [[ "${ipAddr}" == "" ]]; then
		whiptail --title "Wrong User input." --msgbox "IP-Addr for virtual bridge still undefined. Aborting script..." 10 ${GUI_MAXHWIDTH}
		exit 2
	fi
fi


######################################
##      The Program Execution:      ##
######################################

echo
echo "Cloud Federation Testbed Installer"
echo "=================================="

## Preservation of State: ##
############################

echo "Generating progress-file, if non-existent: " ${PROGRESS_FILE}
su -c "touch ${PROGRESS_FILE}" ${SUDO_USER}
echo "Generating log-file, if non-existent: " ${PROGRESS_FILE}
su -c "touch ${LOG_FILE}" ${SUDO_USER}
sudo chmod ug+rwx ${PROGRESS_FILE}
sudo chmod ug+rwx ${LOG_FILE}

hasProgress ${PROGRESS_FILE} PROGRESS
if [ ${PROGRESS} -ne 0 ]; then
    loadProgress ${PROGRESS_FILE} PROGRESS_LIST
    echo -e "Installation Progress found. Previous install-points, already done: \n${PROGRESS_LIST}. Continue? [Y/n]"
    queryUserCont_Yn "User aborts, continuing with installation after points: \n${PROGRESS_LIST}."
fi


## SECTION 1 ##
###############

#Only show summary, if the whole section is not installed completely (partially aborted):
#1.4 is the last subsection in this first section, so query this:
containsProgress ${PROGRESS_FILE} "1.4" containsSection
if [ ${containsSection} -eq 0 ] && [ ${yestoAll} == false ]; then
	whiptail --title "Installation Procedure" --backtitle "First Section" \
			 --yesno \
	"Summary: \n\
	1.1) Updating and Upgrading Ubuntu Installation. \n\
	1.2) Installing administrative tools. \n\
	1.3) Installing kvm and virtualization tools. \n\
	1.4) User group management.\n\n\
	The whole process list will be executed on the next step. Continue?"\
	${GUI_MAXHEIGHT} ${GUI_MAXHWIDTH}
	choice=$?
	if [[ $choice != 0 ]]; then 
		echoAndLog "Aborted script, before user started Section 1." ${LOG_FILE}
		exit 5
	fi
fi


## SUBSECTION 1.1 ##
#Only run the section 1.1, if the progress file does not contain it already:
containsProgress ${PROGRESS_FILE} "1.1" containsSection
if [ ${containsSection} -eq 0 ]; then
	echoAndLog "1.1) Updating and Upgrading Ubuntu installation..." ${LOG_FILE}
	sudo apt-get update -qqy
	sudo apt-get upgrade -qy >> ${LOG_FILE}
	saveProgress ${PROGRESS_FILE} "1.1" "Updating & Upgrading Ubuntu. DONE"
fi


## SUBSECTION 1.2 ##
containsProgress ${PROGRESS_FILE} "1.2" containsSection
if [ ${containsSection} -eq 0 ]; then
	echoAndLog "1.2.1) Installing important administrative tools, if not already installed: git, vim, ranger, htop, lshw, iptraf, ethtool and traceroute..." ${LOG_FILE}
	sudo apt-get install -qy --force-yes git vim htop lshw iptraf ethtool traceroute >> ${LOG_FILE}

	echoAndLog "1.2.2) Injecting bash aliases to bash console..."
#	TODO: ask for injection here.
	su -c "cp ${CONFIGFILE_PATH}/bash.global_aliases ~/.bash_aliases" ${SUDO_USER}
	su -c "source ~/.bashrc" ${SUDO_USER}
	saveProgress ${PROGRESS_FILE} "1.2" "Installing Administrative Tools. DONE"
fi


## SUBSECTION 1.3 ##
containsProgress ${PROGRESS_FILE} "1.3" containsSection
if [ ${containsSection} -eq 0 ]; then
	echoAndLog "1.3) Installing ubuntu-virt-server, python-vm-builder, kvm-ipxe, libguestfs-tools and bridge-utils..." ${LOG_FILE}
	echo "This really may take a while."
	sudo apt-get install -qy --force-yes ubuntu-virt-server python-vm-builder kvm-ipxe libguestfs-tools bridge-utils >> ${LOG_FILE}
	saveProgress ${PROGRESS_FILE} "1.3" "Installing KVM Tools. DONE"
fi


## SUBSECTION 1.4 ##
containsProgress ${PROGRESS_FILE} "1.4" containsSection
if [ ${containsSection} -eq 0 ]; then
	echoAndLog "1.4) Adding root and maintenance user to \"libvirtd\" and \"kvm\" group..." ${LOG_FILE}

	#Adding root user to kvm groups (root is currently active, as script needs sudo rights):
	echo "Adding KVM Groups for \"${CURR_USER}\""
	adduser `id -un` libvirtd >> ${LOG_FILE}
	adduser `id -un` kvm >> ${LOG_FILE}

	#Adding default management user to kvm groups (assigned as a script argument in $user):
	echo "Adding KVM Groups for \"${SUDO_USER}\""
	sudo usermod -aG libvirtd ${SUDO_USER} >> ${LOG_FILE}
	sudo usermod -aG kvm ${SUDO_USER} >> ${LOG_FILE}
	saveProgress ${PROGRESS_FILE} "1.4" "Adding User to libvirtd & kvm group. DONE"

	echoAndLog "Finished Installation procedure successfully!" ${LOG_FILE}
fi


## SECTION 2 ##
###############

containsProgress ${PROGRESS_FILE} "2.3" containsSection
if [ ${containsSection} -eq 0 ] && [ ${yestoAll} == false ]; then
	whiptail --title "Network Configuration" --backtitle "Second Section" \
			 --yesno \
	"Summary: \n\
	2.1) Changing /etc/network/interfaces config, using a virtual bridge on the assigned eth-interface. \n\
	2.2) Restarting the Network \n\
	2.3) Rebooting. \n\
	The whole process list will be executed on the next step. Continue?"\
	${GUI_MAXHEIGHT} ${GUI_MAXHWIDTH}
	choice=$?
	if [[ $choice != 0 ]]; then 
		echoAndLog "Aborted script, before user started Section 2." ${LOG_FILE}
		exit 5
	fi
fi


## SUBSECTION 2.1 ##
containsProgress ${PROGRESS_FILE} "2.1" containsSection
if [ ${containsSection} -eq 0 ]; then
	echoAndLog "2.1) Changing /etc/network/interfaces so that the assigned ${ethX} and IP-Address ${ipAddr} are used in the virtual bridge config" ${LOG_FILE}
	cp ${CONFIGFILE_PATH}/network.interfaces ${CONFIGFILE_PATH}/interfaces

	echoAndLog "Preparing the new interface-configuration file..." ${LOG_FILE}
	#Replace the pre-defined symbols inside the network.interfaces copy by the assigned ones:
	sed -i "s/ETH_X/${ethX}/g" ${CONFIGFILE_PATH}/interfaces
	sed -i "s/IPADDR/${ipAddr}/g" ${CONFIGFILE_PATH}/interfaces

	#Define the IP-Subnet by cutting the last entry from the IP Address:
	ipSubnet=`echo ${ipAddr} | awk 'BEGIN {FS = "[.]+" } {print $1"."$2"."$3}'`
	sed -i "s/IPSUBNET/${ipSubnet}/g" ${CONFIGFILE_PATH}/interfaces

	#At last, give the network-admin who queried this tools (basically us ;) an overview of the config, using vim:
	if [[ ${yestoAll} == false ]]; then
		vim ${CONFIGFILE_PATH}/interfaces /etc/network/interfaces
		echo "Was network/interfaces correctly edited? If yes, interface file would be replaced in the next step. Continue? [y/N]"
		queryUserCont_yN "User aborts after observing /etc/network/interaces."
	fi

	echoAndLog "Moving the generated interface-configuration file to /etc/network/interfaces, saving the current one there in interfaces.old..." ${LOG_FILE}
	#First move the current network/interfaces to store it for later and replace it afterwards
	mv /etc/network/interfaces /etc/network/interfaces.old
	mv ${CONFIGFILE_PATH}/interfaces /etc/network/interfaces
	saveProgress ${PROGRESS_FILE} "2.1" "Changed /etc/network/interfaces to have a vBridge installed. DONE"
fi


## SUBSECTION 2.2 ##
containsProgress ${PROGRESS_FILE} "2.2" containsSection
if [ ${containsSection} -eq 0 ]; then
	echo "2.2) Restarting the network. If only SSH access is available, really be sure that the network/interfaces config is correct!"
	
	if [[ ${yestoAll} == false ]]; then
		echo "Continue? [y/N]"
		queryUserCont_yN "User aborts before restarting network interfaces."
	fi

	/etc/init.d/networking restart
	saveProgress ${PROGRESS_FILE} "2.2" "Restarted Network. DONE"
fi


## SUBSECTION 2.3 ##
containsProgress ${PROGRESS_FILE} "2.3" containsSection
if [ ${containsSection} -eq 0 ]; then
	echo "2.3) Rebooting the device..."

	if [[ ${yestoAll} == false ]]; then
		echo "Continue? [Y/n]"
		queryUserCont_Yn "User aborts before rebooting the Server."
	fi

	echoAndLog "Rebooting Server..." ${LOG_FILE}
	saveProgress ${PROGRESS_FILE} "2.3" "Rebooting Server... "
	reboot
fi


## SECTION 3 ##
###############

containsProgress ${PROGRESS_FILE} "3.4" containsSection
if [ ${containsSection} -eq 0 ] && [ ${yestoAll} == false ]; then
	whiptail --title "KVM Configuration" --backtitle "Third Section" \
			 --yesno \
	"Summary: \n\
	3.1) Preparing a local template folder for the vmbuilder and post-build initialization. \n\
	3.2) Building/Importing VM-Images. \n\
	3.3) Inject further Scripts/Resources into each pre-build VM. \n\
	The whole process list will be executed on the next step. Continue?"\
	${GUI_MAXHEIGHT} ${GUI_MAXHWIDTH}
	choice=$?
	if [[ $choice != 0 ]]; then 
		echoAndLog "Aborted script, before user started Section 3." ${LOG_FILE}
		exit 5
	fi
fi


## SUBSECTION 3.1 ##
containsProgress ${PROGRESS_FILE} "3.1" containsSection
if [ ${containsSection} -eq 0 ]; then
	echoAndLog "3.1) Preparing a local template folder for the vmbuilder and post-build initialization..." ${LOG_FILE}
	
	su -c "mkdir -p ~/.vmbuilder/CoDECoM-Templates/libvirt" ${SUDO_USER}
	su -c "cp /etc/vmbuilder/libvirt/* ${TEMPLATE_PATH}/libvirt/" ${SUDO_USER}

	saveProgress ${PROGRESS_FILE} "3.1" "Preparing a local template folder for the vmbuilder and post-build initialization. DONE"
fi