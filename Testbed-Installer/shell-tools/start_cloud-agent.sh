#!/bin/bash


######################################
##       Function Definitions:      ##
######################################

printHelp(){
	echo -e "\n\033[1mFederated Cloud-Agents starter script(Jan, 26th. 2015)\033[0m:"
	echo "This startup script launches the pre build cloud-agents.jar from the development jenkins server"
	echo "and logs all output in a specific file."
	echo "The Jenkins Server made a build from the development branch of https://github.com/CGaul/cloud-federation before"
	echo 
	echo "Basic Usage:"
    echo "\"bash start_cloud-agent.sh --user jenkinsUser --password userPW [ARGS (optional)]\""
    echo
	echo
	echo "Arguments (optional):"
    echo -e "--logdir           Change the logging directory, which per default points to"
    echo -e             "\t\t   \"~/logs/cloud-agent\""
    echo -e "-h | --help        Prints this help."

	exit 0
}


######################################
##     Argument Variable setup:     ##
######################################

# Command Line Variables (and default assignments):
LOGPATH=~/logs/cloud-agent
USER=""
PASSWD=""

### Command argument evaluation: ###
while [ $# -gt 0 ]; do 	# Until you run out of parameters
	case "$1" in
	#Required Parameters:
        -[uU]|--user)		USER=$2 ;;
        -[pP]|--password)	PASSWD=$2 ;;
	#Optional Parameters:
        --logdir)         	LOGDIR=$2 ;;
	#Printing the Help:
		-[hH]|-help|--help)	printHelp ;;
	esac
	shift	# Check next set of parameters.
done

if [[ ${USER} == "" ]] || [[ ${PASSWD} == "" ]]; then
	echo -e "\n\033[1mUser and Password need to be set!\033[0m:"
	echo "See \"bash start_cloud-agent.sh --help\" for further details."
	exit 1
fi

# Constants:
JARNAME="cloud-agents_0.3-SNAPSHOT.fat.jar"

NOW=$(date +"%y-%m-%d_%H-%M-%S")
LOGFILE=${LOGPATH}/${NOW}.log


######################################
##      The Program Execution:      ##
######################################

# Test if oracle java-8 is installed:
if ! [[ -x /usr/lib/jvm/java-8-oracle/jre/bin/java ]]; then
    echo "java-8-oracle needs to be installed for this program to run! \n"
    echo "Under a debian system, use ppa:"
    echo "sudo add-apt-repository ppa:webupd8team/java"
    echo "sudo apt-get update"
    echo "sudo apt-get install oracle-java8-installer"
fi

JAVA8_EXEC=/usr/lib/jvm/java-8-oracle/jre/bin/java

# Establish the logpath for the program:
mkdir -p ${LOGPATH}


# Redirect a copy of stdout and errout into a logfile files:
# (from: http://stackoverflow.com/questions/3173131/redirect-copy-of-stdout-to-log-file-from-within-bash-script-itself)
exec >  >(tee -a ${LOGFILE})
exec 2> >(tee -a ${LOGFILE} >&2)


echo "Starting CCFM Cloud-Agent at ${NOW}..." 
wget --auth-no-challenge --user=${USER} --password=${PASSWD} -N http://lab.is-by.us:8080/job/Cloud-Federation/lastSuccessfulBuild/artifact/Cloud-Federation/Cloud-Agents/target/scala-2.11/${JARNAME} -P ~/Development/

$JAVA8_EXEC -jar ~/Development/${JARNAME} --appconf ~/Development/cloudnet-1_application.conf --cloudconf ~/Development/cloudconf

exit 0
