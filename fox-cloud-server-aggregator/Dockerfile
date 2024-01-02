FROM pig4cloud/java:8-jre

MAINTAINER wangiegie@gmail.com

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms1024m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && mkdir -p /fox-cloud-server-aggregator
WORKDIR /fox-cloud-server-aggregator

EXPOSE 9501

ADD ./target/fox-cloud-server-aggregator-1.1.0.jar ./

CMD java $JAVA_OPTS -jar fox-cloud-server-aggregator-1.1.0.jar
