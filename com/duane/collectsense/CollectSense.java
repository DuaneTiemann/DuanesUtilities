package com.duane.collectsense;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.String;
import java.lang.Thread;
import java.lang.Void;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import javax.net.ssl.HttpsURLConnection;
import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.DeploymentException;

import com.duane.utilities.CollectionInterval;
import com.duane.utilities.CompileDate;
import com.duane.utilities.CV;
import com.duane.utilities.Max;
import com.duane.utilities.Mean;
import com.duane.utilities.Min;
import com.duane.utilities.N;
import com.duane.utilities.Initializable;
import com.duane.utilities.JsonParser;
import com.duane.utilities.OutputTsvFileNamer;
import com.duane.utilities.MyIOException;
import com.duane.utilities.MyParseException;
import com.duane.utilities.OutputTsvFile;
import com.duane.utilities.ParmParser;
import com.duane.utilities.ParmParserParm;
import com.duane.utilities.Stats;
import com.duane.utilities.Utilities;
import com.duane.utilities.Variable;

@ParmParserParm("Invocation: java -cp javax.websocket-api-1.1.jar;tyrus-standalone-client-1.9.jar;CollectSense.jar com.duane.collectsense.CollectSense parameters\t"
               +"retrieves real time data from Sesne energy monitor site")
public class CollectSense implements Initializable
{
                                                                           private String                  AccessToken                 = null;
 @ParmParserParm("true|(false)"                                          ) private static boolean          Debug                       = false;
                                                                           private String[]                ColumnNames                 = new String[0];
 @ParmParserParm("Number of seconds to include for each data record (5)\t"
                +"0=> output every sample"                               ) private double                  CollectionIntervalSecs       = 5;
                                                                           private long                    CollectionIntervalMs        = 500;
                                                                           private long                    Count                       = 0;
 @ParmParserParm("(300)Number of seconds to collect data 0=>forever"     ) private long                    DurationSecs                = 300;
 @ParmParserParm("required for login"                                    ) private String                  Email                       = null;
 @ParmParserParm("(60) We should be getting traffic every .5s. But if a router fails, etc. the only symptom is no traffic forever.")
                                                                           private long                    IdleTimeoutSecs             = 60;
                                                                           private JsonParser              JsonParser                  = new JsonParser();
                                                                           private CollectionInterval      OutputCollectionInterval    = null;
                                                                           private CollectionInterval      OutputFileCollectionInterval= null;
                                                                           private long                    OutputFileIntervalMs        = 0;
 @ParmParserParm("Interval to switch to new OutputFile(s)(NoRoll)\t"
                +" 0     => persistent file with start time in file name\t"      
                +">0     => start and intended end in file name(s)(rolling)\t"
                +"NoRoll => persistent file with no timestamp\t" 
                +"If multiple OutputFiles are specified, they all respect this parameter")
                                                                           private String                  OutputFileRollIntervalSecs   = "noroll";
                                                                           private OutputTsvFileNamer      OutputTsvFileNamer           = new OutputTsvFileNamer();
 @ParmParserParm("(required) File(s) to receive output\t"
                +"file is locked per below\t"
                +"Columns: IntervalStart IntervalEnd DeviceName CVWatts MaxWatts MeanWatts MinWatts N NNonZero OutputTime\t"
                +"         following raw values are from the last sample in the collection interval:\t"
                +"         W Mature"                                     ) private OutputTsvFile[]         OutputFile                   = null;
 @ParmParserParm("required for login"                                    ) private String                  Password                     = null;
 @ParmParserParm("(5) Retries after 1s, 2s, 4s, ..."                     ) private int                     Retries                      = 5;
                                                                           private HashMap<String,MyStats> StatsHashMap                 = new HashMap<String,MyStats>();
                                                                           private Object                  This                         = this;
                                                                           private TimeZone                TimeZone                     = new GregorianCalendar().getTimeZone();
                                                                           private final static String     Version                      = "1.0.0";

 public class MyClientEndpointConfigurator extends ClientEndpointConfig.Configurator
 {
  @Override
  public void beforeRequest(Map<String,List<String>> map)
  {
   ArrayList<String> hostAL   = new ArrayList<String>(); hostAL  .add("clientrt.sense.com"    );map.put("Host"  ,hostAL  );
   ArrayList<String> originAL = new ArrayList<String>(); originAL.add("https://home.sense.com");map.put("Origin",originAL);
   if (Debug)Utilities.debug("beforeRequest{"
                            +"map="+Utilities.toString(map)+"}");
  }
 }

 public class MyStats extends Stats
 {
  private String Mature = null;
  private String Watts  = null;

  public String getMature(             ){return Mature;}
  public String getWatts (             ){return Watts ;}
  public void   setMature(String mature){Mature=mature;}
  public void   setWatts (String watts ){Watts =watts ;}
  public void   reset()
  {
   super.reset();
   Mature = null;
   Watts  = null;
  }
 }

 public class MyEndpoint extends Endpoint
 {
  @Override
  public void onClose(Session session, CloseReason closeReason)
  {
   if (Debug)Utilities.debug("MyEndpoint.onClose{"
                            +"closeReason="+closeReason+"}");
   synchronized(This)
   {
    try {This.notifyAll();}
    catch (Exception e){Utilities.error("onClose notify exception{"
                                       +"e="+Utilities.toString(e)+"}");  }
   }
  }

  @Override
  public void onError(Session session, Throwable e)
  {
   Utilities.error("MyEndpoint.onError{"
                  +"e="+Utilities.toString(e)+"}");
  }

  @Override
  public void onOpen(Session session, EndpointConfig config)
  {
   if (Debug)Utilities.debug("MyEndpoint.onOpen");
   session.addMessageHandler(new MessageHandler.Whole<String>() 
                             {
                              public void onMessage(String text) 
                              {
                               synchronized(This)
                               {
                                if (new Date().compareTo(OutputCollectionInterval.getStop()) >= 0)
                                   {
                                    outputStats();
                                    resetStats();
                                    Count              = 0;
                                    OutputCollectionInterval = new CollectionInterval(CollectionIntervalMs);
                                   }
                                scanData(text);
                               }
                              }
                             }
                            );
   Future<Void> future = session.getAsyncRemote().sendText("{\"type\":\"hello\"}");
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

 public CollectSense(){}

 public void finalize() throws Throwable {super.finalize();}

 private void getAccessToken()
 throws IOException, MalformedURLException, MyIOException, ProtocolException
 {
  URL                apiURL         = new URL("https://api.sense.com/apiservice/api/v1/authenticate");
  HttpsURLConnection apiConnection  = (HttpsURLConnection)apiURL.openConnection();
  apiConnection.setRequestMethod  ("POST");
  apiConnection.setRequestProperty("Host"            ,"api.sense.com"          );
  apiConnection.setRequestProperty("Referer"         ,"https://home.sense.com/");
  apiConnection.setRequestProperty("x-sense-protocol","3"                      );
  apiConnection.setRequestProperty("origin"          ,"https://home.sense.com" );
  apiConnection.setDoOutput(true);
  OutputStream       outputStream   = apiConnection.getOutputStream();
  String             request        = "email="+Email+"&password="+Password;
  outputStream.write(request.getBytes());
  outputStream.flush();
  outputStream.close();
  InputStream        inputStream    = apiConnection.getInputStream ();
  byte[]             responseBuffer = new byte[2048];
  byte[]             responseBytes  = new byte[0];
  for (int len = inputStream.read(responseBuffer);len !=-1;len = inputStream.read(responseBuffer))
       responseBytes = Utilities.concatenateArrays(responseBytes,responseBuffer,len);
  inputStream.close();
  String             response       = new String(responseBytes);
  String[][]         parsedResponse = null;
  try{parsedResponse = JsonParser.parse(response);}
  catch (MyParseException e){Utilities.error("processData json parse exception{"
                                            +"e="+Utilities.toString(e)+"}");}
  for (String[] nameValue: parsedResponse)
      if (nameValue.length == 3 &&
          nameValue[1].equals("access_token"))AccessToken = nameValue[2];

  if (AccessToken == null)Utilities.fatalError("processData unable to get access_token"
                                            +"parsedResponse="+Utilities.toString(parsedResponse)+"\t"
                                            +"response="      +response                          +"}");
 }

 public void initialize(){}

 public static void main(String[] argv)
 {
  Utilities.help(argv,CollectSense.class);
  Utilities.debug(false);

  try {((CollectSense)ParmParser.createObject(CollectSense.class, argv)).run();}
  catch (DeploymentException    e){Utilities.fatalError("CollectSense.main DeploymentException{e="   +Utilities.toString(e)+"}");}
  catch (IllegalAccessException e){Utilities.fatalError("CollectSense.main IllegalAccessException{e="+Utilities.toString(e)+"}");}
  catch (IllegalStateException  e){Utilities.fatalError("CollectSense.main IllegalStateException{e=" +Utilities.toString(e)+"}");}
  catch (IOException            e){Utilities.fatalError("CollectSense.main IOException{e="           +Utilities.toString(e)+"}");}
  catch (MyIOException          e){Utilities.fatalError("CollectSense.main MyIOException{e="         +Utilities.toString(e)+"}");}
  catch (MyParseException       e){Utilities.fatalError("CollectSense.main MyParseException{e="      +Utilities.toString(e)+"}");}
  catch (URISyntaxException     e){Utilities.fatalError("CollectSense.main URISyntaxException{e="    +Utilities.toString(e)+"}");}
  catch (Exception              e){Utilities.fatalError("CollectSense.main Exception{e="             +Utilities.toString(e)+"}");}
 }

 private void noteColumnNames()
 {
  ColumnNames = new String[]{"IntervalStart","IntervalEnd","DeviceName",
                             "CVWatts","MaxWatts","MeanWatts","MinWatts","N","NNonZero","OutputTime","W","Mature"};
  for (OutputTsvFile outputFile : OutputFile)outputFile.noteColumnNames(ColumnNames);
 }

 private boolean objectLevelsEqual(String o1, String o2, int levels)
 {
  if (o1 == null && o2 == null)return true;
  if (o1 == null && o2 != null)return false;
  if (o1 != null && o2 == null)return false;

  String[] o1Levels = o1.split(":",-1);
  String[] o2Levels = o2.split(":",-1);

  int oLevels = o1Levels.length <= o2Levels.length?o1Levels.length:o2Levels.length;
  int length  = oLevels         <= levels         ?oLevels        :levels;

  if (o1Levels.length < 3 && o2Levels.length >=3)return false;
  if (o2Levels.length < 3 && o1Levels.length >=3)return false;

//  if (Debug)Utilities.debug("CollectSense.objectLevelsEqual{"
//                           +"length="  +length                      +"\t"
//                           +"levels="  +levels                      +"\t"
//                           +"o1="      +o1                          +"\t"
//                           +"o1Levels="+Utilities.toString(o1Levels)+"\t"
//                           +"o2="      +o2                          +"\t"
//                           +"o2Levels="+Utilities.toString(o2Levels)+"}");
  for (int i = 0; i < length; i++) if (!o1Levels[i].equals(o2Levels[i])) return false;
//  if (Debug)Utilities.debug("CollectSense.objectLevelsEqual return true");
  return true;
 }

 private void outputStats()
 {
  if (Debug)Utilities.debug("CollectSense.outputStats");

  Date now = new Date();
  if (now.compareTo(OutputCollectionInterval.getStop())>0)
     {
      outputStats_(Count);
      resetStats();
      Count = 0;
      OutputCollectionInterval = new CollectionInterval(CollectionIntervalMs);  // If outputStats took a long time the response could be included in an interval 
                                                                                // that is too late for it. // That may be awkward, but may highlight the delay.
     }
  if (OutputFileIntervalMs > 0 &&
      now.compareTo(OutputFileCollectionInterval.getStop()) > 0)
     {
      closeOutputFiles();
      for (OutputTsvFile outputFile : OutputFile)Utilities.log(outputFile.stats());
      OutputTsvFileNamer.nameOutputTsvFiles(OutputFile,OutputFileCollectionInterval);
      OutputFileCollectionInterval = new CollectionInterval(OutputFileIntervalMs); // If outputStats took a really long time the response could be included in a file 
                                                                         // that is too late for it. That would highlight the delay, which would have to be 
                                                                         // extreme. e.g. Output files cut every minute with a 2 minute delay in writing, closing, etc.
     }
 }

 private void outputStats_(long count)
 {
  for (Map.Entry<String,MyStats> entry : StatsHashMap.entrySet())
      {
       String  key   = entry.getKey  ();
       MyStats stats = entry.getValue();
       long    n     = stats.getN    ().getResult();
       if (Debug)Utilities.debug("CollectSense.outputStats_ note stats{"
                                +"count="+count+"\t"
                                +"key="  +key  +"\t"
                                +"n="    +n    +"}");
       if (n == 0) continue;
       if (n < count)
          {
           stats.getMax().noteValue(0);   // note that observations could have all been negative so 0 would be max.
           stats.getMin().noteValue(0);
          }
       Date now = new Date();   // So every file gets the same thing.
       for (OutputTsvFile outputFile: OutputFile)
           {
            outputFile.put(new String[]{Utilities.toStringMillis(OutputCollectionInterval.getStart()),
                                        Utilities.toStringMillis(OutputCollectionInterval.getStop ()),
                                        key                                                       ,
                                        Utilities.toString      (stats.getCV  ().getResult(Count)),
                                        Utilities.toString      (stats.getMax ().getResult(     )),
                                        Utilities.toString      (stats.getMean().getResult(Count)),
                                        Utilities.toString      (stats.getMin ().getResult(     )),
                                        Utilities.toString      (count                           ),
                                        Utilities.toString      (stats.getN   ().getResult(     )),
                                        Utilities.toStringMillis(now                             ),  
                                        stats.getWatts ()                                         ,
                                        stats.getMature()                                         });
            outputFile.flush();
           }
       }
 }

 private void recordData(String key, double value, String watts, String mature)
 {
  MyStats stats = StatsHashMap.get(key);
  if (stats == null)
     {
      stats = new MyStats();
      StatsHashMap.put(key,stats);
     }
  stats.noteValue(value );
  stats.setWatts (watts );
  stats.setMature(mature);
 }

 private void resetStats()
 {
  for (Map.Entry<String,MyStats> entry : StatsHashMap.entrySet())
       entry.getValue().reset();
 }

 private void run() 
 throws DeploymentException, IOException, MyIOException, URISyntaxException
 {
  Date startTime = new Date();
  Utilities.log("CollectSense{"
               +"Compile Time="          +CompileDate.getDate() +"\t"
               +"CollectionIntervalSecs="+CollectionIntervalSecs+"\t"
               +"DurationSecs="          +DurationSecs          +"\t"
               +"Email="                 +Email                 +"}");

  if (Email    == null)Utilities.fatalError("run Email not provided"   );
  if (Password == null)Utilities.fatalError("run Password not provided");

  CollectionIntervalMs = (long)(CollectionIntervalSecs*1000);
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

  getAccessToken();

  ClientEndpointConfig.Configurator configurator  = new MyClientEndpointConfigurator();
  ClientEndpointConfig              configuration = ClientEndpointConfig.Builder.create().configurator(configurator).build();
  WebSocketContainer                container     = ContainerProvider.getWebSocketContainer();
  MyEndpoint                        endpoint      = new MyEndpoint();
  long                              now           = new Date().getTime();
  Session                           session       = null;
  long                              stopTime      = DurationSecs==0?0:now+DurationSecs*1000;
                                    This          = this;

  OutputCollectionInterval                        = new CollectionInterval(CollectionIntervalMs);
  OutputFileCollectionInterval                    = new CollectionInterval(OutputFileIntervalMs,true);
  for (OutputTsvFile outputFile : OutputFile)outputFile.setLock(true);
  if (OutputFileIntervalMs >= 0)OutputTsvFileNamer.nameOutputTsvFiles(OutputFile,OutputFileCollectionInterval);
  Utilities.vetOutputTsvFiles(OutputFile);
  noteColumnNames();

  synchronized(this)
  {
   while (true)
   {
    long waitTime = 0;
    if (stopTime > 0)
       {
        waitTime = stopTime - new Date().getTime();
        if (waitTime <= 0)break;
       }

    String urlString="wss://clientrt.sense.com/monitors/29603/realtimefeed?access_token="+AccessToken;
    int retrySecs = 1;
    retries:
    {
     for (int i=0;i<Retries;i++)
         {
          if (Debug)Utilities.debug("CollectSense.run connecting to server");
          session = null;
          try {session = container.connectToServer(endpoint, configuration, URI.create(urlString));}
          catch (ConnectException    e){Utilities.error("CollectSense.run connect DeploymentException{"
                                                       +"e=" +Utilities.toString(e)+"}");}
          catch (DeploymentException e){Utilities.error("CollectSense.run connect DeploymentException{"
                                                       +"e=" +Utilities.toString(e)+"}");}

          if (session == null)
             {
              Utilities.sleepMs(retrySecs*1000);
              retrySecs *= 2;
              continue;
             }

          if (i>0)Utilities.error("CollectSense.run note successful getResponse after error(s)");
          break retries;
         }
     Utilities.fatalError("CollectSense.run connect exceeded retries");
    }

    if (session != null)session.setMaxIdleTimeout(IdleTimeoutSecs*1000); 
    if (Debug) Utilities.debug("run connect{"
                              +"session=" +Utilities.toString(session)+"\t"
                              +"waitTime="+waitTime                   +"}");
    try {
         if (stopTime == 0)wait(        );
         else              wait(waitTime);
        }
    catch (InterruptedException e){Utilities.error("run InterruptedException{"
                                                  +"e="+Utilities.toString(e)+"}");}
    session.close();
    if (Debug)Utilities.debug("run afterWait");
   }
  }

  outputStats     ();
  closeOutputFiles();

  Date endTime     = new Date();
  Date elapsedTime = new Date(endTime.getTime()-startTime.getTime());

  for (OutputTsvFile outputFile : OutputFile)Utilities.log(outputFile.stats());
  Utilities.log("CollectSense.run{elapsedTime="+Utilities.toStringTime(elapsedTime)+"}");
 }

 private void scanData(String text)
 {
  String     id           = null;
  String     lagObjectId  = "";
  String     matureString = null;
  String     name         = null;
  String     objectId     = "";
  String[][] results      = null;
  String     wattsString  = null;

  if (Debug)Utilities.debug("CollectSense.scanData enter");
  try
      {
      results = JsonParser.parse(text);
      }
  catch (MyIOException    e){Utilities.error("onMessage exception{"
                                            +"e="   +Utilities.toString(e)+"\t"
                                            +"text="+text                 +"}");}
  catch (MyParseException e){Utilities.error("onMessage exception{"
                                            +"e="   +Utilities.toString(e)+"\t"
                                            +"text="+text                 +"}");}
  if (Debug && !text.startsWith("{\"type\":\"realtime_update\",\"payload\":"))
      Utilities.debug("MyEndpoint.onMessage note text{"
                     +"text="  +text                                      +"\t"
                     +"Thread="+Utilities.toString(Thread.currentThread())+"}");
  if (Debug)Utilities.debug("CollectSense.MyEndpoint.onMessage note message{"
                           +"text="   +text                       +"}");
  if (results == null)
     {
      if (Debug)Utilities.debug("CollectSense.scanData null return");
      return;
     }
  boolean foundResult = false;
  if (Debug)Utilities.debug("CollectSense.scanData note results length{"
                           +"results.length="+results.length+"}");
  for (String[] row: results)
      {
       objectId = row[0];
       if (row.length == 3)
          {
           if (!objectLevelsEqual(lagObjectId,objectId,3))
              {
               if (id           != null &&
                   name         != null &&
                   wattsString  != null)
                  {
                   foundResult = true;
                   if (Debug)Utilities.debug("CollectSense.scanData call recordData{"
                                            +"id="          +id          +"\t"
                                            +"matureString="+matureString+"\t"
                                            +"name="        +name        +"\t"
                                            +"wattsString=" +wattsString +"}");
                   recordData(name+"_"+id,Utilities.parseDouble(wattsString),wattsString,matureString);
                  }
               id           = null;                                                                                                  
               name         = null;                                                                                                  
               matureString = null;
               wattsString  = null;
              }
           if (row[1].equals("payload:devices:id"         )){id           = row[2]; if (Debug)Utilities.debug("CollectSense.ScanData{objectId="+objectId+" id="          +id          +"}");}
           if (row[1].equals("payload:devices:name"       )){name         = row[2]; if (Debug)Utilities.debug("CollectSense.ScanData{objectId="+objectId+" name="        +name        +"}");}
           if (row[1].equals("payload:devices:tags:Mature")){matureString = row[2]; if (Debug)Utilities.debug("CollectSense.ScanData{objectId="+objectId+" matureString="+matureString+"}");}
           if (row[1].equals("payload:devices:w"          )){wattsString  = row[2]; if (Debug)Utilities.debug("CollectSense.ScanData{objectId="+objectId+" wattsString=" +wattsString +"}");}
           if (row[1].equals("payload:w"))
              {
               if (Debug)Utilities.debug("CollectSense.ScanData{objectId="+objectId+" w="+row[2]+"}");
               foundResult = true;
               recordData("TotalUsage_none",Utilities.parseDouble(row[2]),row[2],matureString);
              }
          }
       lagObjectId = objectId;
      }
  if (foundResult)Count++;
  if (Debug)Utilities.debug("CollectSense.scanData return");
 }
}

