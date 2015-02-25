#!/bin/bash


######################################
##       Function Definitions:      ##
######################################

printHelp(){
	echo -e "\n\033[1mOpenVirteX Development starter script(Jan, 26th. 2015)\033[0m:"
	echo "This startup script launches the ovx.sh script inside currently active branch of"
	echo "https://github.com/opennetworkinglab/OpenVirteX and logs all output in a specific file."
	echo 
	echo "Basic Usage:"
    echo "\"bash start_ovx.sh [ARGS (optional)]\""
	echo
	echo "Arguments (optional):"
    echo -e "--logdir           Change the logging directory, which per default points to"
    echo -e             "\t\t   \"~/logs/ovx\""
    echo -e "-h | --help        Prints this help."

	exit 0
}


######################################
##     Argument Variable setup:     ##
######################################

# Command Line Variables (and default assignments):
LOGDIR=~/logs/ovx

### Command argument evaluation: ###
while [ $# -gt 0 ]; do 	# Until you run out of parameters
	case "$1" in
	#Optional Parameters:
        --logdir)         	LOGDIR=$2 ;;
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


echo "Starting OpenVirteX at ${NOW}..." 

sh ~/Development/openVirteX/scripts/ovx.sh
