package com.duane.utilities;

import com.duane.utilities.InputTsvFile;
import com.duane.utilities.MyOutputFile;
import com.duane.utilities.RecordsEditor;
import com.duane.utilities.Utilities;

@ParmParserParm("Output tsv file:"                        +"\t"
               +"Values are separated by tabs"            +"\t"
               +"The first line will contain column names")
public class OutputTsvFile extends    RecordsEditor
                           implements Initializable
{
 @ParmParserParm private MyOutputFile File              = null;
                 private boolean      Initialized       = false;
                 private String[]     OutputColumnNames = null;
                 private char         Separator         = '\t';
                 private String       SeparatorString   = null;
                 private boolean      WroteHeader       = false;

 private boolean append()
 {
  if (File     == null )return false;
  if (!File.getAppend())return false;
  if (length() == 0    )return false;

  InputTsvFile inputTsvFile       = new InputTsvFile(File.getFileName());
  String[]     currentColumnNames = inputTsvFile.noteColumnNames(new String[0]);
  inputTsvFile.close();
  if (Utilities.compare(currentColumnNames,OutputColumnNames)!=0)
      Utilities.fatalError("OutputTsvFile.append current columnnames differ{"
                          +"currentColumnNames="+Utilities.toString(currentColumnNames)+"\t"
                          +"newColumnNames="    +Utilities.toString(OutputColumnNames )+"}");
  return true;
 }

 public void close()
 {
  if (File != null)
     {
      if (!WroteHeader)
         {
          writeRow(OutputColumnNames);
          WroteHeader = true;
         }
      File.flush();
      File.close();
     }
  WroteHeader = false;
 }

 public void finalize()
 {
  close();
  super.finalize();
 }

 public void flush(){File.flush();}

 public String getFileName    (){return File.getFileName    ();}
 public long   getLinesWritten(){return File.getLinesWritten();}
 public void initialize()
 {
//  if (Debug)System.err.println("OutputTsvFile.initialize");
  if (Initialized)return;
  super.initialize();
  super.noteFile(File.getFileName());
  SeparatorString = new String(new char[]{Separator});
  Initialized = true;
 }

 public boolean isOpen(){return File.isOpen();}

 public String[] noteColumnNames(String[] columnNames)
 {
  OutputColumnNames = super.noteInputColumnNames(columnNames);
  if (Debug)Utilities.debug("OutputTsvFile.noteColumnNames{"
                           +"FileName="+File.getFileName()                  +"\t"
                           +"columnNames="  +Utilities.toString(columnNames)+"}");
  return columnNames; // Don't tell caller what our column names will be.
 }

 public long length()
 {
  if (File == null)return -1;
  return File.length();
 }

 public OutputTsvFile()
 {
  super();
 }

 public OutputTsvFile(String fileName)
 {
  this(fileName,null);
 }

 public OutputTsvFile(String fileName, String[] columns)
 {
  super(columns);
  File = new MyOutputFile(fileName);
  OutputColumnNames = columns;
 }

 public void open(){File.open();}

 public void put(String[] values)
// throws MyIOException
 {
  if (values == null)return;

  if (!WroteHeader)
     {
      if (!append())writeRow(OutputColumnNames);
      WroteHeader = true;
     }

  super.put(values);

  for (String[] result = super.get();result!=null;result=super.get())
       writeRow(result);
 }

 public void   setFileName(String  value){File.setFileName(value);}
 public void   setLock    (boolean lock ){File.setLock    (lock );}

 public String stats(){return File.stats();}

 public String toString()
 {
  if (File == null)return "{OutputTsvFile:File=null";
  return "{OutputTsvFile:"
        +"File="+File.toString()+"}";
 }

 private void writeRow(String[] values)
 {
  if (Debug)Utilities.debug("OutputTsvFile.writeRow{"
                           +"values="+Utilities.toString(values)+"}");
  StringBuffer output = new StringBuffer();
  boolean first = true;
  for (String value : values)
      {
       if (Separator == ',' &&
           (value.contains("," ) ||
            value.contains("\"")))
          {
           value = value.replace("\"","\"\"");
           value = "\"" + value + "\"";
          }

       if (first)first=false;
       else      output.append(SeparatorString);
       if (value!=null)output.append(value);
      }
  output.append(File.getEOL());
  File.write(output.toString());
  if (Debug)Utilities.debug("OutputTsvFile.writeRow return");
 }
}
