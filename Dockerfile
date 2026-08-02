FROM maven:3.8-eclipse-temurin-8 AS build
WORKDIR /app

# 配置阿里云Maven镜像
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>\
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">\
    <mirrors>\
    <mirror>\
    <id>aliyun</id>\
    <mirrorOf>central</mirrorOf>\
    <name>Alibaba Cloud Maven Mirror</name>\
    <url>https://maven.aliyun.com/repository/public</url>\
    </mirror>\
    </mirrors>\
    </settings>' > /root/.m2/settings.xml

COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 1144
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
