@echo off
echo Setting JAVA_HOME
set JAVA_HOME=C:\java\jdk-11.0.17
set PATH_TO_FX=E:\jdk\javafx-sdk-11.0.2
set PATH_TO_FX_MODS=E:\jdk\javafx-jmods-11.0.2
echo setting PATH
set PATH=%JAVA_HOME%\bin;%PATH%
echo Display java version
java -version