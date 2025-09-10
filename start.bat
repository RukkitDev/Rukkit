@echo off
for %%i in (Rukkit-*.jar) do set JAR_FILE=%%i
java -Dfile.encoding=UTF-8 -Djava.library.path=./data/native -cp "%JAR_FILE%;libs/*" cn.rukkit.RukkitLauncher
pause