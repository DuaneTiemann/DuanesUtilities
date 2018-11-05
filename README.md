Collect*.jar files contain source and a build bat. Build_CollectSense.bat assumes the 3rd party jar files are in the current directory.
Everything was developed under Windows and the build files have some incidental Windows specific parts. I have not tested on Linux, Mac, etc.
The build just requires javac. No maven, ant, Eclipse, etc. I use java jdk 1.8. I don't know what the minimum level required is. I run on Windows 7.

Jar files are available at https://drive.google.com/open?id=1YEqEQb3opZ-8zBCVRfJtuSP9Ww9CL7-0

Changes: 

2018/10/26 All          Initial version

2018/10/30 All          Repaired horible performance issue writing to output files
                        Added the Flush Keyword for files to manage performance vs currency

           CollectKasa  Improved error recovery

2018/11/02 All          Changed output file record timestamp to include time zone
                        It can be tweaked to be Excel compatible. AFAICT Excel doesn't allow time zones.
                        See CollectSense example below.
                        
                        Repaired log rolling interval. It was GMT based. Now it respects the current time zone.
                        
           CollectKasa  Normalized current, voltage, power, total for V2. The V2 reports milli-amps, etc. Divided by 1000 for V2.
           CollectSense Removed Account keyword.  It will fail now if you try to supply Account. Account was a bogus variable.

2018/11/04 All          Output file rolling still wasn't right. It was repeating the 1st file name and then lagged the file name. 
                        This resulted in the first file being overwritten and all files contained data that should have been in the prior file.
                        Reversed order of renaming output files and updating Interval.

Quality: It should work, but I know it isn't bulletproof yet. Invalid input can be tough to track down and can produce exceptions.
         I keep working to help track down problems better, but always pointing to the exact spot is still more of an aspiration. 
         I wouldn't be surprised if there is a better environment to do what I want these days.

         I expect these will do until something better comes along. I'll keep refining. Maybe add a GUI.

Feedback: duanetiemann@prodigy.net

Invocations:
java -jar CollectKasa.jar
java -jar CollectWemo.jar
java -cp javax.websocket-api-1.1.jar;tyrus-standalone-client-1.9.jar;CollectSense.jar com.duane.collectsense.CollectSense

Above will result in comprehensive, but terse, help messages.

Parameters are supplied via command line or stdin and can become extensive.

The Wemo app gives you its IP address, but the Kasa TP-LINK HS-110 app doesn't. It does give you its MAC address. And it's on the
back of the device. On Windows you can get the IP address by pinging your network broadcast address (usually 192.168.1.255) and 
then looking at the Arp map. e.g.

         ping 192.168.1.255 (wait for timeouts)
         arp -a

The utilities rely on the same home grown infrastructure that allows manuipulation of output before it is written.
More columns are supplied than are likely to be useful. It is EXPECTED that they will usually be filtered. (See Kasa sample below.)
Note that multiple OutputFiles are allowed, though they all respect the same roll parameter.

Note Kasa and Wemo are polled. Sense is websocket and samples arrive every .5s.

Sample uses:

  Collects for an hour 
java -jar CollectKasa.jar <CollectKasa.inp
CollectKasa.inp:
CollectionIntervalSecs=    1
DurationSecs          = 3600
DeviceName            =DuanesComputerStuff
IP                    =<IP Address of TP-LINK HS-110 (Kasa) Device>
OutputFile            ={
                        Columns={IntervalStart DeviceName MeanWatts}
                        File=CollectKasa.tsv
                       }
PollingIntervalSecs   =    1
Retries               =   10

  Collects forever, summarizes by minute, rolls output file at midnight, appends output file name datetime stamps, Excel timestamp.
java -cp javax.websocket-api-1.1.jar;tyrus-standalone-client-1.9.jar;CollectSense.jar com.duane.collectsense.CollectSense <CollectSense.inp
CollectSense.inp:
CollectionIntervalSecs    =   60
DurationSecs              =    0
Email                     =<email for your account>
OutputFile                ={
                            File=CollectSense.tsv
                            Result={
                                    Name=ExcelTime
                                    Operation={
                                               {Name=IntervalStartTime Operation=split       Value=" "}
                                               {                       Operation=pop                  } // discard time zone
                                               {Name=time              Operation=pop                  } // time 
                                               {                       Operation=concatenate Value=" "}
                                               {Name=time              Operation=concatenate          } 
                                              }
                                   }

                           }
OutputFileRollIntervalSecs=86400
Password                  ="<password for your Sense account"           
Retries                   =   14

java -jar c:\Duane\Utilities\Distribution\CollectWemo.jar <CollectWemo.inp
CollectWemo.inp:
CollectionIntervalSecs=  5
DeviceName            =TVComplex
DurationSecs          = 60
IP                    =<Your Wemo IP Address>
OutputFile            =CollectWemo.tsv
PollingIntervalSecs   =  1
Port                  =49153
Retries               = 10
