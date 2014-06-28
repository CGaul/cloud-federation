#!/bin/bash

##########################################################
## This Script provides helper (auxillary) functions    ##
## for the Test-Bed installer in order to keep the      ##
## main script clean and understandable.                ##
##########################################################

#author:    Constantin Gaul
#version:   0.20
#date:      June, 28th. 2014



## Function Definitions: ##
###########################

##String Functions: ##
echoAndLog(){
	MSG=$1
	LOGFILE=$2

	#Echo file first:
	echo ${1}
	#And append to logfile afterwards:
	echo ${1} >> ${LOGFILE}
}

transformToSafeSed(){
	sedCmd=$1
	safeSedCmd=$(printf '%s\n' "${sedCmd}" | sed 's/[[\.*/]/\\&/g; s/$$/\\&/; s/^^/\\&/')
	eval "$2='${safeSedCmd}'"
}

##Finding Dirs: ##
findCurrentDir() {
	SOURCE="${BASH_SOURCE[0]}"

	# resolve $SOURCE until the file is no longer a symlink:
	while [ -h "$SOURCE" ]; do
	  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
	  SOURCE="$(readlink "$SOURCE")"

	  # if $SOURCE was a relative symlink,
	  #we need to resolve it relative to the path where the symlink file was located:
	  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
	done
	CURR_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
	eval "$1='${CURR_DIR}'" #Return the parameter as an argument reference to $1
}

findHomeDir() {
	USER_NAME=$(id -un)

	#Examine user home-dir:
	if [ "${USER_NAME}" = "root" ]; then
		USER_DIR=/root
	else
		USER_DIR=/home/${USER_NAME}
	fi

	eval "$1='${USER_DIR}'" #Return the parameter as an argument reference to $1
}


##Logging & Progress handling: ##
saveProgress() {
	PROGRESS_FILE=$1
	PROGRESS=$2
	DESCRIPTOR=$3

	echo ${PROGRESS}" "${DESCRIPTOR} >> ${PROGRESS_FILE}
}

hasProgress() {
	PROGRESS_FILE=$1

	if [ -f ${PROGRESS_FILE} -a -s ${PROGRESS_FILE} ]; then
		eval "$2='1'"
	else
		eval "$2='0'"
	fi
}

loadProgress() {
	PROGRESS_FILE=$1

	#Check, if the PROGRESS_FILE exists and has a greater size then 0:
	#(see "man test" for details about -f, -s and -z)
	if [ -f ${PROGRESS_FILE} -a -s ${PROGRESS_FILE} ]; then
		#Then check, if there is at least any content in the file:
		#if  [ -n "${PROGRESS_FILE}" ]; then
			#If so, return the progress list by reference:
			#PROGRESS=`cat ${PROGRESS_FILE} | sed -n 3p`
		PROGRESS=`cat ${PROGRESS_FILE} | awk '{print $1}'`
		eval "$2='${PROGRESS}'" #Return the parameter as an argument reference to $2
		#else
			#If the file does not contain anything, return 0 by reference
		#    eval "$2='0'"
		#fi
	else
		#If the file is non-existent, create it and return 0 by reference
		touch ${PROGRESS_FILE}
		eval "$2='0'"
	fi
}

containsProgress() {
	PROGRESS_FILE=$1
	SEARCHED_PROG=$2

	#Sums up the number of occurrences of SEARCHED_PROG, using an awk pipe:
	MATCHES=`cat ${PROGRESS_FILE} | awk -v pat="$SEARCHED_PROG" -v nmatches=0 '$1 ~ pat {nmatches++} END {print nmatches}'`
	eval "$3='${MATCHES}'"
}


##User Interactions: ##
queryUserCont_yN() {
	noEcho=$1

	read cont
	#If the user does not query y/Y, the script will be aborted, echoing the arg$1 message.
	if ! [ "${cont}" = "y" -o "${cont}" = "Y" ]; then
		echo ${noEcho}
		exit 5
	fi
}

queryUserCont_Yn() {
	noEcho=$1

	read cont
	#If the user does not query y/Y, the script may be aborted, echoing the arg$1 message, however...
	if ! [ "${cont}" = "y" -o "${cont}" = "Y" ]; then
		#...in a Y/n query, it is also possible to just hit ENTER:
		if ! [ "${cont}" = "\n" -o "${cont}" = "\r" ]; then
			echo ${noEcho}
			exit 5
		fi
	fi
}