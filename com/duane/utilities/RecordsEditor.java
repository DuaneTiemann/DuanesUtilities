package com.duane.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.duane.utilities.Operation;
import com.duane.utilities.Result;

// This is to consolidate the editing for InputTsvFile, OutputTsvFile, and any new (database?) streams that may arise.

// Column names are associated with input (put) or output (get).
// For InputTsvFile, we need to negotiate column names. 
// OutputTsvFile Will call noteInputColumnNames which will record the column names provided and return the, potentially reduced, set of column names which will be 
// returned by get.
// InputTsvFile will call noteInputColumnNames and noteOutputColumnNames in noteColumnNames and which will only append to the list provided and ensure the mandated
// order. These will be the columns returned by get.

public class RecordsEditor implements Initializable
{
 @ParmParserParm("Names of columns to include. default=all") private String[]               Columns               = null;
 @ParmParserParm("true|(false)")                             protected boolean              Debug                 = false;
 @ParmParserParm("Columns to be excluded")                   private String[]               Drop                  = new String[0];
                                                             private String                 File                  = null;
                                                             private boolean                Initialized           = false;
                                                             private String[]               InputColumnNames      = null;
                                                             private String[]               InputValues           = null;
                                                             private HashMap<String,String> MapNewNamesToOldNames = null;
                                                             private ArrayList<String>      OutputColumnNames     = new ArrayList<String>();
                                                             private ArrayList<Integer>     OutputNumbersAL       = new ArrayList<Integer>();
                                                             private int[]                  OutputNumbers         = null;
                                                             private String[]               WorkColumnNames       = new String[0];
                                                             private String[]               WorkValues            = null;
                                                             private int                    NextResult            = 0;
 @ParmParserParm("{Name=NewName pairs..}")                   private HashMap<String,String> Rename                = new HashMap<String,String>();
 @ParmParserParm()                                           private Result[]               Result                = new Result[0];
 @ParmParserParm("Determines whether to include record. true => include")           
                                                             private Operation[]            Select                = new Operation[0];

 // This column is not mandated. It is added only if not in Drop and is in Columns (if anny).
//  What if we rename a column to another column that already exists?
//  That should be OK if the other column is dropped or not in Columns. That will be handled in vetRename.

 private void addColumnName(String columnName)
 {
  if (                   Utilities.indexOf(Drop   ,columnName) != -1)return;
  if (Columns != null && Utilities.indexOf(Columns,columnName) == -1)return;

  String newName = Rename.get(columnName);
  if (newName == null)newName = columnName;
  int index1 = Utilities.indexOf(OutputColumnNames, newName);
  if (index1 != -1)return;  // duplicate. 

  int index2 = Utilities.indexOf(WorkColumnNames,columnName);
  if (index2 == -1)Utilities.fatalError("RecordsEditor.addColumnName columnName not found{"
                                       +"columnName="+columnName+"\t"
                                       +"newName="   +newName   +"}");
  OutputColumnNames.add(newName);
  OutputNumbersAL  .add(index2 );
  if (Debug)Utilities.debug("RecordsEditor.addColumnName{"
                              +"columnName="            +columnName                           +"\t"
                              +"Debug="                 +Debug                                +"\t"
                              +"index1="                +index1                               +"\t"
                              +"index2="                +index2                               +"\t"
                              +"nest="                  +Utilities.nest()                     +"\t"
                              +"newName="               +newName                              +"\t"
                              +"OutputColumnNames="     +Utilities.toString(OutputColumnNames)+"\t"
                              +"OutputColumnNames.size="+OutputColumnNames.size()             +"\t"
                              +"OutputNumbersAL.size="  +OutputNumbersAL.size()               +"}");
 }

 // This column must be added to output. It was mandated by caller.
 // However, if it was dropped or not mentioned in Columns, its index is -1
 private void addRenamedColumnName(String columnName)
 {
  int index = Utilities.indexOf(OutputColumnNames, columnName);
  if (index != -1)return; // already there.   Should be impossible. Case is covered in vetRequestedColumns

  if (MapNewNamesToOldNames == null)
     {
      MapNewNamesToOldNames = new HashMap<String,String>();
      for (Map.Entry<String,String> entry : Rename.entrySet())
           MapNewNamesToOldNames.put(entry.getValue(),entry.getKey());
     }

  String oldName = MapNewNamesToOldNames.get(columnName);
  if (oldName==null)oldName=columnName;
  String newName = Rename.get(columnName);

  index = Utilities.indexOf(WorkColumnNames,oldName); // -1 is OK. It may be a column provided by another source.
  if (newName != null                                            )index = -1; // column was renamed to something else but is mandated. Not getting it from us.
                                                                              // Can not rename a column to itself unless it is dropped or not in Columns, which makes
                                                                              // the rename moot.
  if (                   Utilities.indexOf(Drop,   oldName) != -1)index = -1;
  if (Columns != null && Utilities.indexOf(Columns,oldName) == -1)index = -1;

  OutputColumnNames.add(columnName);
  OutputNumbersAL  .add(index     );
  if (Debug)Utilities.debug("RecordsEditor.addRenamedColumnName{"
                              +"index="           +index                                +"\t"
                              +"columnName="      +columnName                           +"\t"
                              +"newName="         +newName                              +"\t"
                              +"oldName="         +oldName                              +"\t"
                              +"OutputColumNames="+Utilities.toString(OutputColumnNames)+"\t"
                              +"WorkColumNames="  +Utilities.toString(WorkColumnNames)  +"\t"
                              +"nest="            +Utilities.nest()                     +"}");
 }

 public void finalize(){}

 public String[] get()
 {
  if (WorkValues == null)return null;
  for (;NextResult<Result.length;NextResult++)
      {
       Result  result = Result[NextResult];
       boolean output = result.process(WorkValues);
       boolean repeat = result.repeat();
       if (repeat) NextResult--;
       if (Debug)Utilities.debug("RecordsEditor.get note repeat{"
                                   +"NextResult="+NextResult+"\t"
                                   +"repeat="    +repeat    +"}");
       if (output)
          {
           NextResult++;
           String[] outputValues = getOutputValues();
           if (Debug)Utilities.debug("RecordsEditor.get1{"
                                       +"NextResult="  +NextResult                      +"\t"
                                       +"outputValues="+Utilities.toString(outputValues)+"}");
           return outputValues;
          }
      }
  NextResult = 0;
  boolean  selectResult = select();
  String[] outputValues = selectResult?getOutputValues():null;
  for (Result    result    : Result)result   .noteNewValues(WorkValues);
  for (Operation operation : Select)operation.noteNewValues(WorkValues);
  WorkValues = null;
  if (Debug)Utilities.debug("RecordsEditor.get2{"
                              +"outputValues="+Utilities.toString(outputValues)+"\t"
                              +"selectResult="+selectResult                    +"}");
  return outputValues;
 }

 public String[] getColumns(){return OutputColumnNames.toArray(new String[0]);}

 private String[] getOutputValues()
 {
  if (OutputNumbers == null)
     {
      OutputNumbers = new int[OutputNumbersAL.size()];
      for (int i = 0;i<OutputNumbers.length;i++)OutputNumbers[i] = OutputNumbersAL.get(i);
     }
  if (Debug)Utilities.debug("RecordsEditor.getOutputValues{"
                              +"OutputNumbersAL.size="+OutputNumbersAL.size()+"\t"
                              +"OutputNumbers.length="+OutputNumbers.length+"}");
  return getValues(WorkValues, OutputNumbers);
 }

 private String[] getValues(String[] source, int[] index)
 {
  String[] result = new String[index.length];

  for (int i=0;i<result.length;i++)
      {
       int index_ = index[i];
       if (index_ != -1)result[i] = source[index_];
      }
  if (Debug)Utilities.debug("RecordsEditor.getValues{"
                              +"index=" +Utilities.toString(index )+"\t"
                              +"result="+Utilities.toString(result)+"\t"
                              +"source="+Utilities.toString(source)+"}");

  return result;
 }

 public void initialize()
 {
  if (Initialized)return;

  Initialized = true;
  for (Result    result    : Result)result   .initialize();
  for (Operation operation : Select)operation.initialize();
 }

 public static void main(String[] args){selfTest();}

 public String[] noteInputColumnNames(String[] columnNames)
 {
  if (Debug)Utilities.debug("RecordsEditor.noteInputColumnNames enter{"
                              +"columnNames="+Utilities.toString(columnNames)+"\t"
                              +"nest="       +Utilities.nest()               +"}");
  InputColumnNames=Utilities.clone(columnNames);
  WorkColumnNames =Utilities.clone(columnNames);
  processColumnNames();
  if (Debug)Utilities.debug("RecordsEditor.noteInputColumnNames return{"
                              +"OutputColumnNames="+Utilities.toString(OutputColumnNames)+"}");
  return OutputColumnNames.toArray(new String[0]);
 }

 public void noteFile(String file){File=file;}

 public String[] noteOutputColumnNames(String[] columnNames)
 {
  if (Debug)Utilities.debug("RecordsEditor.noteOutputColumnNames enter{"
                              +"columnNames="+Utilities.toString(columnNames)+"}");
  processColumnNames(columnNames);
  if (Debug)Utilities.debug("RecordsEditor.noteOutputColumnNames return{"
                              +"OutputColumnNames="+Utilities.toString(OutputColumnNames)+"}");
  return OutputColumnNames.toArray(new String[0]);
 }

 public RecordsEditor(){}
 public RecordsEditor(String[] columns)
 {
  WorkColumnNames = columns;
 }

 // drop, assign output column numbers, rename
 public void processColumnNames(){processColumnNames(new String[0]);}
 public void processColumnNames(String[] requestedColumns)
 {
  if (Debug)Utilities.debug("RecordsEditor.processColumnNames Entry note columns{"
                              +"Columns="          +Utilities.toString(Columns)          +"\t"
                              +"OutputColumnNames="+Utilities.toString(OutputColumnNames)+"\t"
                              +"requestedColumns=" +Utilities.toString(requestedColumns) +"}");

  for (Result    result    : Result)WorkColumnNames = result   .noteColumnNames(WorkColumnNames);
  for (Operation operation : Select)WorkColumnNames = operation.noteColumnNames(WorkColumnNames);

  vetRequestedColumns(requestedColumns);
  vetColumns();
  vetDrop   ();
  vetRename ();
  OutputColumnNames = new ArrayList<String>();
  OutputNumbersAL   = new ArrayList<Integer>();
  for (String columnName : requestedColumns)addRenamedColumnName(columnName);
  if (Columns != null)for (String columnName : Columns        )addColumnName(columnName);
  else                for (String columnName : WorkColumnNames)addColumnName(columnName);

  if (Debug)Utilities.debug("RecordsEditor.processColumnNames note columns{"
                              +"Columns="           +Utilities.toString(Columns          )+"\t"
                              +"OutputColumnNames=" +Utilities.toString(OutputColumnNames)+"}");
 }

 public void put(String[] values)
 {
  if (values == null)         // Should be impossible. Caller should not give us a null record.
      values = new String[0]; // But if they do, pretend all columns are empty.
  WorkValues = new String[WorkColumnNames.length];
  int limit  = values.length<WorkValues.length?values.length:WorkValues.length;   // Short array is impossible because InputTsvFile checks for it.
  for (int i = 0; i < limit; i++)WorkValues[i] = values[i];
 }


 // reset is not currently in use.
 public void reset()
 {
  for (Result    result    : Result)result   .reset();
  for (Operation operation : Select)operation.reset();
 }

 private static void selfTest()
 {
 }

 private boolean select()
 {
  if (Select.length == 0)return true;
  ArrayList<String> stack = new ArrayList<String>();
  for (Operation operation : Select)operation.process(WorkValues,stack);
  if (stack.size() != 1)Utilities.fatalError("RecordEditor.select Stack size is not 1 after Select processing{"
                                                            +"nest="      +Utilities.nest()              +"\t"
                                                            +"stack="     +Utilities.toString(stack     )+"\t"
                                                            +"WorkValues="+Utilities.toString(WorkValues)+"}");
  return stack.get(0).equals("true");
 }

 public String toString()
 {
  return "{RecordsEditor:"
         +"Columns="          +Utilities.toString(Columns          )+"\t"
         +"File="             +File                                 +"\t"
         +"InputColumnNames=" +Utilities.toString(InputColumnNames )+"\t"
         +"OutputColumnNames="+Utilities.toString(OutputColumnNames)+"}";
 }

 private void vetColumns()
 {
  if (Columns == null)return;
  for (String column : Columns)
       if (Utilities.indexOf(WorkColumnNames,column) == -1)Utilities.fatalError("RecordsEditor.vetColumns column name not found{"
                                                                               +"column="     +column+"\t"
                                                                               +"ColumnNames="+Utilities.toString(WorkColumnNames)+"\t"
                                                                               +"File="       +File                               +"}");
 }

 private void vetDrop()
 {
  for (String drop : Drop)
      if (Utilities.indexOf(WorkColumnNames,drop)==-1)Utilities.fatalError("RecordsEditor.vetDrop column name not found{"            
                                                                           +"ColumnNames="+Utilities.toString(WorkColumnNames)+"\t"
                                                                           +"drop="       +drop                               +"\t"
                                                                           +"Drop="       +Utilities.toString(Drop)           +"\t"
                                                                           +"File="       +File                               +"}");
 }

// What if we rename a column to another column that already exists?
// That should be OK if the other column is dropped or not in Columns.

 private void vetRename()
 {
  for (Map.Entry<String,String> entry : Rename.entrySet())
      {
       String oldName = entry.getKey  ();
       String newName = entry.getValue();
       if (Utilities.indexOf(WorkColumnNames,oldName)==-1)Utilities.fatalError("RecordsEditor.vetRename renamed column not found{"             
                                                                              +"oldName="    +oldName                            +"\t"                                         
                                                                              +"newName="    +newName                            +"\t"
                                                                              +"File="       +File                               +"\t"
                                                                              +"WorkColumns="+Utilities.toString(WorkColumnNames)+"}");     
       if (Utilities.indexOf(WorkColumnNames,newName)!=-1) 
          { // newName already exists. 
           boolean error = true;
           if (                 Utilities.indexOf(Drop   ,newName) != -1)error=false; // new name was dropped.
           if (Columns!=null && Utilities.indexOf(Columns,newName) == -1)error=false; // new name was not in Columns.
           if (error)
               Utilities.fatalError("RecordsEditor.vetRename new column name already exists and is not dropped or ommitted from Columns{"
                                   +"Columns="    +Utilities.toString(Columns)        +"\t"
                                   +"Drop="       +Utilities.toString(Drop   )        +"\t"
                                   +"oldName="    +oldName                            +"\t"                                         
                                   +"newName="    +newName                            +"\t"
                                   +"File="       +File                               +"\t"
                                   +"WorkColumns="+Utilities.toString(WorkColumnNames)+"}");     
          }
      }

 }

 private void vetRequestedColumns(String[] requestedColumns)
 {
  for (int i=0;i<requestedColumns.length;i++)
       for (int j=i+1;j<requestedColumns.length;j++)
            if (requestedColumns[i].equals(requestedColumns[j]))Utilities.fatalError("RecordsEditor.vetRequestedColumns duplicate column{"
                                                                                    +"column="+requestedColumns[i]+"\t"
                                                                                    +"File="  +File               +"}");
//  for (String requestedColumn : requestedColumns)
//       if (Utilities.indexOf(Drop,requestedColumn) != -1){System.err.println("RecordsEditor.vetRequestedColumns requestedColumn is Dropped{"
//                                                                            +"Drop="            +Utilities.toString(Drop)             +"\t"
//                                                                            +"File="            +File                                 +"\t"
//                                                                            +"requestedcolumn=" +requestedColumn                      +"\t"
//                                                                            +"requestedcolumns="+Utilities.toString(requestedColumns) +"}");                  
//                                                          System.exit(1);}                                                        
//  if (Columns != null)
//      for (String requestedColumn : requestedColumns)
//           if (Utilities.indexOf(Columns,requestedColumn) == -1){System.err.println("RecordsEditor.vetRequestedColumns requestedColumn is not in Columns{"
//                                                                                   +"Columns="         +Utilities.toString(Columns)          +"\t"
//                                                                                   +"File="            +File                                 +"\t"
//                                                                                   +"requestedcolumn=" +requestedColumn                      +"\t"
//                                                                                   +"requestedcolumns="+Utilities.toString(requestedColumns) +"}");                  
//                                                                 System.exit(1);}                                                        
 }
}
