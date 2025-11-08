# Variante recommandée via Microsoft Build of OpenJDK
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
EXPOSE 8089
ADD target/gestion-station-ski-1.0.jar gestion-station-ski-1.0.jar
ENTRYPOINT ["java","-jar","/gestion-station-ski-1.0.jar"]
