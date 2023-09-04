#!/bin/bash
APP_HOME=/opt/fox-cloud
APP_PROFILES=dev

#nacos参数
nacos_server=127.0.0.1:8848

#切换当前目录
cd $APP_HOME

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


function runAll()
{
jar_name=ruoyi-auth.jar
log_nams=ruoyi-auth.jar.out
echo "开始启动：${jar_name}"
result=$(runApp ${jar_name} ${log_nams})
echo "启动完成：${jar_name}"


jar_name=ruoyi-gateway.jar
log_nams=ruoyi-gateway.jar.out
echo "开始启动：${jar_name}"
result=$(runApp ${jar_name} ${log_nams})
echo "启动完成：${jar_name}"


jar_name=ruoyi-modules-system.jar
log_nams=ruoyi-modules-system.jar.out
echo "开始启动：${jar_name}"
result=$(runApp ${jar_name} ${log_nams})
echo "启动完成：${jar_name}"

jar_name=ruoyi-visual-monitor.jar
log_nams=ruoyi-visual-monitor.jar.out
echo "开始启动：${jar_name}"
result=$(runApp ${jar_name} ${log_nams})
echo "启动完成：${jar_name}"

jar_name=fox-cloud-server-aggregator-1.0.0.jar
log_nams=fox-cloud-server-aggregator-1.0.0.jar.out
echo "开始启动：${jar_name}"
result=$(runApp ${jar_name} ${log_nams})
echo "启动完成：${jar_name}"

jar_name=fox-cloud-server-manager-service-1.0.0.jar
log_nams=fox-cloud-server-manager-service-1.0.0.jar.out
echo "开始启动：${jar_name}"
result=$(runApp ${jar_name} ${log_nams})
echo "启动完成：${jar_name}"
}

nohup $(runAll) > $APP_HOME/logs/output.log 2>&1 &
