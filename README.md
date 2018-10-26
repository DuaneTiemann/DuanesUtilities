Collect*.jar files contain source and a build bat. Build_CollectSense.bat assumes the 3rd party jar files are in the current directory.
Everything was developed under Windows and the build files have some incidental Windows specific parts. I have not tested on Linux, Mac, etc.
The build just requires javac. No maven, ant, Eclipse, etc. I use java jdk 1.8. I don't know what the minimum level required is. I run on Windows 7.

Quality: I've been using (and tweaking) the infrastructure for years. It should work, but I know it isn't bulletproof yet. Invalid input can be
         tough to track down and can produce exceptions. I keep working to help track down problems better, but always pointing to the exact
         spot is still more of an aspiration. I wouldn't be surprised if there is a better environment to do what I want these days.

         I expect these will do until something better comes along. I'll keep refining. Maybe add a GUI.

         Feedback to duanetiemann@prodigy.net

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
The output is a tsv (tab separated values) file with column headers suitable for import into Excel.
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
                        Columns={IntervalStart DeviceName WattsMean}
                        File=CollectKasa.tsv
                       }
PollingIntervalSecs   =    1
Retries               =   10

  Collects for 3 days, summarizes by minute, rolls output file at midnight, appends output file name datetime stamps.
java -cp javax.websocket-api-1.1.jar;tyrus-standalone-client-1.9.jar;CollectSense.jar com.duane.collectsense.CollectSense <CollectSense.inp
CollectSense.inp:
Account               =<Your Sense Account Number>
CollectionIntervalSecs=   60
DurationSecs          = 259200
Email                 =<email for your account>
OutputFile            =CollectSense.tsv
OutputFileIntervalSecs=86400
Password              ="<password for your Sense account"           
Retries               = 10

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
