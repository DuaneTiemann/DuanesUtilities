package com.duane.collectwemo;

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
import com.duane.utilities.Initializable;
import com.duane.utilities.MyIOException;
import com.duane.utilities.MyParseException;
import com.duane.utilities.OutputTsvFile;
import com.duane.utilities.OutputTsvFileNamer;
import com.duane.utilities.ParmParser;
import com.duane.utilities.ParmParserParm;
import com.duane.utilities.Stats;
import com.duane.utilities.Utilities;

@ParmParserParm("Invocation: java -jar CollectWemo.jar parmaeters\t"
               +"retrieves real time data from Wemo smart plug")
public class CollectWemo implements Initializable
{
 @ParmParserParm("true|(false)"                                         ) private static boolean      Debug                        = false;
 @ParmParserParm("Number of seconds to include for each data record (5)\t"
                +"0=> output every sample"                              ) private double              CollectionIntervalSecs       = 5;
                                                                          private long                CollectionIntervalMs         = 1000;
                                                                          private long                Count                        = 0;
 @ParmParserParm("Name of monitoring target (Wemo)"                     ) private String              DeviceName                   = "Kasa";
 @ParmParserParm("(300)Number of seconds to collect data 0=>forever"    ) private long                DurationSecs                 = 300;
                                                                          private InetAddress         IPInetAddress                = null;
 @ParmParserParm("(required) Wemo IP Address"                           ) private String              IP                           = null;
                                                                          private CollectionInterval  OutputCollectionInterval     = null;
                                                                          private Response            LastResponse                 = null;
 @ParmParserParm("(required) File(s) to receive output\t"
                +"file is locked per below\t"
                +"Columns: IntervalStart IntervalEnd DeviceName CVWatts MaxWatts MeanWatts MinWatts N NNonZero OutputTime\t"
                +"         following values from the last sample in the collection interval:\t"
                +"         State Lastchange Onfor Ontoday Ontotal Period _X Current Todaymw Totalmw")
                                                                          private OutputTsvFile[]     OutputFile                   = new OutputTsvFile[0];
                                                                          private CollectionInterval  OutputFileCollectionInterval = null;
                                                                          private long                OutputFileIntervalMs         = 0;   
 @ParmParserParm("Interval to switch to new OutputFile(s)(NoRoll)\t"
                +" 0     => persistent file with start time in file name\t"      
                +">0     => start and intended end in file name(s)(rolling)\t"
                +"NoRoll => persistent file with no timestamp\t" 
                +"If multiple OutputFiles are specified, they all respect this parameter")
                                                                          private String              OutputFileRollIntervalSecs   = "noroll";
                                                                          private OutputTsvFileNamer  OutputTsvFileNamer           = new OutputTsvFileNamer();
 @ParmParserParm("(1)"                                                  ) private double              PollingIntervalSecs          = 1;
 @ParmParserParm("(required) Wemo Port"                                 ) private Integer             Port                         = null;
 @ParmParserParm("(5) Retries after 1s, 2s, 4s, ..."                    ) private int                 Retries                      = 5;
                                                                          private Stats               Stats                        = new Stats();
                                                                          private final static String Version                      = "1.0.0";
 
 private class Response
 {
  public String State     ;
  public String Lastchange;
  public String Onfor     ;
  public String Ontoday   ;
  public String Ontotal   ;
  public String Period    ;
  public String _X        ;
  public String Current   ;
  public String Todaymw   ;
  public String Totalmw   ;
  public Response(String state     ,
                  String lastchange,
                  String onfor     ,
                  String ontoday   ,
                  String ontotal   ,
                  String period    ,
                  String _x        ,
                  String current   ,
                  String todaymw   ,
                  String totalmw   )
  {
   State     =state     ;
   Lastchange=lastchange;
   Onfor     =onfor     ;
   Ontoday   =ontoday   ;
   Ontotal   =ontotal   ;
   Period    =period    ;
   _X        =_x        ;
   Current   =current   ;
   Todaymw   =todaymw   ;
   Totalmw   =totalmw   ;
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

 public CollectWemo(){super();}

 public void finalize() throws Throwable {super.finalize();}

 public void initialize(){}

 private String getResponse(String request)
 throws IOException
 {
  Socket socket = new Socket(IPInetAddress,Port);
  if (!socket.isBound())socket.bind(null);
  OutputStream outputStream     = socket.getOutputStream();
  InputStream  inputStream      = socket.getInputStream ();
  byte[]       buffer           = new byte[2000];
  byte[]       responseBytes    = new byte[0];
  byte[]       requestBytes     = request.getBytes();

  outputStream.write(requestBytes);
  for (int len = inputStream.read(buffer);len!=-1;len=inputStream.read(buffer))
       responseBytes=Utilities.concatenateArrays(responseBytes,buffer,len);
  socket.close();
  String       result           = new String(responseBytes);

  if (Debug)Utilities.debug("CollectWemo.getResponse{"
                           +"IPInetAddress="   +Utilities.toString  (IPInetAddress   )+"\t"
                           +"Port="            +Port                                  +"\t"
                           +"request="         +request                               +"\t"
                           +"result="          +result                                +"}");
  return result;
 }

 public static void main(String[] argv)
 {
  Utilities.help(argv,CollectWemo.class);
  Utilities.debug(false);

  try {((CollectWemo)ParmParser.createObject(CollectWemo.class, argv)).run();}
  catch (IllegalAccessException e){Utilities.fatalError(Utilities.toString(e));}
  catch (IOException            e){Utilities.fatalError(Utilities.toString(e));}
  catch (MyIOException          e){Utilities.fatalError(Utilities.toString(e));}
  catch (MyParseException       e){Utilities.fatalError(Utilities.toString(e));}
 }

 private void noteColumnNames()
 {
  for (OutputTsvFile outputFile : OutputFile)outputFile.noteColumnNames(new String[]{"IntervalStart","IntervalEnd","DeviceName",
                                                                                     "CVWatts","MaxWatts","MeanWatts","MinWatts","N","NNonZero","OutputTime",
                                                                                     "State", "Lastchange", "Onfor", "Ontoday", "Ontotal", "Period",
                                                                                     "_X","Current","Todaymw","Totalmw"});
 }

 private void outputStats(Response response)
 {
  Date now = new Date();
  if (now.compareTo(OutputCollectionInterval.getStop())>0)
     {
      outputStats_(LastResponse,Count);
      Stats.reset();
      Count = 0;
      OutputCollectionInterval = new CollectionInterval(CollectionIntervalMs);     // If outputStats took a long time the response could be included in an interval 
                                                                                   // that is too late for it. // That may be awkward, but may highlight the delay.
     }
  LastResponse = response;
  if (OutputFileIntervalMs > 0 &&
      now.compareTo(OutputFileCollectionInterval.getStop()) > 0)
     {
      closeOutputFiles();
      for (OutputTsvFile outputFile : OutputFile)Utilities.log(outputFile.stats());
      OutputFileCollectionInterval = new CollectionInterval(OutputFileIntervalMs); // If outputStats took a really long time the response could be included in a file 
                                                                                   // that is too late for it. That would highlight the delay, which would have to be 
                                                                                   // extreme. e.g. Output files cut every minute with a 2 minute delay in writing, 
                                                                                   // closing, etc.
      OutputTsvFileNamer.nameOutputTsvFiles(OutputFile,OutputFileCollectionInterval);
     }

  if (!Utilities.isFloat(response.Current))
     {
      Utilities.error("CollectWemo.run current is not numeric{"
                     +"current="+response.Current+"}");
      return;
     }

  double doubleCurrent = Utilities.parseDouble(response.Current)/1000;
  if (doubleCurrent != 0)Stats.noteValue(doubleCurrent);
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
                                   DeviceName                                                   ,
                                   Utilities.toString      (Stats.getCV  ().getResult(count))   ,
                                   Utilities.toString      (Stats.getMax ().getResult(     ))   ,
                                   Utilities.toString      (Stats.getMean().getResult(count))   ,
                                   Utilities.toString      (Stats.getMin ().getResult(     ))   ,
                                   Utilities.toString      (count                           )   ,
                                   Utilities.toString      (Stats.getN   ().getResult(     ))   ,
                                   Utilities.toStringMillis(now                             )   ,
                                   lastResponse.State                                           ,
                                   lastResponse.Lastchange                                      ,
                                   lastResponse.Onfor                                           ,
                                   lastResponse.Ontoday                                         ,
                                   lastResponse.Ontotal                                         ,
                                   lastResponse.Period                                          ,
                                   lastResponse._X                                              ,
                                   lastResponse.Current                                         ,
                                   lastResponse.Todaymw                                         ,
                                   lastResponse.Totalmw                                         });
       outputFile.flush();
      }
 }

 private Response parseResponse(String response)
 {
  int pos1 = response.indexOf("<InsightParams>" );
  int pos2 = response.indexOf("</InsightParams>");
  if (pos1 == -1 ||
      pos2 == -1 ||
      pos1 >= pos2)
     {
      Utilities.error("CollectWemo.parseResponse invalid response{"
                     +"response="+response+"}");
      return null;
     }
  String   responseData = response.substring(pos1+"<InsightParams>".length(),pos2-"</InsightParams>".length());
  String[] strings      = Utilities.split(responseData,"|");
  if (strings.length != 10)
      {
       Utilities.error("CollectWemo.parseResponse unexpected response data{"
                      +"responseData="+responseData+"}");
       return null;
      }
  return new Response(strings[0],strings[1],strings[2],strings[3],strings[4],strings[5],strings[6],strings[7],strings[8],strings[9]);
 }

 private void run()
 throws IOException, MyIOException, MyParseException
 {
  Date startTime = new Date();
  Utilities.log("CollectWemo{"
               +"Compile Time="          +CompileDate.getDate()                     +"\t"
               +"CollectionIntervalSecs="+Utilities.toString(CollectionIntervalSecs)+"\t"
               +"DurationSecs="          +DurationSecs                              +"\t"
               +"IP="                    +Utilities.toString(IP                    )+"\t"
               +"Port="                  +Port                                      +"}");
  IPInetAddress = Utilities.vetIP  (IP  );
  Utilities.vetPort(Port);

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
  if (OutputFileIntervalMs >= 0)OutputTsvFileNamer.nameOutputTsvFiles(OutputFile,OutputFileCollectionInterval);

  Utilities.vetOutputTsvFiles(OutputFile);
  noteColumnNames();
  long       pollingIntervalMs = (long)(PollingIntervalSecs*1000.0);
  long       stopTime          = DurationSecs==0?0:startTime.getTime()+DurationSecs*1000;
  while (true)
        {
         Date       now          = new Date();
         long       targetTime   = ((now.getTime()+pollingIntervalMs)/pollingIntervalMs)*pollingIntervalMs;
         Utilities.sleepMs(targetTime-now.getTime());
         String     time         = Utilities.toStringMillis(new Date(targetTime));
         String     content      = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                  +"<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                                  +"   <s:Body>\n"
                                  +"      <u:GetInsightParams xmlns:u=\"urn:Belkin:service:insight:1\"></u:GetInsightParams>\n"
                                  +"   </s:Body>\n"
                                  +"</s:Envelope>\n";

         String     response     = null;
         int        retrySecs    = 1;
         retry:
         {
          for (int i=0;i<Retries;i++)
              {
               try {response = getResponse("POST /upnp/control/insight1 HTTP/1.0\r\n"
                                          +"Content-Type: text/xml; charset=\"utf-8\"\r\n"
                                          +"HOST: "+IP+"\r\n"
                                          +"Content-Length: "+content.length()+"\r\n"
                                          +"SOAPACTION: \"urn:Belkin:service:insight:1#GetInsightParams\"\r\n"
                                          +"Connection: close\r\n\r\n"
                                          +content);}
               catch (IOException e)
                     {
                      Utilities.error("CollectWemo.run getResponse failed{"
                                     +"e="+Utilities.toString(e)+"}");
                      Utilities.sleepMs(retrySecs*1000);
                      retrySecs *= 2;
                      continue;
                     }
               if (i>0)Utilities.error("CollectWemo.run note successful getResponse after error(s)");
               break retry;
              }
          Utilities.fatalError("CollectWemo.run retries exceeded");
         }

         Response results = parseResponse(response);
         if (results != null)
             outputStats(results);
         if (stopTime > 0 && targetTime >= stopTime)break;
        }

  for (OutputTsvFile outputFile : OutputFile)outputFile.close();

  Date endTime     = new Date();
  Date elapsedTime = new Date(endTime.getTime()-startTime.getTime());

  for (OutputTsvFile outputFile : OutputFile)Utilities.log(outputFile.stats());
  Utilities.log("CollectWemo.run{elapsedTime="+Utilities.toStringTime(elapsedTime)+"}");
 }
}

