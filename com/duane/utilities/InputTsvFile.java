package com.duane.utilities;

import com.duane.utilities.MyInputFile;
import com.duane.utilities.RecordsEditor;
import com.duane.utilities.Utilities;
import java.util.ArrayList;

// Can't inherit from 2 classes so have to replicate MyInputFile parms. Sucks.

@ParmParserParm("Input tsv file:"                                           +"\t"
               +"Values are usually separated by tabs"                      +"\t"
               +"The first line is usually expected to contain column names")
public class InputTsvFile extends    RecordsEditor
                          implements Initializable
{
 @ParmParserParm("true|(false)"                 ) private boolean                AllowMissingColumns = false;
 @ParmParserParm("(required)"                   ) private MyInputFile            File                = null;
 @ParmParserParm("Column names to be used when file doesn't have header row.")
                                                  private String[]               FileColumnNames     = null;
 @ParmParserParm("Line number of header row (1)") private long                   HeaderLineNo        = 1;
                                                  private boolean                Initialized         = false;
 @ParmParserParm("Separator (tab)"                                          +"\t"+
                 "If Separator is a comma, lines get extended csv treatment"+"\t"+
                 "i.e. lines starting with # are ignored"                   +"\t"+
                 "     and fields use double quote to escape double quote")
                                                  private String                 Separator           = "\t";
 @ParmParserParm("Trim leading and trailing spaces from fields (true)")
                                                  private boolean                Trim                = true;
 private enum State 
 {
  BETWEEN_FIELDS          ,
  FOUND_DQ_IN_QUOTED_FIELD,
  IN_NONQUOTED_FIELD      ,
  IN_QUOTED_FIELD    
 };

 public void close(){File.close();}

 public void finalize()
 {
  File.close();
  super.finalize();
 }

 public String[] get()
 {
  String[] result = get_();
  if (Debug)Utilities.debug("InputTsvFile.get note result{"
                              +"Debug=" +Debug                     +"\t"
                              +"File="  +File.getFileName()        +"\t"
                              +"result="+Utilities.toString(result)+"}");
  return result;
 }

 public String[] get_()
 {
  String result[] = super.get();

  while (result == null)
        {
         String[] columns = getLocalColumns();
         if (columns == null)return null;
         if (columns.length < FileColumnNames.length)
            {
             if (!AllowMissingColumns)
                 Utilities.error("InputTsvFile.get_ not enough columns in line. Missing columns treated as zero length{"
                                +"columns.length="+columns.length             +"\t"
                                +"FileName="      +File.getFileName()         +"\t"
                                +"lineNo="        +File.getCurrentLineNumber()+"}");
             columns = Utilities.concatenateArrays(columns,new String[FileColumnNames.length-columns.length]);
            }
        
         super.put(columns);
         if (Debug)Utilities.debug("InputTsvFile.get_ note columns{"
                                  +"columns="+Utilities.toString(columns)+"}");
         result = super.get();
         if (Debug)Utilities.debug("InputTsvFile.get_ note result{"
                                  +"result="+Utilities.toString(result)+"}");
        }
  if (Debug)Utilities.debug("InputTsvFile.get_ note returned result{"
                           +"result="+Utilities.toString(result)+"}");
  return result;
 }

 private String[] getLocalColumns()
 {
  String[] result = null;
  if (Separator.equals(","))
      result = getCommaColumns();
  else
     {
      String line = File.readLine();
      if (line == null)return null;
      result = line.split(Separator, -1);
      if (Debug)Utilities.debug("InputTsvFile.getLocalColumns line{"
                               +"line="+line+"}");
     }
  if (Debug)Utilities.debug("InputTsvFile.getLocalColumns result before trim{"
                           +"result="+Utilities.toString(result)+"}");
  if (Trim && result != null) for (int i = 0; i < result.length; i++) result[i] = result[i].trim();
  if (Debug)Utilities.debug("InputTsvFile.getLocalColumns result after trim{"
                           +"result="+Utilities.toString(result)+"}");
  return result;
 }

 private String[] getCommaColumns()
 {
  String line;
  while(true)
       {
        line = File.readLine();
        if (line == null) return null;
        if (!line.startsWith("#"))break;
       }
  if (line.length()==0)return new String[]{""};
  ArrayList<String> fields = new ArrayList<String>();
  StringBuffer      currentField = new StringBuffer();
  State             state = State.BETWEEN_FIELDS;

  if (Debug)Utilities.debug("InputTsvFile.getCommaColumns{"
                           +"line="+line+"}");
  for (int i = 0; i < line.length(); i++)
      {
       char c = line.charAt(i);
       if (Debug && c == ',')Utilities.debug("InputTsvFile.getCommaColumns found comma{"
                                            +"i="    +i    +"\t"
                                            +"state="+state+"}");
       switch (state)
              {
               case BETWEEN_FIELDS:
                    switch (c)
                           {
                            case ' ' :
                            case '\t': 
                            case 0x1a:
                                 continue;

                            case ',':
                                  state=State.BETWEEN_FIELDS;
                                  fields.add(currentField.toString());
                                  currentField.setLength(0);
                                  break;

                             case '"': 
                                  state=State.IN_QUOTED_FIELD;
                                  break;

                             default:
                                  currentField.append(c);
                                  state=State.IN_NONQUOTED_FIELD;
                                  break;
                            }                                                                                                           
                    break;                                                                                                          
               case FOUND_DQ_IN_QUOTED_FIELD:                                                                                    
                    if (c == ',')
                       {
                        state=State.BETWEEN_FIELDS;
                        fields.add(currentField.toString());
                        currentField.setLength(0);
                        break;
                       }
                    state=State.IN_QUOTED_FIELD;
                    currentField.append(c);            // Forgive unescaped "
                    break;
               case IN_NONQUOTED_FIELD:                                                
                    switch(c)
                          {
                           case ',':
                                state=State.BETWEEN_FIELDS;
                                fields.add(currentField.toString());
                                currentField.setLength(0);
                                break;
                           default:
                                currentField.append(c);
                                break;
                          }
                     break;
               case IN_QUOTED_FIELD:
                    if (c=='\"')
                       {
                        state=State.FOUND_DQ_IN_QUOTED_FIELD;
                        break;
                       }
                    currentField.append(c);
                    break;
              }
      }
  fields.add(currentField.toString());
  if (FileColumnNames != null)
      for (int i = fields.size(); i < FileColumnNames.length; i++) fields.add("");

  String[] result = fields.toArray(new String[0]);
  if (Trim)for (int i=0;i<result.length;i++)result[i]=result[i].trim();
  return result;
 }

 public long    getCurrentLineNumber(){return File.getCurrentLineNumber();}
 public String  getFileName         (){return File.getFileName         ();}
 public int     getLineHistoryLimit (){return File.getLineHistoryLimit ();}
 public boolean getTail             (){return File.getTail             ();}

 // We don't want internal calls to initialize to invoke subclass's initialize()
 public void initialize(){initialize_();}
 private void initialize_()
 {
//  if (Debug)System.err.println("InputTsvFile.initialize");
//  System.err.println("InputTsvFile.initialize");
  if (Initialized)return;
  Initialized = true;
  if (File!=null)File.setDebug(Debug);
  if (Debug)Utilities.debug("InputTsvFile.initialize_ enter{"
                           +"HashCode=" +this.hashCode()+"\t"
                           +"Separator="+Separator      +"}");
  super.initialize();
  File.initialize();
  super.noteFile(File.getFileName());

  if (FileColumnNames == null)
     {
      if (Debug)Utilities.debug("InputTsvFile.initialize_ do FileColumnNames");
      for (int i = 1; i < HeaderLineNo; i++)
          {
           String line = File.readLine();
           if (line == null)
               Utilities.fatalError("InputTsvFile.initialize_ file has no header row{"
                                   +"FileName="+File.getFileName()+"}");
          }

      if (Debug)Utilities.debug("InputTsvFile.initialize_ do getLocalColumns{"
                               +"Separator="+Separator+"}");
      FileColumnNames = getLocalColumns();
      if (Debug)Utilities.debug("InputTsvFile.initialize_ note FileColumnNames{"
                               +"FileColumnNames="+Utilities.toString(FileColumnNames)+"}");
     }
  noteInputColumnNames(FileColumnNames);
 }

 public InputTsvFile()
 {
  if (Debug)Utilities.debug("InputTsvFile.InputTsvFile{"
                           +"HashCode=" +this.hashCode() +"\t"
                           +"nest="     +Utilities.nest()+"}");
 }

 public InputTsvFile(String fileName)
 {
  if (File == null)File = new MyInputFile(fileName);
  if (Debug)Utilities.debug("InputTsvFile.InputTsvFile{"
                           +"HashCode=" +this.hashCode() +"\t"
                           +"nest="     +Utilities.nest()+"}");
 }

 public InputTsvFile(String fileName, String[] columns)
 {
  this(fileName);
  super.noteOutputColumnNames(columns);
  if (Debug)Utilities.debug("InputTsvFile.InputTsvFile{"
                           +"HashCode=" +this.hashCode() +"\t"
                           +"nest="     +Utilities.nest()+"}");
 }

 public boolean isNextLineAvailable(){return File.isNextLineAvailable();}

 public long length()
 {
  if (File == null)return -1;
  return File.length();
 }

 public String[] noteColumnNames(String[] columnNames)
 {
  if (Debug)Utilities.debug("InputTsvFile.noteColumnNames enter{"
                           +"columnNames="+Utilities.toString(columnNames)+"}");
  initialize();
  String[] result=super.noteOutputColumnNames(columnNames);
  if (Debug)Utilities.debug("InputTsvFile.noteColumnNames return{"
                           +"result="+Utilities.toString(result)+"}");
  return result;
 }

 public void   seek                  (long    value){File.seek(value);}
 public void   seek(long offset,long lineNumber)    {File.seek(offset,lineNumber);}

 public void   setAllowMissingColumns(boolean value){AllowMissingColumns=value;}
 public void   setDebug              (boolean value){Debug=value;}
 public String stats                 (             ){return File.stats();}
 public long   tell                  (             ){return File.tell();}
 public long   tellPosition          (             ){return File.tellPosition();}

 public String toString()
 {
  return "{InputTsvFile:"
         +"AllowMissingColumns="+AllowMissingColumns                +"\t"
         +"FileColumnNames="    +Utilities.toString(FileColumnNames)+"\t"
         +"File="               +Utilities.toString(File           )+"\t"
         +"HeaderLineNo="       +HeaderLineNo                       +"\t"
         +"Initialized="        +Initialized                        +"\t"
         +"Separator="          +Separator                          +"\t"
         +"Trim="               +Trim                               +"}";
 }
}
