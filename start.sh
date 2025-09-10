#!/bin/bash
JAR_FILE=$(ls Rukkit-*.jar | head -n 1)
java -Dfile.encoding=UTF-8 -Djava.library.path=./data/native -cp "$JAR_FILE;libs/*" cn.rukkit.RukkitLauncher