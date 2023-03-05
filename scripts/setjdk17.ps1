$env:JAVA_HOME = 'C:\java\jdk-17'
$env:PATH_TO_FX = 'E:\jdk\javafx-sdk-17.0.6'
$env:PATH_TO_FX_MODS = 'E:\jdk\javafx-jmods-17.0.6'
$env:PATH = $env:JAVA_HOME+'\bin;'+$env:Path
java -version