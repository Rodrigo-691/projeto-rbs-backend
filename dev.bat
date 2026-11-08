@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Iniciando Planeta Atlantida Backend (Spring Boot 3 + Java 17)...
call .\mvnw.cmd spring-boot:run
