@echo off
echo Setting JAVA_HOME
set JAVA_HOME=E:\jdk\jdk-19
setx JAVA_HOME "E:\jdk\jdk-19"
setx /M JAVA_HOME "E:\jdk\jdk-19"
echo setting PATH
set PATH=%JAVA_HOME%\bin;%PATH%
setx PATH "%JAVA_HOME%\bin;%PATH%"
setx /M PATH "%JAVA_HOME%\bin;%PATH%"
echo Display java version
java -version