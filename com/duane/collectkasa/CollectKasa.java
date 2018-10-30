package com.duane.collectkasa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.String;
import java.util.Arrays;
import java.util.Date;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.duane.utilities.CollectionInterval;
import com.duane.utilities.CompileDate;
import com.duane.utilities.CV;
import com.duane.utilities.Initializable;
import com.duane.utilities.JsonParser;
import com.duane.utilities.OutputTsvFileNamer;
import com.duane.utilities.Max;
import com.duane.utilities.Mean;
import com.duane.utilities.Min;
import com.duane.utilities.MyIOException;
import com.duane.utilities.MyParseException;
import com.duane.utilities.N;
import com.duane.utilities.OutputTsvFile;
import com.duane.utilities.ParmParser;
import com.duane.utilities.ParmParserParm;
import com.duane.utilities.Stats;
import com.duane.utilities.Utilities;

@ParmParserParm("Invocation: java -jar CollectKasa.jar parmaeters...\t"
               +"retrieves real time data from Kasa/TP-LINK smart plug HS-110 V1 & V2")
public class CollectKasa implements Initializable
{
                                                                          private long                Count                        = 0;
 @ParmParserParm("true|(false)"                                         ) private static boolean      Debug                        = false;
 @ParmParserParm("Number of seconds to include for each data record (5)\t"
                +"0=> output every sample"                              ) private double              CollectionIntervalSecs       = 5;
                                                                          private long                CollectionIntervalMs         = 1000;
 @ParmParserParm("Name of monitoring target (Kasa)"                     ) private String              DeviceName                   = "Kasa";
 @ParmParserParm("(300)Number of seconds to collect data 0=>forever"    ) private long                DurationSecs                 = 300;
                                                                          private InputStream         InputStream                  = null;
                                                                          private InetAddress         IPInetAddress                = null;
 @ParmParserParm("Kasa IP Address"                                      ) private String              IP                           = null;
                                                                          private CollectionInterval  OutputCollectionInterval     = null;
                                                                          private CollectionInterval  OutputFileCollectionInterval = null;
                                                                          private long                OutputFileIntervalMs         = 0;
 @ParmParserParm("Interval to switch to new OutputFile(s)(NoRoll)\t"
                +" 0     => persistent file with start time in file name\t"      
                +">0     => start and intended end in file name(s)(rolling)\t"
                +"NoRoll => persistent file with no timestamp\t" 
                +"If multiple OutputFiles are specified, they all respect this parameter")
                                                                          private String              OutputFileRollIntervalSecs   = "noroll";
                                                                          private OutputStream        OutputStream                 = null;
                                                                          private OutputTsvFileNamer  OutputTsvFileNamer           = new OutputTsvFileNamer();
                                                                          private Socket              Socket                       = null;
 @ParmParserParm("(required) File(s) to receive output\t"
                +"file is locked per below\t"
                +"Columns: IntervalStart IntervalEnd DeviceName CVWatts MaxWatts MeanWatts MinWatts N NNonZero OutputTime\t"
                +"         following raw values are from the last sample in the collection interval:\t"
                +"         Current Voltage Power Total"                 ) private OutputTsvFile[]     OutputFile                   = null;
 @ParmParserParm("(1)"                                                  ) private double              PollingIntervalSecs          = 1;
                                                                          private Response            LastResponse                 = null;
 @ParmParserParm("(5) Retries after 1s, 2s, 4s, ..."                    ) private int                 Retries                      = 5;
                                                                          private Stats               Stats                        = new Stats();
                                                                          private final static String Version                      = "1.0.0";

 private class Response
 {
  public String Current=null;
  public String Voltage=null;
  public String Power  =null;
  public String Total  =null;

  public Response(String current, String voltage, String power, String total)
  {
   Current=current;
   Voltage=voltage;
   Power  =power  ;
   Total  =total  ;
  }
 }

 private void closeOutputFiles()
 {
  for (OutputTsvFile outputFile : OutputFile)
      {
       outputFile.flush();
       outputFile.close();
      }
 }

 public CollectKasa(){super();}

 public void finalize() throws Throwable {super.finalize();}

 public void initialize(){}

 private String getResponse(String request)
 throws IOException
 {
  if (Debug)Utilities.debug("CollectKasa.getResponse enter");

  if (Socket != null &&
     !Socket.isConnected())
     {
      if (Debug)Utilities.debug("CollecKasa.getResponse Socket disconnected");
      setupSocket();
     }

  if (Socket == null)setupSocket();

  byte[]       buffer           = new byte[2000];
  byte[]       requestBytes     = request.getBytes();
  byte[]       encryptedBytes   = Utilities.encryptKasaBytes(requestBytes);
  byte[]       length           = new byte[]{0,0,0,(byte)encryptedBytes.length}; // sloppy. Only works for strings under 128 bytes.
  byte[]       fullRequestBytes = Utilities.concatenateArrays(length,encryptedBytes);

  OutputStream.write(fullRequestBytes);

  byte[]       lengthBytesBuffer = new byte[4];     
  int          len               = InputStream.read(lengthBytesBuffer);
  if (len == -1)
     {     // retry once because V1 drops the connection.
      setupSocket();
      OutputStream.write(fullRequestBytes);
      len = InputStream.read(lengthBytesBuffer);
     }
  if (len != 4)
     {
      Utilities.error("CollectKasa.getResponse unable to get response length{"
                              +"len="+len+"}");
      setupSocket();
      return null;
     }
  int          responseLength    = Utilities.bytesToInt(lengthBytesBuffer);
  if (responseLength > 1500 ||
      responseLength <    0)Utilities.fatalError("CollectKasa.getResponse suspicious response length{"
                                                +"lengthBytesBuffer="+Utilities.toString(lengthBytesBuffer)+"\t"
                                                +"responseLength="   +responseLength                       +"}");

  byte[]       responseBytes     = new byte[responseLength];
  int          amountRead        = 0;

  while (amountRead < responseLength)
        {
         int amountRemaining = responseLength - amountRead;
         len = InputStream.read(responseBytes,amountRead,amountRemaining);

         if (len == -1){Utilities.error("CollectKasa.getResponse EOF before end of response{"
                                       +"amountRead="    +amountRead    +"\t"
                                       +"responseLength="+responseLength+"}");
                        return null;}
         amountRead += len;
        }

  byte[]       decryptedBytes   = Utilities.decryptKasaBytes(responseBytes);
  String       result           = new String(decryptedBytes);

  if (Debug)Utilities.debug("getResponse{"
                           +"decryptedBytes="  +Utilities.bytesToHex(decryptedBytes  )+"\t"
                           +"fullRequestBytes="+Utilities.bytesToHex(fullRequestBytes)+"\t"
                           +"IPInetAddress="   +Utilities.toString  (IPInetAddress   )+"\t"
                           +"length="          +Utilities.bytesToHex(length          )+"\t"
                           +"request="         +request                               +"\t"
                           +"requestBytes="    +Utilities.bytesToHex(requestBytes    )+"\t"
                           +"responseBytes="   +Utilities.bytesToHex(responseBytes   )+"\t"
                           +"result="          +result                                +"}");
  return result;
 }

 public static void main(String[] argv)
 {
  Utilities.help(argv,CollectKasa.class);
  Utilities.debug(false);

  try {((CollectKasa)ParmParser.createObject(CollectKasa.class, argv)).run();}
  catch (IllegalAccessException e){Utilities.fatalError(Utilities.toString(e));}
  catch (IOException            e){Utilities.fatalError(Utilities.toString(e));}
  catch (MyIOException          e){Utilities.fatalError(Utilities.toString(e));}
  catch (MyParseException       e){Utilities.fatalError(Utilities.toString(e));}
 }

 private void noteColumnNames()
 {
  for (OutputTsvFile outputFile : OutputFile)outputFile.noteColumnNames(new String[]{"IntervalStart","IntervalEnd","DeviceName",
                                                                                     "CVWatts","MaxWatts","MeanWatts","MinWatts","N","NNonZero","OutputTime",
                                                                                     "Current","Voltage","Power","Total"});
 }

 private void outputStats(String current, String voltage, String power, String total)
 {
  Date now = new Date();
  if (now.compareTo(OutputCollectionInterval.getStop())>0)
     {
//      outputStats_(current,voltage,power,total,Count);  oops.
      outputStats_(LastResponse,Count);
      Stats.reset();
      Count = 0;
      OutputCollectionInterval = new CollectionInterval(CollectionIntervalMs);     // If outputStats took a long time the response could be included in an interval 
                                                                                   // that is too late for it. // That may be awkward, but may highlight the delay.
     }
  LastResponse = new Response(current,voltage,power,total);

  if (OutputFileIntervalMs > 0 &&
      now.compareTo(OutputFileCollectionInterval.getStop()) > 0)
     {
      closeOutputFiles();
      for (OutputTsvFile outputFile : OutputFile)Utilities.log(outputFile.stats());
      OutputTsvFileNamer.nameOutputTsvFiles(OutputFile,OutputFileCollectionInterval);
      OutputFileCollectionInterval = new CollectionInterval(OutputFileIntervalMs); // If outputStats took a really long time the response could be included in a file 
                                                                                   // that is too late for it. That would highlight the delay, which would have to be 
                                                                                   // extreme. e.g. Output files cut every minute with a 2 minute delay in writing, 
                                                                                   // closing, etc.
     }

  if (!Utilities.isFloat(power))
     {
      Utilities.error("CollectKasa.outputStats power is not numeric{"
                     +"power="+power+"}");
      return;
     }

  double doublePower = Utilities.parseDouble(power);
  if (doublePower != 0)Stats.noteValue(doublePower);
  Count ++;
 }

 private void outputStats_(Response lastResponse, long count)
 {
  if (count == 0)return;
  Date now = new Date();   // So every file gets the same thing.
  for (OutputTsvFile outputFile: OutputFile)
      {
       outputFile.put(new String[]{Utilities.toStringMillis(OutputCollectionInterval.getStart()),
                                   Utilities.toStringMillis(OutputCollectionInterval.getStop ()), 
                                   DeviceName                                                ,
                                   Utilities.toString      (Stats.getCV  ().getResult(count)),
                                   Utilities.toString      (Stats.getMax ().getResult(     )),
                                   Utilities.toString      (Stats.getMean().getResult(count)),
                                   Utilities.toString      (Stats.getMin ().getResult(     )),
                                   Utilities.toString      (count                           ),
                                   Utilities.toString      (Stats.getN   ().getResult(     )),
                                   Utilities.toStringMillis(now                             ),
                                   lastResponse.Current                                      ,
                                   lastResponse.Voltage                                      ,
                                   lastResponse.Power                                        ,
                                   lastResponse.Total                                        });
       outputFile.flush();
      }
 }

 public void run() 
 throws IOException, MyIOException, MyParseException
 {
  Date startTime = new Date();
  Utilities.log("CollectKasa{"
               + "Compile Time="          +CompileDate.getDate()                     +"\t"
               + "CollectionIntervalSecs="+Utilities.toString(CollectionIntervalSecs)+"\t"
               + "DurationSecs="          +Utilities.toString(DurationSecs          )+"\t"
               + "IP="                    +Utilities.toString(IP                    )+"\t"
               + "PollingIntervalSecs="   +Utilities.toString(PollingIntervalSecs   )+"}");

  IPInetAddress = Utilities.vetIP(IP);

  CollectionIntervalMs         = (long)(CollectionIntervalSecs * 1000);
  if (OutputFileRollIntervalSecs.toLowerCase().equals("noroll"))
      OutputFileIntervalMs = -1;
  else
     {
      if (!Utilities.isFloat(OutputFileRollIntervalSecs))Utilities.fatalError("CollectKasa.run invalid OutputFileRollIntevalSecs. Must be >= 0 or NoRoll{"
                                                                             +"OutputFileRollIntervalSecs="+OutputFileRollIntervalSecs+"}");
      double outputFileRollIntervalSecs = Utilities.parseDouble(OutputFileRollIntervalSecs);
      if (outputFileRollIntervalSecs < 0)Utilities.fatalError("CollectKasa.run invalid OutputFileRollIntevalSecs. Must be >= 0 or NoRoll{"
                                                             +"OutputFileRollIntervalSecs="+OutputFileRollIntervalSecs+"}");
      OutputFileIntervalMs = (long)(outputFileRollIntervalSecs * 1000);
     }

  OutputCollectionInterval     = new CollectionInterval(CollectionIntervalMs,false);  // force first record to boundaries.
  OutputFileCollectionInterval = new CollectionInterval(OutputFileIntervalMs,true );  // allow first file name to use current time.

  for (OutputTsvFile outputFile : OutputFile)outputFile.setLock(true);

  if (OutputFileIntervalMs >= 0) OutputTsvFileNamer.nameOutputTsvFiles(OutputFile, OutputFileCollectionInterval);
  Utilities.vetOutputTsvFiles(OutputFile);
  noteColumnNames();
  long       pollingIntervalMs = (long)(PollingIntervalSecs*1000.0);
  long       stopTime          = DurationSecs==0?0:startTime.getTime()+DurationSecs*1000;
  JsonParser jsonParser = new JsonParser();
  while (true)
        {
         Date       now          = new Date();
         long       targetTime   = ((now.getTime()+pollingIntervalMs)/pollingIntervalMs)*pollingIntervalMs;
         Utilities.sleepMs(targetTime-now.getTime());
         String     time         = Utilities.toStringMillis(new Date(targetTime));
         String     response     = null;
         int        retrySecs    = 1;
         retry:
         {
          for (int i=0;i<Retries;i++)
              {
               response = null;
               try {response = getResponse("{\"emeter\":{\"get_realtime\":{}}}");}
               catch (IOException e)
                     {
                      Utilities.error("CollectKasa.run getResponse failed{"
                                     +"e="+Utilities.toString(e)+"}");
                      try {setupSocket();}
                      catch (IOException e1){Utilities.error("CollectKasa.run IOException from setupSocket{"
                                                            +"e1="+Utilities.toString(e1)+"}");}
                      response = null;
                     }
               if (response == null)
                   {
                    Utilities.sleepMs(retrySecs * 1000);
                    retrySecs *= 2;
                    continue;
                   }
               if (i > 0) Utilities.error("CollectKasa.run note successful getResponse after error(s)");
               break retry;
              }
          Utilities.fatalError("CollectKasa.run retries exceeded");
         }
         if (response != null)
            {
             Date       OutputTime   = new Date();  // Arbitrarily assume response is for the time it arrived rather than was requested.
             String[][] results      = jsonParser.parse(response);
             String     current      = "";
             String     voltage      = "";
             String     power        = "";
             String     total        = "";
             for (String[] row : results)
                  if (row.length == 3)
                     {
                      if (Debug)Utilities.debug("CollectKasa.run note data row{"
                                                +"row="+Utilities.toString(row)+"}");
                      switch (row[1])
                             {
                              case "emeter:get_realtime:current"   :current = row[2];break;
                              case "emeter:get_realtime:current_ma":current = row[2];break;
                              case "emeter:get_realtime:power"     :power   = row[2];break;
                              case "emeter:get_realtime:power_mw"  :power   = row[2];break;
                              case "emeter:get_realtime:total"     :total   = row[2];break;
                              case "emeter:get_realtime:total_wh"  :total   = row[2];break;
                              case "emeter:get_realtime:voltage"   :voltage = row[2];break;
                              case "emeter:get_realtime:voltage_mv":voltage = row[2];break;
                            }
                     }
             outputStats(current,voltage,power,total);

             if (stopTime > 0 && targetTime >= stopTime)break;
            }
        }

  closeOutputFiles();

  Date endTime     = new Date();
  Date elapsedTime = new Date(endTime.getTime()-startTime.getTime());

  for (OutputTsvFile outputFile : OutputFile)Utilities.log(outputFile.stats());
  Utilities.log("CollectKasa.run{elapsedTime="+Utilities.toStringTime(elapsedTime)+"}");
 }

 private void setupSocket()
 throws IOException
 {
  if (Socket != null)
     {                // tear down current socket
      if (Debug)Utilities.debug("CollectKasa.setupSocket close Socket");
      try {
           Socket.shutdownOutput();
           Socket.shutdownInput();
           Socket.close();
          }
      catch (IOException e){Utilities.error("CollectKasa.setupSocket IOExceptoin{"
                                           +"e="+Utilities.toString(e)+"}");}
      Socket = null;
     }
    
  Socket = new Socket(IPInetAddress, 9999);
  if (!Socket.isBound())Socket.bind(null);
  OutputStream  = Socket.getOutputStream();
  InputStream   = Socket.getInputStream ();
 }
}
