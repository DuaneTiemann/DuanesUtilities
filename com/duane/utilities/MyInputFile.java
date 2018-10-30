package com.duane.utilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.xml.ws.Holder;

import com.duane.utilities.ParmParserParm;

/*
  To Do:
  Support File rolling.
  Support Tailing.
  Support Wait for file to appear.
*/

/*
  Current line number (line just read) is supported on a best effort basis, without rereading after seeks.
*/

@ParmParserParm("FileName")
public class MyInputFile implements Initializable
{
                                                       private byte[]             Buffer                = new byte[8192];
                                                       private int                BufferEndOffset       = 0;
                                                       private int                BufferExpansionSize   = 8192;
                                                       private int                BufferStartOffset     = 0;
                                                       private long               CurrentFileOffset     = 0;
                                                       private long               CurrentLineNumber     = 0;
                                                       private boolean            Debug                 = false; 
 @ParmParserParm("(true)|false controls deletion of initial Unicode BOM (Byte Order Mark) in file")
                                                       private boolean            DeleteBOM             = true;
                                                       private RandomAccessFile   File                  = null;
 @ParmParserParm("Name of input file")                 private String             FileName              = null;
                                                       private boolean            Initialized           = false;
                                                       private String[]           LineHistory           = null;
                                                       private Holder<Integer>    LineSegmentBegin1     = new Holder<Integer>(-1);
                                                       private Holder<Integer>    LineSegmentEnd1       = new Holder<Integer>(-1);
                                                       private Holder<Integer>    LineSegmentBegin2     = new Holder<Integer>(-1);
                                                       private Holder<Integer>    LineSegmentEnd2       = new Holder<Integer>(-1);
 @ParmParserParm("number of prior lines to keep for reporting problem context")
                                                       private int                LineHistoryLimit      = 10;
 @ParmParserParm                                       private int                MaxBufferSize         = 32*1024*1024;
                                                       private int                NextLineHistoryIndex  = 0;
 @ParmParserParm("tail the file")                      private boolean            Tail                  = false;
 @ParmParserParm                                       private long               TailSleepMs           = 5000;
 @ParmParserParm("beginning|end default=beginning")    private String             TailStart             = "beginning";
                                                       private HashMap<Long,Long> TellLineNumbers       = new HashMap<Long,Long>();
 @ParmParserParm("true|false default=Tail")            private Boolean            WaitForFileToAppear   = null;

 public void close()
 {
  initialize_();
  try {
       if (File != null)File.close();
       File=null;
      }
  catch (IOException e)
        {
         Utilities.fatalError("MyInputFile.close IOException{"
                             +"FileName=" +FileName              +"\t"
                             +"exception="+Utilities.toString(e) +"\t"
                             +"stack="    +Utilities.nest()      +"}");
        }
 }

 private void copyBytes(byte[] target, int start, int end)
 {
  if (start == 0 && end == 0)return;

  if (start < end)
     {
      for (int pos = start; pos < end; pos++)target[pos - start] = Buffer[pos];
      return;
     }

  for (int pos=start; pos<Buffer.length; pos++)target[pos - start                ]=Buffer[pos];
  for (int pos=0    ; pos<end          ; pos++)target[Buffer.length - start + pos]=Buffer[pos];
 }

 private void expandBuffer()
 {
  int newBufferLength = Buffer.length+BufferExpansionSize;
  if (newBufferLength > MaxBufferSize)
      Utilities.fatalError("MyInputFile.expandBuffer buffer is too large{"
                         +"BufferExpansionSize="+BufferExpansionSize+"\t"
                         +"MaxBufferSize="      +MaxBufferSize      +"\t"
                         +"newBufferLength="    +newBufferLength    +"}");
  byte[] newBuffer = new byte[newBufferLength];
  int bytes = 0;
  if (BufferStartOffset == 0 && BufferEndOffset == 0)   // shouldn't be possible
      bytes = 0;
  else
  if (BufferStartOffset < BufferEndOffset)              // buffer is full
     {
      bytes = BufferEndOffset - BufferStartOffset;
      for (int pos = BufferStartOffset; pos < BufferEndOffset; pos++) newBuffer[pos - BufferStartOffset] = Buffer[pos];
     }
  else
     {   // BufferEndOffset <= BufferStartOffset
      bytes = Buffer.length - BufferStartOffset + BufferEndOffset;
      for (int pos = BufferStartOffset; pos < Buffer.length  ; pos++)newBuffer[pos-BufferStartOffset              ]=Buffer[pos];
      for (int pos = 0                ; pos < BufferEndOffset; pos++)newBuffer[pos+Buffer.length-BufferStartOffset]=Buffer[pos];
     }
  BufferStartOffset = 0;
  BufferEndOffset   = bytes;
  Buffer            = newBuffer;
  if (Debug)Utilities.debug("MyInputFile.expandBuffer done{"
                           +"BufferStartOffset="+BufferStartOffset+"\t"
                           +"BufferEndOffset="  +BufferEndOffset  +"}");
 }

 private String extractLineSegments()
 {
  String result = extractLineSegments_();
  resetLineSegments();
  return result;
 }

 private String extractLineSegments_()
 {
  try {
       int end = 0;
       if (LineSegmentBegin2.value == -1)
          {
           setBufferStartOffset(LineSegmentEnd1.value);
           if (LineSegmentEnd1.value > 0)end = Buffer[LineSegmentEnd1.value-1] == 0x0a?LineSegmentEnd1.value-1:LineSegmentEnd1.value;
           return new String(Buffer, LineSegmentBegin1.value, end - LineSegmentBegin1.value, "UTF-8").replace("\r", "");
          }
    
       setBufferStartOffset(LineSegmentEnd2.value);
       if (LineSegmentEnd2.value > 0)end = Buffer[LineSegmentEnd2.value-1] == 0x0a?LineSegmentEnd2.value-1:LineSegmentEnd2.value;
       byte[] contiguousBytes = new byte[LineSegmentEnd1.value-LineSegmentBegin1.value + end-LineSegmentBegin2.value];
       copyBytes(contiguousBytes,LineSegmentBegin1.value,end);  //copyBytes knows to wrap
       return new String(contiguousBytes,"UTF-8").replace("\r","");
      }
  catch (UnsupportedEncodingException e){Utilities.fatalError("MyInputFile.extractLineSegments_ UnsupportedEncodingException"
                                                             +"e="+e+"}");}
  return null; // unreachable.
 }

 private boolean fillBuffer()  // True return is success. False is EOF.
 {
  long totalBytesRead = 0;
  while (true)
        {
         int end   = 0;
         int start = 0;
         
         if (BufferStartOffset == 0 && BufferEndOffset == 0)
            {
             if (Debug)Utilities.debug("MyInputFile.fillBuffer trace 1");
             start = 0;
             end   = Buffer.length;
            }
         else
         if (BufferEndOffset < BufferStartOffset)
            {
             if (Debug)Utilities.debug("MyInputFile.fillBuffer trace 2");
             start = BufferEndOffset;
             end   = BufferStartOffset;
            }
         else
         if (BufferStartOffset < BufferEndOffset)
            {
             if (Debug)Utilities.debug("MyInputFile.fillBuffer trace 3{"
                                      +"BufferEndOffset="  +BufferEndOffset  +"\t"
                                      +"BufferStartOffset="+BufferStartOffset+"}");
             if (BufferEndOffset == Buffer.length)
                {
                 if (Debug)Utilities.debug("MyInputFile.fillBuffer trace 3.5{"
                                          +"BufferEndOffset="  +BufferEndOffset  +"\t"
                                          +"BufferStartOffset="+BufferStartOffset+"}");
                 if (BufferStartOffset == 0)return true;  // it is currently full.  Should be impossible
                 BufferEndOffset = 0;
                 start           = 0;
                 end             = BufferStartOffset;
                }
             else
                {
                 if (Debug)Utilities.debug("MyInputFile.fillBuffer trace 4{"
                                          +"BufferEndOffset="  +BufferEndOffset  +"\t"
                                          +"BufferStartOffset="+BufferStartOffset+"}");
                 // This could happen if buffer wasn't completely filled by last read or we just expanded the buffer.
                 // Just do to end of buffer this time round.
                 start = BufferEndOffset;
                 end   = Buffer.length;
                }
            }
         else
            return true; // BufferStartOffset == BufferEndOffset so it is already full 
         
         int nBytesRead = readBytes(start,end);
         if (Debug)Utilities.debug("MyInputFile.fillBuffer trace 5{"
                                  +"Buffer="           +Utilities.toString(Buffer)+"\t"
                                  +"BufferEndOffset="  +BufferEndOffset           +"\t"
                                  +"BufferStartOffset="+BufferStartOffset         +"\t"
                                  +"end="              +end                       +"\t"
                                  +"nBytesRead="       +nBytesRead                +"\t"
                                  +"start="            +start                     +"}");
         if (nBytesRead == -1)return totalBytesRead==0?false:true;
         totalBytesRead  += nBytesRead;
         BufferEndOffset += nBytesRead;
         if (BufferStartOffset == 0 && BufferEndOffset == 0            )continue;    // still empty must have read 0 bytes.
         if (BufferStartOffset == BufferEndOffset                      )return true; // It is full 
         if (BufferStartOffset == 0 && BufferEndOffset == Buffer.length)return true; // It is full
        }
 }

 public void finalize()
 {
  initialize_();
  close();
  File    =null;
  FileName=null;
 }

 private int findEOL(int start, int end)
 {
  int result = findEOL_(start,end);
  if (Debug)Utilities.debug("MyInputFile.findEOL{"
                           +"end="   +end   +"\t"
                           +"result="+result+"\t"
                           +"start=" +start +"}");
  return result;
 }

 private int findEOL_(int start, int end)
 {
  for (int pos=start;pos<end;pos++)if (Buffer[pos] == 0x0a)return pos;
  return -1;
 }

 private boolean findNextLineSegments()
 {
  boolean result=findNextLineSegments_();
  if (Debug)Utilities.debug("MyInputFile.findNextLineSegments{"
                           +"BufferStartOffset="       +BufferStartOffset        +"\t"
                           +"BufferEndOffset="         +BufferEndOffset          +"\t"
                           +"LineSegmentBegin1.value=" +LineSegmentBegin1.value  +"\t"
                           +"LineSegmentEnd1.value="   +LineSegmentEnd1  .value  +"\t"
                           +"LineSegmentBegin2.value=" +LineSegmentBegin2.value  +"\t"
                           +"LineSegmentEnd2.value="   +LineSegmentEnd2  .value  +"\t"
                           +"result="                  +result                   +"}");
  return result;
 }

 private boolean findNextLineSegments_()
 {
  resetLineSegments();

  while (true)
        {
         if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ trace 1");
         if (!(BufferStartOffset == 0 && BufferEndOffset == 0))
             { // if not empty
              if (Debug) Utilities.debug("MyInputFile.findNextLineSegments_ trace 1.5");
              int end = BufferStartOffset < BufferEndOffset ? BufferEndOffset : Buffer.length;
              int pos = findEOL(BufferStartOffset,end);
              if (pos != -1)
                 {
                  LineSegmentBegin1.value = BufferStartOffset;
                  LineSegmentEnd1.value   = pos+1;
                  if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ found EOL 1{"
                                           +"LineSegmentBegin1.value=" +LineSegmentBegin1.value  +"\t"
                                           +"LineSegmentEnd1.value="   +LineSegmentEnd1  .value  +"\t"
                                           +"LineSegmentBegin2.value=" +LineSegmentBegin2.value  +"\t"
                                           +"LineSegmentEnd2.value="   +LineSegmentEnd2  .value  +"}");
                  return true;
                 }
              
              if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ trace 2");
              if (BufferStartOffset >= BufferEndOffset) // wrapped or empty
                 {
                  pos = findEOL(0,BufferEndOffset);
                  if (pos != -1)
                     {
                      LineSegmentBegin1.value = BufferStartOffset;
                      LineSegmentEnd1  .value = Buffer.length;
                      LineSegmentBegin2.value = 0;
                      LineSegmentEnd2  .value = pos+1;
                      if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ found EOL 2{"
                                               +"LineSegmentBegin1.value=" +LineSegmentBegin1.value  +"\t"
                                               +"LineSegmentEnd1.value="   +LineSegmentEnd1  .value  +"\t"
                                               +"LineSegmentBegin2.value=" +LineSegmentBegin2.value  +"\t"
                                               +"LineSegmentEnd2.value="   +LineSegmentEnd2  .value  +"}");
                      return true;
                     }
                 }
              if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ trace 3");
              if ((BufferStartOffset == BufferEndOffset &&
                 !(BufferStartOffset == 0 && BufferEndOffset == 0)) ||  // check to be sure not empty buffer
                  BufferStartOffset == 0 && BufferEndOffset == Buffer.length)expandBuffer();
              if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ trace 3.5");
             }
         if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ trace 4");
         if (!fillBuffer())
            {
             if (Tail)return false; // if Tail then we are in middle of a line.  Have to wait for the rest of it.
             // if !Tail then any remaining data is the last line.
             if (BufferStartOffset == 0 && BufferEndOffset == 0)return false;
             if (BufferStartOffset < BufferEndOffset)
                {
                 LineSegmentBegin1.value = BufferStartOffset;
                 LineSegmentEnd1  .value = BufferEndOffset;
                 return true;
                }

             LineSegmentBegin1.value = BufferStartOffset;
             LineSegmentEnd1  .value = Buffer.length    ;
             LineSegmentBegin2.value = 0                ;
             LineSegmentEnd2  .value = BufferEndOffset  ;
             return true;
            }
         if (Debug)Utilities.debug("MyInputFile.findNextLineSegments_ trace 5");
        }
 }

 public long   getCurrentLineNumber(){return CurrentLineNumber;}
 public String getFileName         (){return FileName         ;}

 public String[] getLineHistory()
 {
  if (LineHistory == null)return null;
  String[] result = null;

  if (LineHistory[NextLineHistoryIndex] == null)
     {
      result = new String[NextLineHistoryIndex];
      for (int i = 0;i<NextLineHistoryIndex;i++)result[i] = LineHistory[i];
     }
  else
     {
      result = new String[LineHistory.length];
          
      for (int i = 0, index=NextLineHistoryIndex;i<LineHistory.length;i++,index++)
          {
           if (index >= LineHistory.length)index = 0;
           result[i] = LineHistory[index];
          }
     }
  return result;
 }

 public int getLineHistoryLimit()
 {
  return LineHistoryLimit;
 }

 public boolean getTail(){return Tail;}

 // We don't want internal calls to initialize to call subclass's initialize()

 public void initialize(){initialize_();}
 private void initialize_()
 {
  if (Initialized)return;
  Initialized = true;
  if (WaitForFileToAppear == null)WaitForFileToAppear = Tail;
  if (FileName == null)
      Utilities.fatalError("MyInputFile.initialize filename is null{"
                        +"stack="+Utilities.nest()+"}");
  try {File = new RandomAccessFile(FileName,"r");}
  catch (FileNotFoundException e)
        {
         Utilities.fatalError("MyInputFile.MyInputFile FileNotFoundException{"
                             +"exception="+Utilities.toString(e) +"\t"
                             +"file="     +FileName              +"\t"
                             +"stack="    +Utilities.nest()      +"}");
        }
  LineHistory = LineHistoryLimit > 0?new String[LineHistoryLimit]:null;
 }

// this is so clients can tell if we're at EOF without getting stuck when tailing.
 public boolean isNextLineAvailable()
 {
  initialize_();
  if (LineSegmentBegin1.value != -1)return true; // avoid rescanning if we already know.
  return findNextLineSegments();
 }

 public long length()
 {
  if (File == null)return -1;
  try {return File.length();}
  catch (IOException e){Utilities.fatalError("MyInputFile.length IOException{"
                                            +"e="+Utilities.toString(e)+"}");}
  return -1;
 }
 
 public static void main(String[] args)
 {
  selfTest();
 }

 public MyInputFile()
 {
  this(null,false,null,false);
 }

 public MyInputFile(String fileName)
 {
  this(fileName,false,null,false);
 }

 public MyInputFile(String fileName, boolean tail, String tailStart, boolean waitForFileToAppear)
 {
  FileName            = fileName;
  Tail                = tail;
  TailStart           = tailStart;
  WaitForFileToAppear = waitForFileToAppear;
 }

 private int readBytes(int start, int end)
 {
  try   {
         long offset = File.getFilePointer();
         int ret = File.read(Buffer,start,end-start);
         if (DeleteBOM && offset == 0 && ret >= 3)
            {
             if (Buffer[0] == -17 && // ef 11101111 00010000 00010001 17
                 Buffer[1] == -69 && // bb 10111011 01000100 01000101 69
                 Buffer[2] == -65)   // bf 10111111 01000000 01000001 65
                {
                 File.seek(3);
                 ret = File.read(Buffer,start,end-start);
                }
            }
         return ret;
        }
  catch (IOException e){Utilities.fatalError("MyInputFile.readBytesr IOException{"
                                            +"FileName="  +FileName              +"\t"
                                            +"exception=" +Utilities.toString(e) +"\t"
                                            +"stack="     +Utilities.nest()      +"}");}
  return -1;   // impossible
 }


 // BufferStartOffset == 0 && BufferEndOffset == 0             => Empty
 // BufferStartOffset == BufferEndOffset                       => Full
 // BufferStartOffset == 0 && BufferEndOffset == Buffer.length => Full
 // BufferStartOffset == Buffer.length                         => Error


 public String readLine()
 {
  initialize_();
  String result = readLine_();
  if (LineHistory != null)
     {
      LineHistory[NextLineHistoryIndex] = result;
      NextLineHistoryIndex ++;
      if (NextLineHistoryIndex >= LineHistory.length)NextLineHistoryIndex = 0;
     }
  if (result != null)CurrentLineNumber ++;
  if (Debug)Utilities.debug("MyInputFile.readLine{"
                           +"BufferEndOffset="  +BufferEndOffset  +"\t"
                           +"BufferStartOffset="+BufferStartOffset+"\t"
                           +"CurrentLineNumber="+CurrentLineNumber+"\t"
                           +"FileName="         +FileName         +"\t"
                           +"result="           +result           +"}");
  return result;
 }

 private String readLine_()
 {
  if (Debug)Utilities.debug("MyInputFile.readLine_{"
                           +"LineSegmentBegin1.value=" +LineSegmentBegin1.value  +"\t"
                           +"LineSegmentEnd1.value="   +LineSegmentEnd1  .value  +"\t"
                           +"LineSegmentBegin2.value=" +LineSegmentBegin2.value  +"\t"
                           +"LineSegmentEnd2.value="   +LineSegmentEnd2  .value  +"}");
  if (LineSegmentBegin1.value != -1)
      return extractLineSegments(); // avoid rescan if we already know where next line is.
  while (true)
        {
         if (findNextLineSegments())
             return extractLineSegments();
         if (!Tail)return null;
         Utilities.sleepMs(TailSleepMs);
        }
 }

// public String readLineSave2() 2nd obsolete version
// {
//  boolean eof = false;
//  int     pos;
//  String  result = null;
//  try {
//       lineCode:
//       while (true)
//             {
//              if (Debug)System.err.println("MyInputFile.readLine trace 1");
//              if (BufferStartOffset == Buffer.length) BufferStartOffset = 0;
//              if (BufferStartOffset == 0 && BufferEndOffset == 0)
//                 { // Buffer was empty
//                  if (Debug)System.err.println("MyInputFile.readLine trace 2");
//                  if (eof)break lineCode;
//                  eof = fillBuffer();  // Buffer was empty
//                  continue lineCode;
//                 }
//              
//              if (BufferStartOffset < BufferEndOffset)
//                 {
//                  if (Debug)System.err.println("MyInputFile.readLine trace 2{"
//                                              +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                              +"BufferStartOffset="+BufferStartOffset+"}");
//                  pos = findEOL(BufferStartOffset,BufferEndOffset);
//                  if (pos != -1)
//                     {
//                      result = new String(Buffer,BufferStartOffset,pos-BufferStartOffset,"UTF-8").replace("\r","");
//                      setBufferStartOffset(pos+1);
//                      break lineCode;
//                     }
//                  if (BufferEndOffset == Buffer.length)
//                     {
//                      if (Debug)System.err.println("MyInputFile.readLine trace 3{"
//                                                  +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                                  +"BufferStartOffset="+BufferStartOffset+"}");
//                      if (BufferStartOffset == 0)
//                         { // buffer is full and no \n
//                          if (Debug)System.err.println("MyInputFile.readLine trace 3.5 expandBuffer{"
//                                                      +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                                      +"BufferStartOffset="+BufferStartOffset+"}");
//                          expandBuffer();
//                          continue lineCode;
//                         }
//                       eof = fillBuffer();   // There is a gap at beginning of Buffer.  Fill it and go round again.
//                       continue lineCode;
//                     }
//                  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
//                   *                                                                           *
//                   *  EOF scanning contiguous data                                             *
//                   *                                                                           *
//                   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
//                  if (eof)
//                     {
//                      if (Tail)
//                         {
//                          Utilities.sleepMs(TailSleepMs);
//                          eof = false;
//                          break lineCode; // do not return partial line if tail is enabled.
//                         }
//                      result            = new String(Buffer, BufferStartOffset, BufferEndOffset - BufferStartOffset, "UTF-8").replace("\r", "");
//                      BufferStartOffset = 0;
//                      BufferEndOffset   = 0;
//                      break lineCode;
//                     }
//                  eof = fillBuffer(); // fill gap at end of buffer
//                  continue lineCode;
//                 }
//               // BufferStartOffset >= BufferEndOffset
//              if (Debug)System.err.println("MyInputFile.readLine trace 4 BufferStartOffset >= BufferEndOffset{"
//                                          +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                          +"BufferStartOffset="+BufferStartOffset+"}");
//              pos = findEOL(BufferStartOffset,Buffer.length);
//              if (pos != -1)
//                 {
//                  if (Debug)System.err.println("MyInputFile.readLine trace 5{"
//                                              +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                              +"BufferStartOffset="+BufferStartOffset+"}");
//                  result = new String(Buffer,BufferStartOffset,pos-BufferStartOffset,"UTF-8").replace("\r","");
//                  setBufferStartOffset(pos+1);
//                  break lineCode;
//                 }
//              // No eol at far side, check near side
//              pos = findEOL(0,BufferEndOffset);
//              if (pos != -1)
//                 {
//                  if (Debug)System.err.println("MyInputFile.readLine trace 6{"
//                                              +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                              +"BufferStartOffset="+BufferStartOffset+"}");
//                  byte[] contiguousBytes = new byte[Buffer.length-BufferStartOffset+pos];
//                  copyBytes(contiguousBytes,BufferStartOffset,pos);
//                  result            = new String(contiguousBytes,"UTF-8").replace("\r","");
//                  setBufferStartOffset(pos+1);
//                  break lineCode;
//                 }
//
//              if (Debug)System.err.println("MyInputFile.readLine trace 7 no eol on near side{"
//                                          +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                          +"BufferStartOffset="+BufferStartOffset+"}");
//
//               /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
//                *                                                                           *
//                *  EOF scanning second segment                                              *
//                *                                                                           *
//                * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
//              if (eof)
//                 {
//                  if (Debug)System.err.println("MyInputFile.readLine trace 7.5 eof without eol{"
//                                              +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                                              +"BufferStartOffset="+BufferStartOffset+"}");
//                  if (Tail)
//                     {
//                      Utilities.sleepMs(TailSleepMs);
//                      eof = false;
//                      break lineCode; // do not return partial line if tail is enabled.
//                     }
//                  result            = new String(Buffer, BufferStartOffset, Buffer.length - BufferStartOffset, "UTF-8")+
//                                      new String(Buffer,                 0, BufferEndOffset                  , "UTF-8");
//                  BufferStartOffset = 0;
//                  BufferEndOffset   = 0;
//                  break lineCode;
//                 }
//              // not found on far side or near side. Is it full?
//              if (BufferStartOffset == BufferEndOffset)
//                 { // and we know they are not zero from above so buffer is full
//                  expandBuffer();
//                  continue lineCode;
//                 }
//              if (Debug)System.err.println("MyInputFile.readLine trace 8");
//               // not found on far side or near side, but it is not full, so fill it and go round again.
//               eof = fillBuffer();
//               continue lineCode;
//             }
//      }
//  catch (UnsupportedEncodingException e){System.err.println("MyInputFile.readLine UnsupportedEncodingException"
//                                                           +"e="+e+"}");
//                                         System.exit(1);}
//  if (LineHistory != null)
//     {
//      LineHistory[NextLineHistoryIndex] = result;
//      NextLineHistoryIndex ++;
//      if (NextLineHistoryIndex >= LineHistory.length)NextLineHistoryIndex = 0;
//     }
//  if (result != null)CurrentLineNumber ++;
//  if (Debug)System.err.println("MyInputFile.readLine{"
//                              +"BufferEndOffset="  +BufferEndOffset  +"\t"
//                              +"BufferStartOffset="+BufferStartOffset+"\t"
//                              +"CurrentLineNumber="+CurrentLineNumber+"\t"
//                              +"FileName="         +FileName         +"\t"
//                              +"result="           +result           +"}");
//  return result;
// }

// public String readLineSave()
// {
//  boolean eof    = false;
//  String  result = null;
//  int     pos    = 0;
//  for (pos=StringBuffer.indexOf("\n");pos==-1;pos=StringBuffer.indexOf("\n"))
//      {
//       int nBytesRead=0;
//       
//       try   {nBytesRead = File.read(Buffer);}
//       catch (IOException e){System.err.println("MyInputFile.readLine IOException{"
//                                               +"FileName="  +FileName              +"\t"
//                                               +"exception=" +Utilities.toString(e) +"\t"
//                                               +"stack="     +Utilities.nest()      +"}");
//                             System.exit(1);}
//       if (nBytesRead < 0)
//          {
//           eof = true;
//           pos = StringBuffer.length();
//           break;
//          }
//       for (int i=0;i<nBytesRead;i++)CharBuffer[i]=(char)Buffer[i];
//       StringBuffer.append(CharBuffer,0,nBytesRead);
//      }
//
//  if (pos                        >  0   &&
//      StringBuffer.charAt(pos-1) == '\r')result=StringBuffer.substring(0,pos-1);
//  else                                   result=StringBuffer.substring(0,pos  );
//
//  StringBuffer.delete(0,pos+1);
//  if (result.length()==0 && eof)return null;
//  if (LineHistory != null)
//     {
//      LineHistory[NextLineHistoryIndex] = result;
//      NextLineHistoryIndex ++;
//      if (NextLineHistoryIndex >= LineHistory.length)NextLineHistoryIndex = 0;
//     }
//  CurrentLineNumber ++;
//  return result;
// }

 private void resetLineSegments()
 {
  LineSegmentBegin1.value = -1;
  LineSegmentEnd1  .value = -1;
  LineSegmentBegin2.value = -1;
  LineSegmentEnd2  .value = -1;
 }

 public void seek(long offset)
 {
  Long lineNumber = TellLineNumbers.get(offset);
  if (lineNumber == null)
     {
      Utilities.error("MyInputFile.seek line number not found{"
                     +"FileName="+FileName        +"\t"
                     +"offset="  +offset          +"\t"
                     +"stack="   +Utilities.nest()+"}");
      lineNumber = 0L;
     }  
  seek(offset,lineNumber);
 }

 public void seek(long offset,long lineNumber)
 {
  initialize_();
  BufferStartOffset = 0;
  BufferEndOffset   = 0;
  long fileOffset   = offset - CurrentFileOffset;
  try {File.seek(offset);}
  catch (IOException e){Utilities.fatalError("MyInputFile::Seek IOException{"
                                            +"FileName=" +FileName              +"\t"
                                            +"exception="+Utilities.toString(e) +"\t"
                                            +"stack=    "+Utilities.nest()      +"}");}
  CurrentLineNumber = lineNumber;
  resetLineSegments(); // 
 }

 private void setBufferStartOffset(int pos)
 {
  BufferStartOffset = pos;
  if (BufferStartOffset == BufferEndOffset)
     { // is empty.  Make sure we don't think it is full.
      BufferStartOffset = 0;
      BufferEndOffset   = 0; 
     }
 }

 public void setDebug(boolean debug){Debug=debug;}
 public void setTail (boolean value){Tail =value;}


 public void setLineHistoryLimit(int n)
 {
  if (n == 0)LineHistory = null;
  else       LineHistory = new String[n];
 }

 public long tell()
 {
  initialize_();
  long offset = tellPosition();
  TellLineNumbers.put(offset,CurrentLineNumber);
  return offset;
 }

 // provided to avoid huge line number history
 public long tellPosition()
 {
  initialize_();
  long fileOffset = 0;
  try {fileOffset = File.getFilePointer();}     
  catch (IOException e){Utilities.fatalError("MyInputFile::tell IOException{"
                                            +"exception="+Utilities.toString(e) +"\t"
                                            +"FileName=" +FileName              +"\t"
                                            +"stack="    +Utilities.nest()      +"}");}
  int bytes = 0;
  if (BufferStartOffset == 0 && BufferEndOffset == 0)bytes = 0                                ;else           //empty
  if (BufferStartOffset      <  BufferEndOffset     )bytes = BufferEndOffset-BufferStartOffset;else           //continuous
                                                     bytes = Buffer.length-BufferStartOffset+BufferEndOffset; //wrapped
  long offset = CurrentFileOffset + fileOffset - bytes;
  return offset;
 }

 public String stats()
 {
  return "{MyInputFile:Stats={"
        +"LinesRead="+CurrentLineNumber+"\t"
        +"FileName=" +FileName         +"}}";
 }

 public static void selfTest()
 {
  //  1. several lines
  //  2. large line to expand buffer
  //  3. wrap line
  //  4. huge line starting in middle of buffer but larger than buffer.
  //  5. line ending at end of buffer.
  //  6. empty file. 
  //  7. tail
  //  8. isNextLineAvailable
  //  9. last line does not have \n w/o Tail.
  // 10. line is exactly the size of the buffer.
  // 11. non-tail EOF when BufferEndOffset < BufferStartOffset.
  // 13. tell seek
  // 14. tail wait for data to arrive
  // 15. tail wait for file appear.
  // 16. coverage test

  String       bytes63    = new String("123456789012345678901234567890123456789012345678901234567890123");
  String       bytes127   = new String("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567");
  String       bytes128   = new String("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678");
  StringBuffer sb = new StringBuffer();
  for (int i=0;i<100;i++)sb.append(bytes128);
  String       bytes12800 = sb.toString();
  sb = new StringBuffer();
  for (int i=0;i<63;i++)sb.append(bytes128);
  sb.append(bytes127);
  String       bytes8191  = sb.toString();

  // test 1 several lines
  MyOutputFile mof = new MyOutputFile("TestFile1.txt",false);
  mof.write("1st line\n");
  mof.write("2nd line\n");
  mof.write("3rd line\n");
  mof.flush();
  mof.close();
  MyInputFile mif = new MyInputFile("TestFile1.txt");
  if (! mif.readLine().equals("1st line")){Utilities.error("Test 1 line 1 failed");}
  if (! mif.readLine().equals("2nd line")){Utilities.error("Test 1 line 2 failed");}
  if (! mif.readLine().equals("3rd line")){Utilities.error("Test 1 line 3 failed");}
  if (!(mif.readLine() == null          )){Utilities.error("Test 1 line 4 failed");}
  mif.close();

  // test 2 large line to expand buffer
  mof = new MyOutputFile("TestFile2.txt",false);
  mof.write(bytes12800);
  mof.write("\n");
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile2.txt");
  if (! mif.readLine().equals(bytes12800)){System.err.println("Test 2 line 1 failed");}
  if (!(mif.readLine() == null          )){System.err.println("Test 2 line 2 failed");}
  mif.close();


  // test 3 wrap line
  mof = new MyOutputFile("TestFile3.txt",false);
  for (int i=0;i<63;i++){mof.write(bytes127);mof.write("\n");} 
  mof.write(bytes63 );mof.write("\n"); 
  mof.write(bytes127);mof.write("\n"); // This line will wrap.
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile3.txt");
  for (int i=1;i<=63;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 3 line " +i + " failed");}
  if (! mif.readLine().equals(bytes63 )){Utilities.error("Test 3 line 64 failed");}
  if (! mif.readLine().equals(bytes127)){Utilities.error("Test 3 line 65 failed");}
  if (!(mif.readLine() == null        )){Utilities.error("Test 3 line 66 failed");}
  mif.close();

  // test 4 huge line starting in middle of buffer but larger than buffer.
  mof = new MyOutputFile("TestFile4.txt",false);
  for (int i=0;i<32;i++){mof.write(bytes127);mof.write("\n");} 
  mof.write(bytes12800);mof.write("\n"); 
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile4.txt");
  for (int i=1;i<=32;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 4 line " +i + " failed");}
  String line33 = mif.readLine();
  if (! line33        .equals(bytes12800)){Utilities.error("Test 4 line 33 failed{"
                                                             +"bytes12800="+bytes12800+"\t"
                                                             +"line33="    +line33    +"}");}
  if (!(mif.readLine() == null          )){Utilities.error("Test 4 line 34 failed");}
  mif.close();

  // test 5 line ending at end of buffer. 
  mof = new MyOutputFile("TestFile5.txt",false);
  for (int i=0;i<66;i++){mof.write(bytes127);mof.write("\n");} 
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile5.txt");
  for (int i=1;i<=66;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 5 line " +i + " failed");}
  if (!(mif.readLine() == null          )){Utilities.error("Test 5 line 67 failed");}
  mif.close();

  // test 6 empty file
  mof = new MyOutputFile("TestFile6.txt",false);
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile6.txt");
  if (!(mif.readLine() == null          )){Utilities.error("Test 6 line 1 failed");}
  mif.close();

  // test 7 tail
  mof = new MyOutputFile("TestFile7.txt",false);
  mof.write(bytes127);mof.write("\n");
  mof.flush();
  mif = new MyInputFile("TestFile7.txt");
  mif.setTail(true);
  if (!mif.readLine().equals(bytes127)){Utilities.error("Test 7 line 1 failed");}
  mof.write(bytes127);mof.write("\n");
  mof.flush();
  mof.close();
  if (!mif.readLine().equals(bytes127)){Utilities.error("Test 7 line 2 failed");}
  mif.close();

  // test  8 isNextLineAvailable
  mof = new MyOutputFile("TestFile8.txt",false);
  mof.write(bytes127);mof.write("\n");
  mof.flush();
  mif = new MyInputFile("TestFile8.txt");
  mif.setTail(true);
  if (!mif.isNextLineAvailable()      ){Utilities.error("Test 8 line 1 failed available");}
  if (!mif.readLine().equals(bytes127)){Utilities.error("Test 8 line 1 failed");}
  if ( mif.isNextLineAvailable()      ){Utilities.error("Test 8 line 2 failed available");}
  mof.write(bytes127);mof.write("\n");
  mof.flush();
  if (!mif.readLine().equals(bytes127)){Utilities.error("Test 8 line 2 failed");}
  mif.close();

  // test  9 last line does not have \n w/o Tail.
  mof = new MyOutputFile("TestFile9.txt",false);
  mof.write(bytes127);mof.write("\n");
  mof.write(bytes127);                
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile9.txt");
  if (! mif.readLine().equals(bytes127)){Utilities.error("Test 9 line 1 failed");}
  if (! mif.readLine().equals(bytes127)){Utilities.error("Test 9 line 2 failed");}
  if (!(mif.readLine() == null        )){Utilities.error("Test 9 line 3 failed");}
  mif.close();

  // test 10 line is exactly the size of the buffer.
  mof = new MyOutputFile("TestFile10.txt",false);
  mof.write(bytes8191);mof.write("\n");
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile10.txt");
  if (! mif.readLine().equals(bytes8191)){Utilities.error("Test 10 line 1 failed");}
  if (!(mif.readLine() == null         )){Utilities.error("Test 10 line 2 failed");}
  mif.close();

  // test 11 non-tail EOF when BufferEndOffset < BufferStartOffset.
  mof = new MyOutputFile("TestFile11.txt",false);
  for (int i=0;i<63;i++){mof.write(bytes127);mof.write("\n");} 
  mof.write(bytes63 );mof.write("\n"); 
  mof.write(bytes127);  // This line will wrap. No \n at EOF.
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile11.txt");
  for (int i=1;i<=63;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 3 line " +i + " failed");}
  if (! mif.readLine().equals(bytes63 )){Utilities.error("Test 3 line 64 failed");}
  if (! mif.readLine().equals(bytes127)){Utilities.error("Test 3 line 65 failed");}
  if (!(mif.readLine() == null        )){Utilities.error("Test 3 line 66 failed");}
  mif.close();
 
  // test 12 line history
  mof = new MyOutputFile("TestFile12.txt",false);
  for (int i=0;i<63;i++){mof.write(bytes127);mof.write("\n");} 
  mof.write(bytes63 );mof.write("\n"); 
  mof.write(bytes127);  // This line will wrap. No \n at EOF.
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile12.txt");
  for (int i=1;i<= 6;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 12 line " +i + " failed");}
  String[] lineHistory = mif.getLineHistory(); // test short list of lines.
  if (lineHistory.length != 6){Utilities.error("Test 12 lineHistory wrong length");}
  for (int i=0;i<  6;i++)if (!lineHistory[i].equals(bytes127)){Utilities.error("Test 12 line " +(i+1) + " failed");}
  for (int i=7;i<=63;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 12 line " +i + " failed");}
  lineHistory = mif.getLineHistory();
  for (int i=0;i<10;i++)if (!lineHistory[i] .equals(bytes127)){Utilities.error("Test 12 line " +(i+54) + " failed");}
  if (! mif.readLine().equals(bytes63 )){Utilities.error("Test 12 line 64 failed");}
  if (! mif.readLine().equals(bytes127)){Utilities.error("Test 12 line 65 failed");}
  if (!(mif.readLine() == null        )){Utilities.error("Test 12 line 66 failed");}
  mif.close();

  // test 13 tell seek
  mof = new MyOutputFile("TestFile13.txt",false);
  for (int i=0;i<63;i++){mof.write(bytes127);mof.write("\n");} 
  mof.flush();
  mof.close();
  mif = new MyInputFile("TestFile13.txt");
  long tell = mif.tell();
  for (int i=1;i<=63;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 13 line " +i + " failed");}
  if (!(mif.readLine() == null        ))                      {Utilities.error("Test 13 line 64 failed");}
  mif.seek(tell);
  tell = mif.tellPosition();
  for (int i=1;i<=63;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 13 line " +i + " failed");}
  if (!(mif.readLine() == null        ))                      {Utilities.error("Test 13 line 64 failed");}
  mif.seek(tell,0);
  for (int i=1;i<=63;i++)if (!mif.readLine().equals(bytes127)){Utilities.error("Test 13 line " +i + " failed");}
  if (!(mif.readLine() == null        ))                      {Utilities.error("Test 13 line 64 failed");}
  mif.close();

  // test 14 tail wait for data to arrive
  // test 15 tail wait for file appear
  // test 16 coverage test
 }

 public String toString()
 {
  return "{MyInputFile:\t"
        +"BufferEndOffset="  +BufferEndOffset  +"\t"
        +"BufferStartOffset="+BufferStartOffset+"\t"
        +"CurrentLineNumber="+CurrentLineNumber+"\t"
        +"File="             +File             +"\t"
        +"FileName="         +FileName         +"}";
 }
}
