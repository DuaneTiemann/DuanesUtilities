echo package com.duane.utilities;                                                         >com\duane\utilities\CompileDate.java  
echo public class CompileDate{public static String getDate(){return ^"%date% %time%^";}} >>com\duane\utilities\CompileDate.java

javac -classpath . -Xdiags:verbose -Xlint -g com\duane\collectwemo\CollectWemo.java

jar -cfe CollectWemo.jar com/duane/collectwemo/CollectWemo -C . com
jar -uf  CollectWemo.jar                                   -C . Build_CollectWemo.bat




