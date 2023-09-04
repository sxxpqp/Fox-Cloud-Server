#!/bin/bash
APP_HOME=/opt/fox-cloud
APP_PROFILES=dev

#nacos参数
nacos_server=127.0.0.1:8848

#切换当前目录
cd $APP_HOME

function killApp()
{
	#特征值
	feature=$1;
	
	#生成查询进程的命令行
	shell=`ps -ef|grep $feature|grep -v grep|awk '{print $2}'`
	
	#执行该命令行，获得这些进程的ID列表
	result=$shell
	
	#判定结果：然后kill这些进程ID
	if [[ -n ${result:1:1} ]]; then 
		kill -9 $shell
	fi
}

function runApp()
{
	#定义变量
	jar_name=$1;
	log_nams=$2;
	start_result=

	#启动java程序
	nohup \
	java -jar \
	-Dspring.profiles.active=$APP_PROFILES \
	$APP_HOME/bin/$jar_name \
	--spring.cloud.nacos.discovery.server-addr=$nacos_server --spring.cloud.nacos.config.server-addr=$nacos_server \
	>$APP_HOME/logs/$log_nams 2>&1 & 
	
	#检查启动结果:当日志文件还没有出现启动成功字样，则继续等待
	for((integer = 1; integer <= 50; integer++))  
	do  
		#从文件中查询成功标识的字符串
		start_result=$(find ./logs -name $log_nams | xargs grep "启动成功")
		
		#如果字符串找到了，则退出循环，并输出一段字符串
		if [[ -n ${start_result:1:1} ]]; then 
			echo $start_result
			break;
		fi

		sleep 5
	done  
} 

jar_name=${1}
log_nams=$jar_name.out

result=$(killApp ${APP_HOME}/bin/${jar_name} )
nohup $(runApp ${jar_name} ${log_nams}) > $APP_HOME/logs/output.log 2>&1 &
