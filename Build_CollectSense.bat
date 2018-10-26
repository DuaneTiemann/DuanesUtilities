echo package com.duane.utilities;                                                         >com\duane\utilities\CompileDate.java  
echo public class CompileDate{public static String getDate(){return ^"%date% %time%^";}} >>com\duane\utilities\CompileDate.java

javac -classpath javax.websocket-api-1.1.jar;. -Xdiags:verbose -Xlint -g com\duane\collectsense\CollectSense.java

jar -cfe CollectSense.jar com/duane/collectsense/CollectSense -C . com
jar -uf  CollectSense.jar                                     -C . Build_CollectSense.bat



