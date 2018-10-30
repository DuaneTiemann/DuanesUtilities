package com.duane.utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

@ParmParserParm("FileName")
public class MyOutputFile
{
 @ParmParserParm("true|(false)"                                         ) private boolean          Append           = false;
 @ParmParserParm("end of line characters to use. default \\r\\n"        ) private String           EOL              = "\r\n";
                                                                          private FileLock         FileLock         = null;
 @ParmParserParm("name of output file"                                  ) private String           FileName         = null;
 @ParmParserParm("(none): let OS determine disk writes. Much quicker than data.\t"
                +"data: flush data to disk on each write\t"
                +"meta: flush data and position pointer on each write. A little slower than data.")
                                                                          private String           Flush            = "none";
 @ParmParserParm("Flush data and position pointer on each write"        ) private boolean          FlushDataMeta    = true;
                                                                          private boolean          LinePending      = false;
                                                                          private long             LinesWritten     = 0;

 @ParmParserParm("true|(false) May be set by utility if not specified here."
                +"File is protected from multiple utility instances writing to it accidentally. There is no protection from\t"
                +"or interference with other applications, editors, etc. Creates (and locks) associated .lock file.\t"
                +".lock files can be deleted if the happen to linger after crashes, etc.")
                                                                          private Boolean          Lock             = null;
                                                                          private boolean          Locked           = false;
                                                                          private RandomAccessFile LockFile         = null;
                                                                          private boolean          Opened           = false;
                                                                          private RandomAccessFile RandomAccessFile = null;
 
 public synchronized void close()
 {
  open();
  if (LinePending)LinesWritten++;
  if (RandomAccessFile == null) return;
  if (Locked)unlock();
  LinePending    = false;
  Opened         = false;
 }
 
 public void flush()
 {
  open();
 }
 
 public void finalize()
 {
  close();
 }
 
 public boolean getAppend      (){return Append      ;}
 public String  getEOL         (){return EOL         ;}
 public String  getFileName    (){return FileName    ;}
 public long    getLinesWritten(){return LinesWritten;}
 public boolean isOpen         (){return Opened      ;}

 public MyOutputFile(){}
 public MyOutputFile(String fileName                ){this(fileName,false);}
 public MyOutputFile(String fileName, boolean append){FileName = fileName;Append=append;}

 public long length()
 {
  if (RandomAccessFile == null)return -1;
  try {return RandomAccessFile.length();}
  catch (IOException e){Utilities.fatalError("MyOutputFile.length IOException{"
                                            +"e="+Utilities.toString(e)+"}");}
  return -1;
 }
 
 private void lock()
 {
  try {
       LockFile = new RandomAccessFile(FileName+".lock", "rws");
       FileLock = LockFile.getChannel().tryLock(); // get exclusive lock
       if (FileLock == null)Utilities.fatalError("MyOutputFile.lock File is in use{"
                                                 +"FileName="+FileName+"}");
      }
 catch (FileNotFoundException e){Utilities.fatalError("MyOutputFile.lock FileNotFoundException{"
                                                     +"exception="+Utilities.toString(e)+"\t"
                                                     +"FileName=" +FileName+".lock"     +"}");}
 catch (IOException           e){Utilities.fatalError("MyOutputFile.lock IOException{"
                                                    +"exception="+Utilities.toString(e)+"\t"
                                                    +"FileName=" +FileName+".lock"     +"}");}
  Locked = true;
 }

 public void open()
 {
  if (Opened)return;
  if (FileName == null) return;
  LinesWritten = 0;
  Opened       = true;
  Locked       = false;
  if (Lock == null)Lock = false;
  try {
      switch (Flush.toLowerCase())
             {
              case "":
              case "none": RandomAccessFile = new RandomAccessFile(FileName, "rw" ); break;
              case "data": RandomAccessFile = new RandomAccessFile(FileName, "rws"); break;
              case "meta": RandomAccessFile = new RandomAccessFile(FileName, "rwd"); break;
              default    : Utilities.error("MyOutputFile.open unknown Flush{"
                                          +"Flush="+Flush+"}");
             }
       if (Lock)lock();
       if (Append)
           RandomAccessFile.seek(RandomAccessFile.length());
       else
          {
           RandomAccessFile.setLength(0);
           RandomAccessFile.seek     (0);
          }
      }
  catch (FileNotFoundException e){Utilities.fatalError("MyOutputFile.open FileNotFoundException{"
                                                      +"exception="+Utilities.toString(e)+"\t"
                                                      +"FileName=" +FileName             +"}");}
  catch (IOException           e){Utilities.fatalError("MyOutputFile.open IOException{"
                                                     +"exception="+Utilities.toString(e)+"\t"
                                                     +"FileName=" +FileName             +"}");}
 }

 public void setFileName(String value){FileName=value;}

 public String stats()
 {
  return "{MyOutputFile Stats{"
        +"LinesWritten="+LinesWritten+"\t"
        +"FileName=" +FileName         +"}";
 }

 public void setLock(boolean lock){if (Lock==null)Lock=lock;}

 public String toString()
 {
  return "{MyOutputFile:"
        +"FileName="+FileName+"}";
 }

 private void unlock()
 {
  try {
       FileLock.release();
       FileLock = null;
       LockFile.close();
       LockFile = null;
       new File(FileName+".lock").delete();
       Locked = false;
      }
  catch (FileNotFoundException e){Utilities.fatalError("MyOutputFile.unlock FileNotFoundException{"
                                                      +"exception="+Utilities.toString(e)+"\t"
                                                      +"FileName=" +FileName+".lock"     +"}");}
  catch (IOException           e){Utilities.fatalError("MyOutputFile.unlock IOException{"
                                                     +"exception="+Utilities.toString(e)+"\t"
                                                     +"FileName=" +FileName+".lock"     +"}");}
 }

 public void write(String s)
 {
  open();
  if (RandomAccessFile == null)return;

  for (int pos=s.indexOf('\n');pos!=-1;pos=s.indexOf('\n',pos+1))LinesWritten++;
  LinePending = s.length()>0 && s.charAt(s.length()-1) != '\n';
  try {RandomAccessFile.writeBytes(s);}
  catch (IOException e){Utilities.fatalError("MyOutputFile::flush IOException{"
                                            +"exception="+Utilities.toString(e)+"\t"
                                            +"FileName=" +FileName             +"}");}
 }
}
