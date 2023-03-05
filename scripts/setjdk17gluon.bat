@echo off
echo Setting JAVA_HOME
set JAVA_HOME=E:\jdk\graalvm-java17-windows-gluon-22.1.0.1-Final
setx JAVA_HOME "E:\jdk\graalvm-java17-windows-gluon-22.1.0.1-Final"
setx /M JAVA_HOME "E:\jdk\graalvm-java17-windows-gluon-22.1.0.1-Final"
setx GRAALVM_HOME "%JAVA_HOME%"
setx /M GRAALVM_HOME "%JAVA_HOME%"
echo setting PATH
setx PATH "%JAVA_HOME%\bin;%PATH%"
setx /M PATH "%JAVA_HOME%\bin;%PATH%"
echo Display java version
java -version