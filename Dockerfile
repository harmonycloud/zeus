FROM zeus-base:1.0.1
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone
COPY target/*.jar /zeus.jar
COPY target/classes/components /usr/local/zeus-pv/components
COPY target/classes/middleware /usr/local/zeus-pv/middleware
COPY target/classes/config/zeus.jks /cfg/zeus.jks
COPY image-build/zeus-pv /usr/local/zeus-pv/
ENTRYPOINT [ "sh", "-c", "java -jar $JAVA_OPTS /zeus.jar" ]