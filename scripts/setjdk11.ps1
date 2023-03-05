$env:JAVA_HOME = 'C:\java\jdk-11'
$env:PATH_TO_FX = 'E:\jdk\javafx-sdk-11.0.2'
$env:PATH_TO_FX_MODS = 'E:\jdk\javafx-jmods-11.0.2'
$env:PATH = $env:JAVA_HOME+'\bin;'+$env:Path
java -version