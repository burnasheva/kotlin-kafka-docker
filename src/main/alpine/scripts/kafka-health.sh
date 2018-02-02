#!/bin/sh

BROKER_FOUND=$(/opt/kafka/bin/zookeeper-shell.sh localhost:2181 ls /brokers/ids|grep [0])

if [ "$BROKER_FOUND" = "[0]" ]
then
	exit 0
else
	exit 1
fi