@echo off
echo Setting JAVA_HOME
set JAVA_HOME=E:\jdk\graalvm-ce-java19-22.3.1
setx JAVA_HOME "E:\jdk\graalvm-ce-java19-22.3.1"
setx /M JAVA_HOME "E:\jdk\graalvm-ce-java19-22.3.1"
set GRAALVM_HOME=%JAVA_HOME%
setx GRAALVM_HOME "%JAVA_HOME%"
setx /M GRAALVM_HOME "%JAVA_HOME%"
echo setting PATH
set PATH=%JAVA_HOME%\bin;%PATH%
setx PATH "%JAVA_HOME%\bin;%PATH%" 
setx /M PATH "%JAVA_HOME%\bin;%PATH%"
echo Display java version
java -version