FROM demo.harmonycloud.cn:32088/library/zeus-base:1.2.0
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone
COPY target/*.jar /zeus.jar
COPY error-num-language-pkg /error-num-language-pkg
COPY target/classes/components /usr/local/zeus-pv/components
COPY target/classes/middleware /usr/local/zeus-pv/middleware
COPY target/classes/clusterRole /usr/local/zeus-pv/clusterRole
COPY target/classes/config/zeus.jks /cfg/zeus.jks
COPY image-build/zeus-pv /usr/local/zeus-pv/
ENTRYPOINT [ "sh", "-c", "java -jar $JAVA_OPTS /zeus.jar" ]