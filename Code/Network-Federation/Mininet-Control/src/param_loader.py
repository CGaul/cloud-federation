__author__ = 'costa'

#Python system imports:
import getopt
import sys



## CALL ARGUMENT SETUP ##
#########################

def defineArgs(argv):
    ofc_ip   = 'localhost'
    ofc_port = 6633

    try:
        opts, args = getopt.getopt(argv,"hi:p:",["help",
                                                 "ofc_ip=", "ofc_port="])
    except getopt.GetoptError as e:
        print ("Parsing Error of command parameters: %s", e)
        sys.exit(2)
    for opt, arg in opts:
        if opt in ('-h', "--help"):
            # justs prints help and exits
            #_printHelp()
            sys.exit(0)
        elif opt in ('-i', "--ofc_ip"):
            ofc_ip = str(arg).replace('\'','').replace('\"','')

        elif opt in ('-p', "--ofc_port"):
            ofc_port = str(arg).replace('\'','').replace('\"','')

    return ofc_ip, ofc_port

