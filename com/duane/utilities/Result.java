package com.duane.utilities;

import java.util.ArrayList;
import java.util.Date;

@ParmParserParm("Used to create or update variables")
public class Result implements Initializable 
{
 @ParmParserParm("determines whether or not to proceed. true => proceed with result") private Operation[]         Condition     = new Operation[0];
 @ParmParserParm("true|(false) controls whether to use commas in resulting number"  ) private boolean             Commas        = false;
 @ParmParserParm("true|(false)"                                                     ) private boolean             Debug         = false;
 @ParmParserParm("datetime|number|string default=string"                            ) private String              Format        = "string";
 @ParmParserParm("Repeat while Condition=true"                                      ) private boolean             Loop          = false;    
 @ParmParserParm("(optional)result column name"                                     ) private String              Name          = null;
 @ParmParserParm("Operations used to create result"                                 ) private Operation[]         Operation     = new Operation[0];
 @ParmParserParm("true|(false) Force output of record when result is produced"      ) private boolean             Output        = false;
 @ParmParserParm("number of decimal places in numeric result"                       ) private int                 Places        = 0;
                                                                                      private boolean             Repeat        = false;
                                                                                      private int                 ResultFieldNo = -1;
                                                                                      private ArrayList<String>   Stack         = new ArrayList<String>();
 public void initialize()
 {
  for (Operation operation : Operation)operation.initialize();
 }

 public String[] noteColumnNames(String[] columnNames)
 {
  if (Debug)System.err.println("Result.noteColumnNames{"
                              +"columnNames="+Utilities.toString(columnNames)+"\t"
                              +"Name="       +Name                           +"\t"
                              +"nest="       +Utilities.nest()               +"}");
  String[] result=null;
  if (Name == null)
     {
      result = new String[columnNames.length];
      for (int i=0;i<columnNames.length;i++)result[i]=columnNames[i];
     }
  else
     {
      for (int i = 0; i < columnNames.length; i++) if (columnNames[i].equals(Name)) ResultFieldNo = i;
      result        = ResultFieldNo==-1?new String[columnNames.length + 1]:new String[columnNames.length];
      ResultFieldNo = ResultFieldNo==-1?columnNames.length:ResultFieldNo;
      for (int i=0;i<columnNames.length;i++)result[i]=columnNames[i];
      result[ResultFieldNo] = Name;
     }
  for (Operation operation: Condition)result=operation.noteColumnNames(result);
  for (Operation operation: Operation)result=operation.noteColumnNames(result);
  if (Debug)System.err.println("Result.noteColumnNames{"
                              +"columnNames="       +Utilities.toString(columnNames)+"\t"
                              +"columnNames.length="+columnNames.length             +"\t"
                              +"Name="              +Name                           +"\t"
                              +"result="            +Utilities.toString(result     )+"\t"
                              +"result.length="     +result.length                  +"\t"
                              +"ResultFieldNo="     +ResultFieldNo                  +"}");
  return result;
 }

 public void noteNewValues(String[] values)
 {
  for (Operation operation : Condition)operation.noteNewValues(values);
  for (Operation operation : Operation)operation.noteNewValues(values);
 }

 private long EntryNo=0;
 public boolean process(String[] values)
// throws MyIOException
 {
  if (Debug)System.err.println("Result.process Entry{"
                              +"EntryNo="+(++EntryNo)               +"\t"
                              +"values=" +Utilities.toString(values)+"}");
  boolean ret = process_(values);
  noteNewValues(values);
  if (Debug)System.err.println("Result.process Exit{"
                              +"EntryNo="+EntryNo                   +"\t"
                              +"values=" +Utilities.toString(values)+"}");
  return ret;
 }

 public boolean process_(String[] values)
 // throws MyIOException
 {
  Stack = new ArrayList<String>();

  if (Condition != null && Condition.length > 0)
     {
      for (Operation operation : Condition)
          {
           if (Debug)System.err.println("Result.process_ note Condition Operation{"
                                       +"Name="     +Name                    +"\t"
                                       +"operation="+operation.getOperation()+"}");
           if (!operation.process(values, Stack))
              { // error in operation
               Repeat=false;
               return false;
              }
          }
      if (Stack.size() != 1){System.err.println("Result.process_ Stack size is not 1 after Condition processing{"
                                                                +"Result="+this.toString()         +"\t"
                                                                +"Stack="+Utilities.toString(Stack)+"}");
                             System.exit(1);
                            }
      String value = Stack.get(0);
      Stack.remove(0);
      if (value.equals("true"))
         {
          if (Loop)Repeat=true;
         }
      else
         {
          Repeat=false;
          return false;
         }
     }

  for (Operation operation : Operation)
      {
       if (Debug)System.err.println("Result.process_ note Operation{"
                                   +"Name="     +Name                    +"\t"
                                   +"operation="+operation.getOperation()+"}");
       if (!operation.process(values,Stack))return Output;
      }

  if (Name == null)return Output;

  if (Stack.size() != 1)
     {
      System.err.println("Result.process_ Stack size != 1{"
                        +"Name="        +Name                     +"\t"
                        +"Stack="       +Utilities.toString(Stack)+"\t"
                        +"Stack.size()="+Stack.size()             +"}");
      System.exit(1);
     }

  if (Format.equals("datetime"))
     {
      if (Places == 3)values[ResultFieldNo] = Utilities.toStringMillis(new Date(Utilities.parseLong(Stack.get(0))));
      else            values[ResultFieldNo] = Utilities.toString      (new Date(Utilities.parseLong(Stack.get(0))));
     }
  else
  if (Format.equals("number"))
     {
      String value = Stack.get(0);
      if (value.endsWith(".0"))value = value.substring(0,value.length()-2);
      values[ResultFieldNo] = Utilities.toString(value, Places);
      if (Commas)values[ResultFieldNo] = Utilities.commas(values[ResultFieldNo]);
     }
  else
  if (Format.equals("string"))
     {
      values[ResultFieldNo] = Stack.get(0);
     }
  else
      System.err.println("Result.process_ unknown format{"
                        +"Format="+Format+"\t"
                        +"Name="  +Name  +"}");
  return Output;
 }

 public boolean repeat(){return Repeat;}

 public void reset()
 {
  Repeat=false;
  for (Operation operation : Condition)operation.reset();
  for (Operation operation : Operation)operation.reset();
 }

 public Result(){}
 public Result(String value){Name=value;}

 public String toString()
 {
  String result="{Result:"
               +"Format="   +Format                       +"\t"
               +"Name="     +Name                         +"\t"
               +"Operation="+Utilities.toString(Operation)+"\t"
               +"Places="   +Places                       +"}";
  return result;
 }
}
