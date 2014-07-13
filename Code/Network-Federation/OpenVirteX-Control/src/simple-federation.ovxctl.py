python ovxctl.py -n createNetwork tcp:localhost:10000 10.0.1.0 16
python ovxctl.py -n createSwitch 1 00:00:00:00:00:00:11:00,00:00:00:00:00:00:12:00,00:00:00:00:00:00:13:00,00:00:00:00:00:00:14:00
python ovxctl.py -n createPort 1 00:00:00:00:00:00:12:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:12:00 2
python ovxctl.py -n createPort 1 00:00:00:00:00:00:13:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:00:14:00 1
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:01
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:02
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:03
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:04
python ovxctl.py -n startNetwork 1