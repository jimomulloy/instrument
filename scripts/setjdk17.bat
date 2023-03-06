@echo off
echo Setting JAVA_HOME
set JAVA_HOME=E:\jdk\jdk-17
setx JAVA_HOME "E:\jdk\jdk-17"
setx /M JAVA_HOME "E:\jdk\jdk-17"
set PATH_TO_FX=E:\jdk\javafx-sdk-17.0.6
setx PATH_TO_FX "E:\jdk\javafx-sdk-17.0.6"
setx /M PATH_TO_FX "E:\jdk\javafx-sdk-17.0.6"
set PATH_TO_FX_MODS=E:\jdk\javafx-jmods-17.0.6
setx PATH_TO_FX "E:\jdk\javafx-sdk-17.0.6"
setx /M PATH_TO_FX_MODS "E:\jdk\javafx-jmods-17.0.6"
echo setting PATH
set PATH=%JAVA_HOME%\bin;%PATH%
setx PATH "%JAVA_HOME%\bin;%PATH%" 
setx /M PATH "%JAVA_HOME%\bin;%PATH%"
set PATH=%PATH%;C:\PROGRA~2\WiX Toolset v3.11\bin
setx PATH "%PATH%;C:\Program Files (x86)\WiX Toolset v3.11\bin" 
setx /M PATH "%PATH%;C:\Program Files (x86)\WiX Toolset v3.11\bin"
echo Display java version
java -version