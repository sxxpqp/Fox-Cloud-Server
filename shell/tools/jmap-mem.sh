#!/bin/bash

app_name=/opt/fox-cloud-server/bin/fox-cloud-server-$1


#查找进程ID
pids=`ps -ef | grep $app_name | grep -v 'grep' | awk '{print $2}'`
for pid in $pids
do	
	jmap -histo:live $pid | head -20
	#ps aux|grep $pid
done


