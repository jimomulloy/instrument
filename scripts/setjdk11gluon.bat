@echo off
echo Setting JAVA_HOME
set JAVA_HOME=E:\jdk\graalvm-svm-java11-windows-gluon-22.1.0.1-Final
setx JAVA_HOME "E:\jdk\graalvm-svm-java11-windows-gluon-22.1.0.1-Final"
setx /M JAVA_HOME "graalvm-svm-java11-windows-gluon-22.1.0.1-Final"
set GRAALVM_HOME=%JAVA_HOME%
setx GRAALVM_HOME "%JAVA_HOME%"
setx /M GRAALVM_HOME "%JAVA_HOME%"
echo setting PATH
set PATH=%JAVA_HOME%\bin;%PATH%
setx PATH "%JAVA_HOME%\bin;%PATH%" 
setx /M PATH "%JAVA_HOME%\bin;%PATH%"
echo Display java version
java -version