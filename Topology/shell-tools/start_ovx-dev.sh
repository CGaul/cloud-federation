#!/bin/bash


######################################
##       Function Definitions:      ##
######################################

printHelp(){
	echo -e "\n\033[1mOpenVirteX-Federation Development starter script(Jan, 26th. 2015)\033[0m:"
	echo "This startup script launches the ovx.sh script inside the 0.1-DEV-Federation branch of"
	echo "https://github.com/CGaul/OpenVirteX and logs all output in a specific file."
	echo 
	echo "Basic Usage:"
    echo "\"bash start_ovx-dev.sh [ARGS (optional)]\""
    echo
    echo "Examples:"
    echo "Make a clean build before starting OpenVirteX"
    echo "(pulls git repo from github and re-builds OVX locally afterwards):"
	echo "\"sudo ./start_ovx-dev.sh --cleanbuild\""
	echo
	echo "Arguments (optional):"
    echo -e "-c|--cleanbuild    Pulls git repo from github and re-builds OVX locally afterwards"
    echo -e             "\t\t   The startup sequence takes longer, but you are on the safe side"
    echo -e             "\t\t   to have the newest build at your hands."
    echo -e "--logdir           Change the logging directory, which per default points to"
    echo -e             "\t\t   \"~/logs/ovx-dev\""
    echo -e "-h | --help        Prints this help."

	exit 0
}


######################################
##     Argument Variable setup:     ##
######################################

# Command Line Variables (and default assignments):
LOGDIR=~/logs/ovx-dev
CLEANBUILD=false

### Command argument evaluation: ###
while [ $# -gt 0 ]; do 	# Until you run out of parameters
	case "$1" in
	#Optional Parameters:
        --logdir)         	LOGDIR=$2 ;;
		-[c]|--cleanbuild)	CLEANBUILD=true ;;
	#Printing the Help:
		-[hH]|-help|--help)	printHelp ;;
	esac
	shift	# Check next set of parameters.
done

# Constants:
NOW=$(date +"%y-%m-%d_%H-%M-%S")
LOGFILE=${LOGDIR}/${NOW}.log


######################################
##      The Program Execution:      ##
######################################

mkdir -p ${LOGDIR}

#Redirect a copy of stdout and errout into a logfile files:
#(from: http://stackoverflow.com/questions/3173131/redirect-copy-of-stdout-to-log-file-from-within-bash-script-itself)
exec >  >(tee -a ${LOGFILE})
exec 2> >(tee -a ${LOGFILE} >&2)

if [[ ${CLEANBUILD} == true ]]; then
	echo "Cleaning up OpenVirteX-dev build and pull new version from git..."
	cd ~/Development/openVirteX-dev/
	rm -r target
	git pull
	cd ~
fi

echo "Starting OpenVirteX-dev at ${NOW}..." 
sh ~/Development/openVirteX-dev/scripts/ovx.sh
