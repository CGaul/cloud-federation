#!/bin/bash

#author:    Constantin.Gaul
#version:   0.10
#date:      June, 20th. 2014


######################################
##       Function Definitions:      ##
######################################

printHelp(){
	echo "KVM VM-Builder v.0.1 (2014 Jun 20):"
	echo "Uses the JeOSVMBuilder in order to set up a VM with a given vmbuilder configuration file."
	echo "More information on JeOSVMBuilder: (https://help.ubuntu.com/community/JeOSVMBuilder"
	echo ""
	echo "Usage (short commands):"
	echo "kvm_vmbuild.sh [arguments] -n vmName -c myVMBuild.config"
	echo ""
	echo "Arguments (needed):"
	echo -e "-c | --config		KVM Configuration Script of the new build VM."
	echo -e 			"\t\t	IMPORTANT NOTE: If optional arguments are used, "
	echo -e 			"\t\t	the specific config lines must have defined the following"
	echo -e 			"\t\t	placeholders to be used for the respective fields: "
	echo -e 			"\t\t	\"HOSTNAME\", \"FIRSTBOOT\", \"FIRSTLOGIN\", \"IPADDR\", \"GATEWAY\". "
	echo -e "-h | --hypervisor  The hypervisor that will manage the VM. Usually \"kvm\"."
	echo -e "-d | --distro      The distribution of the Operating-System, which the VM is based on."
	echo ""
	echo "Arguments (optional):"
	echo -e "-n | --name	    The name of the new build VM"
	echo -e 			"\t\t	Given this param on startup, config file needs to contain field \"HOSTNAME\"."
	echo -e "-p | --imagePath	Defines the path where to store the vmbuilder image."
	echo -e 			"\t\t	If not specified, \"~/.vmbuilder/VMs\" will be used."
	echo -e "-i | --ipaddr		Pass the IP-Addr of the new build VM to the myVMBuild.config."
	echo -e 			"\t\t	Given this param on startup, config file needs to contain field \"IPADDR\"."
	echo -e "-g | --gateway		Pass the Gateway of the new build VM to the myVMBuild.config."
    echo -e 			"\t\t	Given this param on startup, config file needs to contain field \"GATEWAY\"."
	echo -e "-b | --firstboot	Pass a firstboot.sh script to the vmbuilder,"
	echo -e 			"\t\t	appending it to myVMBuild.config"
    echo -e 			"\t\t	Given this param on startup, config file needs to contain field \"FIRSTBOOT\"."
	echo -e "-l | --firstlogin	Pass a firstlogin.sh script to the vmbuilder,"
	echo -e 			"\t\t	appending it to myVMBuild.config"
	echo -e 			"\t\t	Given this param on startup, config file needs to contain field \"FIRSTLOGIN\"."
	echo -e "-s | --suite       The suite of the system. E.g. \"precise\""
	echo -e 			"\t\t	Given this param on startup, config file needs to contain field \"SUITE\"."
	echo -e "--injectKVMConsole	Injects a ttyS0.conf with Console Port 115200 to the new build VM."

	exit 0
}

transformToSafeSed(){
	sedCmd=$1
	safeSedCmd=$(printf '%s\n' "${sedCmd}" | sed 's/[[\.*/]/\\&/g; s/$$/\\&/; s/^^/\\&/')
	eval "$2='${safeSedCmd}'"
}


######################################
##     Argument Variable setup:     ##
######################################

### Relevant Variables: ###
vmName=""
configFile=""
hypervisor=""
distro=""

### Optional Variables: ###
ipAddr=""
gateway=""
firstbootFile=""
firstloginFile=""
suiteName=""
vmImagePath=~/.vmbuilder/VMs
consoleTTY=""

### Command argument evaluation: ###
while [ $# -gt 0 ]; do 	# Until you run out of parameters
	case "$1" in
	#Relevant Parameters:
		-[c]|--config)		configFile="$2" ;;
		-[h]|--hypervisor)  hypervisor="$2" ;;
		-[d]|--distro)      distro="$2" ;;
	#Optional Parameters:
	    -[n]|--name)		vmName="$2" ;;
		-[p]|--imagePath)	vmImagePath="$2" ;;
		-[i]|--ipaddr)		ipAddr="$2" ;;
		-[g]|--gateway)		gateway="$2" ;;
		-[b]|--firstboot)	firstbootFile="$2" ;;
		-[l]|--firstlogin)	firstloginFile="$2" ;;
		-[s]|--suite)       suiteName="$2" ;;
		--injectKVMConsole)	consoleTTY="$2" ;;
	#Printing the Help:
		-[hH]|-help|--help)	printHelp ;;
	   #*)					configFile="$1"
	esac
	shift	# Check next set of parameters.
done


######################################
##      The Program Execution:      ##
######################################

## Program call-evaluation: ##
##############################

### Only runnable as root: ###
#----------------------------#

CURR_USER=$(id -un)

if ! [ "${CURR_USER}" = "root" ]; then
	echo "Application must be run as root! Current user is \"${CURR_USER}\". Aborting..."
	exit 1
fi

### Only runnable when relevant Parameters assigned: ###
#------------------------------------------------------#

if [[ "${configFile}" == "" || "${hypervisor}" == "" || "${distro}" == "" ]]; then
    echo "Not all script parameters have been correctly set! "
    echo "see \"./kvm_vmbuild.sh --help\" to see how."
    exit 2
fi


## Program main environment ##
##############################

echo
echo "VM Builder for KVM/Qemu"
echo "======================="

echo "VM Image-Path: "${vmImagePath}
echo "Config-File: "${configFile}

#If the configFile is given, prepare the build, otherwise exit the program:
if [[ "${configFile}" != "" ]]; then
	#Extract the config filename:
	configFileName=`echo "${configFile}" | awk -F"/" '{print $NF}'`

	#Find the name of the VM to be build (called the "hostname"):
	#cat streamer.vmbuild.sh | awk -v pat="hostname=" '$0 ~ pat{p=1} p' | awk 'BEGIN {FS = "[=]" } {print $2}' | sed -n 1p | awk '{print $1}' #(used to find in vmbuild.sh files)

	if [[ "${vmName}" == "" ]]; then
	    #If the vmName was not set as a parameter, read it from the config file (needs to be there in that case):
	    vmName=`cat ${configFile} | awk -v pat="hostname" '$0 ~ pat{p=1} p' | sed -n 1p | awk '{print $3}'`
	    echo -e "Found Hostname in ${configFile}: \t ${vmName}."

        if [[ "${vmName}" == "" ]]; then
            #If vmName is still empty, something went wrong in reading the hostname from the config.
            echo "VMName is not specified in config-file (via \"hostname\") and was not given as parameter. Exiting vmbuilder!"
            exit 3
        fi
    else
        #If the vmName is given as a parameter on startup, replace HOSTNAME with vmName in Config:
        sed -i 's/HOSTNAME/'${vmName}\/ ${configFile}
    fi

    #If the name was found, create a dir for that VM under "vmImagePath":
    vmDir=${vmImagePath}/${vmName}
    echo -e "Creating dir ${vmDir}..."
    su -c "mkdir -p ${vmDir}" ${SUDO_USER}

    #Copying the config file to the local VM-Path:
    su -c "cp ${configFile} ${vmDir}/" ${SUDO_USER}
    configFile=${vmDir}/${configFileName}
    echo "Copied config-File. New location is: ${configFile}"

    #Fill that dir with all scripts and configs that were handed over to this vmbuild-tool:
    #On top of that, fill in all optional arguments in the vmbuild.cfg file:

    if [[ "${ipAddr}" != "" ]]; then
        #For the optional Parameter "IPADDR", fill in the field in the config file:
        echo "Replacing IPADDR-Flag in ${configFile} with ${ipAddr}..."
        sed -i "s/IPADDR/${ipAddr}/g" ${configFile}
    fi

    if [[ ${gateway} != "" ]]; then
        #For the optional Parameter "GATEWAY", fill in the field in the config file:
        echo "Replacing GATEWAY-Flag in ${configFile} with ${gateway}..."
        sed -i "s/GATEWAY/${gateway}/g" ${configFile}
    fi

    if [[ ${firstbootFile} != "" ]]; then
        #Import the firstboot-file into the VM-dir:
        su -c "cp ${firstbootFile} ${vmDir}" ${SUDO_USER}
        firstbootFileName=`echo "${firstbootFile}" | awk -F"/" '{print $NF}'`
        firstbootFile=${vmDir}/${firstbootFileName}
        safe_firstbootFile=$(printf '%s\n' "$firstbootFile" | sed 's/[[\.*/]/\\&/g; s/$$/\\&/; s/^^/\\&/')

        #Use this file for the optional Parameter "FIRSTBOOT" and fill in the field in the config file:
        echo "Replacing FIRSTBOOT-Flag in ${configFile} with ${firstbootFile}..."
        sed -i 's/FIRSTBOOT/'${safe_firstbootFile}\/ ${configFile}
    fi

    if [[ ${firstloginFile} != "" ]]; then
        #Import the firstboot-file into the VM-dir:
        su -c "cp ${firstloginFile} ${vmDir}" ${SUDO_USER}
        firstloginFileName=`echo "${firstloginFile}" | awk -F"/" '{print $NF}'`
        firstloginFile=${vmDir}/${firstloginFileName}
        transformToSafeSed ${firstbootFile} safeFirstloginFile

        #Use this file for the optional Parameter "FIRSTLOGIN" and fill in the field in the config file:
        echo "Replacing FIRSTLOGIN-Flag in ${configFile} with ${firstloginFile}..."
        sed -i 's/FIRSTLOGIN/'${safeFirstloginFile}\/ ${configFile}

    fi

    if [[ ${suiteName} != "" ]]; then
        #For the optional Parameter "SUITE", fill in the field in the config file:
        echo "Replacing SUITE-Flag in ${configFile} with ${suiteName}..."
        sed -i "s/SUITE/${suiteName}/g" ${configFile}
    fi


    #After all optional Configs / Parameters are perpared, launch the vmbuilder:
    echo "Executing vmbuild with prepared config: ${configFile}..."
    vmbuilder ${hypervisor} ${distro} -c ${configFile} -d ${vmDir}/${distro}-${hypervisor}


    #When build is done and "--injectKVMConsole" is true, inject the configurations for
    #KVM Console management:
    #qemuConfig=`cat /etc/libvirt/qemu/${vmName}.xml | awk -v pat="<devices>" '$0 ~ pat{p=1} p'`
    if [[ ${consoleTTY} != "" ]]; then

        #Injects the prepared Console TTY to /etc/init in the Guest-VM space:
        sudo virt-copy-in -d ${vmName} ${consoleTTY} /etc/init/

        #Also changes the console to serial mode in the <devices> section of /etc/libvirt/qemu/${vmName}.xml
        consoleSedString="<devices><serial type='pty'><target port='0'\/><\/serial><console type='pty'><target type='serial' port='0'\/><\/console>"

        #Make security copy before:
        cp /etc/libvirt/qemu/${vmName}.xml > /etc/libvirt/qemu/${vmName}.xml.save
        #Inject sedString for serial-console in qemu-config.xml:
        sed -e "s/<devices>/${consoleSedString}/g" /etc/libvirt/qemu/${vmName}.xml.save > /etc/libvirt/qemu/${vmName}.xml
        #Diff both files:
        diff /etc/libvirt/qemu/${vmName}.xml /etc/libvirt/qemu/${vmName}.xml.save
    fi
else
	echo "Config-Path and VM Name are relevant parameters and have to be given as attributes!"
	echo "See the help (--help) for calling this tool."	
	exit 1
fi
