echo package com.duane.utilities;                                                         >com\duane\utilities\CompileDate.java  
echo public class CompileDate{public static String getDate(){return ^"%date% %time%^";}} >>com\duane\utilities\CompileDate.java
javac -classpath . -Xdiags:verbose -Xlint -g com\duane\collectkasa\CollectKasa.java
jar -cfe CollectKasa.jar com/duane/collectkasa/CollectKasa -C . com
jar -uf  CollectKasa.jar                                   -C . Build_CollectKasa.bat



