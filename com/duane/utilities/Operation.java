package com.duane.utilities;

import java.util.ArrayList;
import java.util.Date;

@ParmParserParm("Operations are applied in Reverse Polish fashion")
public class Operation implements Initializable
{
 @ParmParserParm("true|(false)"                                               ) private boolean             Debug           =false;
 @ParmParserParm("datetime|number|string default=string"                      ) private String              Format          ="string";
 @ParmParserParm("true|false default=false, true=>use value from prior record") private boolean             Lag             =false;
 @ParmParserParm("number of records back to use when Lag is true. default=1"  ) private long                LagN            =1;
 @ParmParserParm("Reset Lag values on by breaks. default=true"                ) private boolean             LagReset        =true;
                                                                                private ArrayList<String[]> LagValues       =new ArrayList<String[]>();
 @ParmParserParm("Name(s) of column(s) to be used")                             private String[]            Name            =null;
 @ParmParserParm("add|subtract|multiply|divide|push|floor|and|concatenate|endswith|endwith|"          +"\t"
                +"hextodec|indexof|lastindexof|lowcase|not|numberoffields|or|padleftzero|pop|(push)|" +"\t"
                +"replaceall|replacefirst|replaceLast|set|split|startswith|startwith|"                +"\t"
                +"upcase|upcasefirst|upcasefirstofwords")
                                                                                private String              Operation       ="push";
                                                                                private int[]               OperationFieldNo=null;
 @ParmParserParm("Constant(s) to be used")                                      private String[]            Value           =null;

 private String[] addStringToArray(String[] array,String s)
 {
  String[] result = new String[array.length+1];
  for (int i=0;i<array.length;i++)result[i] = array[i];
  result[array.length] = s;
  return result;
 }

 private void checkStack(ArrayList<String> stack,int size)
// throws MyIOException
 {
  if (stack.size() < size){System.err.println("Operation.checkStack Stack too small{"
                                             +"Operation="    +Operation                +"\t"
                                             +"required size="+size                     +"\t"
                                             +"stack="        +Utilities.toString(stack)+"\t"
                                             +"stack size="   +stack.size()             +"}");
                           System.exit(1);
  }
 }

 private double getDoubleField(String value)
 {
  if (value         ==null     )return Double.NaN;
  if (value.length()==0        )return Double.NaN;
  if (Format.equals("datetime"))return Utilities.getDate(value).getTime();
  return Utilities.parseDouble(value);
 }

 public String getOperation(){return Operation;}

 private String getStringField(String value)
 {
  if (value == null)return "";
  return value;
 }

 public void initialize()
 {
  if (Operation != null)Operation = Operation.toLowerCase();
  if (Name != null)
     {
      OperationFieldNo = new int[Name.length];
      for (int i=0;i<OperationFieldNo.length;i++)OperationFieldNo[i] = -1;
     }
 }

 public String[] noteColumnNames(String[] columnNames)
 {
  if (Debug)System.err.println("Operation.noteColumnNames{"
                              +"columnNames="+Utilities.toString(columnNames)+"\t"
                              +"Name="       +Utilities.toString(Name)       +"\t"
                              +"Operation="  +Operation                      +"}");
  if (Name == null)return columnNames;
  for (int i = 0; i < columnNames.length; i++)
      for (int j=0;j<Name.length;j++)
           if (columnNames[i].equals(Name[j]))OperationFieldNo[j] = i;

  String[] result = columnNames;
  for (int i=0;i<Name.length;i++)
       if (OperationFieldNo[i] == -1)
          {                                        
//           if (!Operation.equals("pop"))           ***********  It is OK for a name to first occur in an operation ************* 
//              {
//               System.err.println("Operation.noteColumnNames Name not found{"
//                                 +"Name="+Name[i]+"}");
//               System.exit(1);
//              }
           OperationFieldNo[i] = result.length;
           result = addStringToArray(result,Name[i]);
          }
  if (Debug)System.err.println("Operation.noteColumnNames{"
                              +"columnNames="       +Utilities.toString(columnNames     )+"\t"
                              +"columnNames.length="+columnNames.length                  +"\t"
                              +"Operation="         +Operation                           +"\t"
                              +"OperationFieldNo="  +Utilities.toString(OperationFieldNo)+"\t"
                              +"result="            +Utilities.toString(result          )+"\t"
                              +"result.length="     +result.length                       +"}");
  return result;
 }

 public void noteNewValues(String[] values)
 {
  if (Name==null)return;
  if (LagValues.size() >= LagN)LagValues.remove(0);
  String[] lagValues = new String[Name.length];
  for (int  i = 0; i < Name.length; i++)
       lagValues[i] = values[OperationFieldNo[i]];
  LagValues.add(lagValues);
  if (Debug)System.err.println("Operation.noteNewValues{"
                              +"Name="     +Utilities.toString(Name)     +"\t"
                              +"LagValues="+Utilities.toString(LagValues)+"\t"
                              +"values="   +Utilities.toString(values)   +"}");
 }

 public Operation(            ){                }
 public Operation(String value){Operation=value;}

 public boolean process(String[] values,ArrayList<String> stack)
// throws MyIOException
 {
  if (Debug)System.err.println("Operation.process entry{"
                              +"Name="     +Utilities.toString(Name )+"\t"
                              +"Value="    +Utilities.toString(Value)+"\t"
                              +"Operation="+Operation                +"\t"
                              +"stack="    +Utilities.toString(stack)+"}");
  boolean result=process_(values,stack);
  if (Debug)System.err.println("Operation.process exit{"
                              +"Name="     +Utilities.toString(Name) +"\t"
                              +"Operation="+Operation                +"\t"
                              +"result="   +result                   +"\t"
                              +"stack="    +Utilities.toString(stack)+"}");
  return result;
 }

 public boolean process_(String[] values,ArrayList<String> stack)
 // throws MyIOException
 {
  if (Debug)System.err.println("Operation.process_ entry{"
                              +"nest="  +Utilities.nest()          +"\t"
                              +"this="  +this                      +"\t"
                              +"stack=" +Utilities.toString(stack )+"\t"
                              +"values="+Utilities.toString(values)+"}");
  if (Operation.equals("pop"))
     {
//      if (Name == null || Name.length == 0){System.err.println("Operation.process No Name for pop operation{"
//                                                              +"stack="+Utilities.toString(stack)+"}");
//                                            System.exit(1);}

      if (Debug)System.err.println("Operation.process_ pop note Value{"
                                  +"Value="+Utilities.toString(Value)+"}");
      if (Value != null)
          for (String value : Value)
              {
               stack.add(0,getStringField(value)); 
               if (Debug)System.err.println("Operation.process_ pop Value{"
                                           +"value="+value+"}");
              }

          // It is OK to just pop the stack.
          if (OperationFieldNo== null || OperationFieldNo.length ==0)
              stack.remove(0);

          if (OperationFieldNo != null)
              for (int i = 0; i < OperationFieldNo.length; i++)
                  {
                   if (OperationFieldNo[i] >= values.length)
                      {
                       System.err.println("Operation.process_ pop values too short for fieldno{"
                                         +"i="               +i                                   +"\t"
                                         +"nest="            +Utilities.nest()                    +"\t"
                                         +"OperationFieldNo="+Utilities.toString(OperationFieldNo)+"\t"
                                         +"this="            +this                                +"\t"
                                         +"stack="           +Utilities.toString(stack )          +"\t"
                                         +"values="          +Utilities.toString(values)          +"}");
                       System.exit(1);
                      }
                   if (stack.size() == 0)
                      {
                       System.err.println("Operation.process_ pop stack is empty{"
                                         +"i="               +i                                   +"\t"
                                         +"nest="            +Utilities.nest()                    +"\t"
                                         +"OperationFieldNo="+Utilities.toString(OperationFieldNo)+"\t"
                                         +"this="            +this                                +"\t"
                                         +"stack="           +Utilities.toString(stack )          +"\t"
                                         +"values="          +Utilities.toString(values)          +"}");
                       System.exit(1);
                      }
                   values[OperationFieldNo[i]] = stack.get(0);
                   stack.remove(0);
                  }
      if (Debug)System.err.println("Operation.process_ note values after pop{"
                                  +"Operation="+Operation                 +"\t"
                                  +"stack="    +Utilities.toString(stack) +"\t"
                                  +"values="   +Utilities.toString(values)+"}");
      return true;
     }

  if (Name  != null)
      if (Lag)
         {
          String[] lagValues = null;
          if (LagValues.size() >= LagN)lagValues = LagValues.get(0);
          else                         lagValues = new String[Name.length];
          for (String value : lagValues)stack.add(0,getStringField(value));
          if (Debug)System.err.println("Operation.process_{"
                                      +"Name="     +Utilities.toString(Name)     +"\t"
                                      +"LagValues="+Utilities.toString(LagValues)+"\t"
                                      +"lagValues="+Utilities.toString(lagValues)+"}");
         }
      else
         {
          for (int fieldNo : OperationFieldNo)
              {
               if (fieldNo >= values.length)
                  {
                   System.err.println("Operation.process_ Internal Error. fieldNo too big{"
                                     +"fieldNo="         +fieldNo                             +"\t"
                                     +"Operation="       +Operation                           +"\t"
                                     +"OperationFieldNo="+Utilities.toString(OperationFieldNo)+"\t" 
                                     +"Name="            +Utilities.toString(Name            )+"\t" 
                                     +"Value="           +Utilities.toString(Value           )+"\t" 
                                     +"values="          +Utilities.toString(values          )+"}");
                   System.exit(1);
                  }
               stack.add(0,getStringField(values[fieldNo]));
              }
         }

  if (Value != null)
      for (String value : Value)
          {
           if (Debug)System.err.println("Operation.process_ note Value{"
                                       +"Value="+value+"}");
           stack.add(0, getStringField(value));
          }

  if (Debug)System.err.println("Operation.process_ note stack{"
                              +"Operation="+Operation                 +"\t"
                              +"stack="    +Utilities.toString(stack) +"\t"
                              +"values="   +Utilities.toString(values)+"}");

  if (Operation.equals("push"))
     {
      if (Name == null && Value == null)stack.add(0,stack.get(0));
      return true;
     }

  if (Operation.equals("add"     ) ||
      Operation.equals("floor"   ) ||
      Operation.equals("subtract") ||
      Operation.equals("multiply") ||
      Operation.equals("divide"  ))
     {
      checkStack(stack,2);
      if (Debug)System.err.println("Operation.process_ note stack1{"
                                  +"stack="+Utilities.toString(stack)+"}");
      double value2 = getDoubleField(stack.get(0));
      double value1 = getDoubleField(stack.get(1));
      double result       = 0;
      if (Utilities.isNaN(value1) ||
          Utilities.isNaN(value2))result = Double.NaN;
      else
          {
           if (Operation.equals("add"     ))result = value1+value2                   ;else
           if (Operation.equals("floor"   ))
              {
               if (value2 == 0)value2 = 1;
               result = Math.floor(value1/value2)*value2;
              }
           else
           if (Operation.equals("subtract"))result = value1-value2                   ;else
           if (Operation.equals("multiply"))result = value1*value2                   ;else
           if (Operation.equals("divide"  ))
              {
               if (value2 == 0)
                  {
    //               System.err.println("Operation.process_ divide by zero{"
    //                                 +"Name="     +Utilities.toString(Name )+"\t"
    //                                 +"Operation="+Operation                +"\t"
    //                                 +"Value="    +Utilities.toString(Value)+"\t"
    //                                 +"value1="   +value1                   +"\t"
    //                                 +"value2="   +value2                   +"}");
                   result = Double.NaN;
                  }
               else
                  {
                   result = value1/value2;
                   if (Debug)System.err.println("Operation.process_ divide debug{"
                                               +"value1="+value1+"\t"
                                               +"value2="+value2+"}");
                  }
              }
          }
       stack.remove(0);
       stack.set(0,Utilities.toString(result));
       if (Debug)System.err.println("Operation.process_ note stack after math{"
                                   +"Operation="+Operation                 +"\t"
                                   +"result="   +result                    +"\t"
                                   +"stack="    +Utilities.toString(stack) +"\t"
                                   +"values="   +Utilities.toString(values)+"}");
       return true;
      }

  if (Operation.equals("and"               ) ||
      Operation.equals("concatenate"       ) ||
      Operation.equals("contains"          ) ||
      Operation.equals("endswith"          ) ||
      Operation.equals("endwith"           ) ||
      Operation.equals("indexof"           ) ||
      Operation.equals("lastindexof"       ) ||
      Operation.equals("numberoffields"    ) ||
      Operation.equals("or"                ) ||
      Operation.equals("startswith"        ) ||
      Operation.equals("startwith"         ))
     {
      checkStack(stack,2);
      String value2 = getStringField(stack.get(0));
      String value1 = getStringField(stack.get(1));
      String result = null;

      if (Operation.equals("and"           ))result = value1    .equals("true") && value2.equals("true")?"true":"false";else
      if (Operation.equals("concatenate"   ))result = value1+value2                                                    ;else 
      if (Operation.equals("contains"      ))result = value1    .contains(value2)                       ?"true":"false";else 
      if (Operation.equals("endswith"      ))result = value1    .endsWith(value2)                       ?"true":"false";else
      if (Operation.equals("endwith"       ))result = Utilities .endWith(value1,value2)                                ;else
      if (Operation.equals("indexof"       ))result = Utilities.toString(value1.indexOf(value2))                       ;else
      if (Operation.equals("lastindexof"   ))result = Utilities.toString(value1.lastIndexOf(value2))                   ;else
      if (Operation.equals("numberoffields"))result = Utilities.toString(Utilities.split(value1,value2).length)        ;else
      if (Operation.equals("or"            ))result = value1    .equals("true") || value2.equals("true")?"true":"false";else
      if (Operation.equals("startswith"    ))result = value1    .startsWith(value2)                     ?"true":"false";else
      if (Operation.equals("startwith"     ))result = Utilities .startWith(value1,value2)                              ;
      stack.remove(0);
      stack.set   (0,result);
      if (Debug)System.err.println("Operation.process_ note result{"
                                  +"Operation="+Operation+"\t"
                                  +"result="   +result   +"}");
      return true;
     }

  if (Operation.equals("equals"            ) ||
      Operation.equals("greaterequal"      ))
     {
      checkStack(stack,2);
      String result = null;
      if (Format.equals("string"))
         {
          String value2 = getStringField(stack.get(0));
          String value1 = getStringField(stack.get(1));
          if (Operation.equals("equals"        ))result = value1.equals   (value2)     ?"true":"false";else
          if (Operation.equals("greaterequal"  ))result = value1.compareTo(value2) >= 0?"true":"false";
         }
      if (Format.equals("datetime") ||
          Format.equals("number"  ))
         {
          double value2 = getDoubleField(stack.get(0));
          double value1 = getDoubleField(stack.get(1));

          if (Operation.equals("equals"        ))result = Utilities.compareDouble(value1,value2)==0?"true":"false";else
          if (Operation.equals("greaterequal"  ))result = Utilities.compareDouble(value1,value2)>=0?"true":"false";
         }
      stack.remove(0);
      stack.set   (0,result);
      if (Debug)System.err.println("Operation.process_ note result{"
                                  +"Operation="+Operation+"\t"
                                  +"result="   +result   +"}");
      return true;
     }

  if (Operation.equals("dequote"           ) ||
      Operation.equals("hextodec"          ) ||
      Operation.equals("isinteger"         ) ||
      Operation.equals("length"            ) ||
      Operation.equals("lowcase"           ) ||
      Operation.equals("not"               ) ||
      Operation.equals("trim"              ) ||
      Operation.equals("upcase"            ) ||
      Operation.equals("upcasefirst"       ) ||
      Operation.equals("upcasefirstofwords"))
     {
      checkStack(stack,1);
      String value1 = stack.get(0);if (value1==null)value1="";
      String result = null;
      if (Operation.equals("dequote"           ))
         {
          try {result = Utilities.dequote(value1);}
          catch (MyParseException e){System.err.println("Operation.process_ dequote MyParseException"
                                                       +"e="+Utilities.toString(e)+"}");
                                     result = value1;}
         }
      else
      if (Operation.equals("hextodec"          ))result = Utilities.hexToDec(value1)                       ;else
      if (Operation.equals("isinteger"         ))result = Utilities.isNumericInteger(value1)?"true":"false";else
      if (Operation.equals("length"            ))result = Utilities.toString(value1.length())              ;else
      if (Operation.equals("lowcase"           ))result = value1   .toLowerCase()                          ;else
      if (Operation.equals("not"               ))result = value1   .equals("true")?"false":"true"          ;else
      if (Operation.equals("trim"              ))result = value1   .trim()                                 ;else 
      if (Operation.equals("upcase"            ))result = value1   .toUpperCase()                          ;else 
      if (Operation.equals("upcasefirst"       ))result = Utilities.upcaseFirst(value1)                    ;else 
      if (Operation.equals("upcasefirstofwords"))result = Utilities.upcaseFirstOfWords(value1)             ;
      if (Debug)System.err.println("Operation.process_ note result{"
                                  +"Operation="+Operation+"\t"
                                  +"result="   +result   +"}");
      stack.set(0,result);
      return true;
     }

  if (Operation.equals("set"))
     {
      return true;
     }

  if (Operation.equals("padleftzero"))
     {
      String result = null;
      checkStack(stack,2);
      String value1 = stack.get(1);if (value1==null)value1="";
      String value2 = stack.get(0);if (value2==null)value2="";
      if (!Utilities.isNumericInteger(value2))
         {
          System.err.println("Operation.process_ padleft length (2nd parm) is not an integer{"
                            +"stack="+Utilities.toString(stack)+"}");
          System.exit(1);
         }
      int value2Int = Utilities.parseInt(value2);
      result        = Utilities.padLeftZero(value1,value2Int);
      stack.remove(0);
      stack.set   (0,result);
      return true;
     }

  if (Operation.equals("replaceall"  ) ||
      Operation.equals("replacefirst") ||
      Operation.equals("replacelast" ) ||
      Operation.equals("substring"   ))
     {
      checkStack(stack,3);
      String value1 = stack.get(2);if (value1==null)value1="";
      String value2 = stack.get(1);if (value2==null)value2="";
      String value3 = stack.get(0);if (value3==null)value3="";
      String result = null;
      if (Operation == null ||
          value1    == null ||
          value2    == null ||
          value3    == null)  
         {
          System.err.println("Operation.process_ null pointer{"
                            +"Operation="+Operation+"\t"
                            +"value1="   +value1   +"\t"
                            +"value2="   +value2   +"\t"
                            +"value3="   +value3   +"}");
          System.exit(1);
         }
      if (Operation.equals("replaceall"  ))result = value1   .replace     (value2, value3      );else
      if (Operation.equals("replacefirst"))result = Utilities.replaceFirst(value1,value2,value3);else
      if (Operation.equals("replacelast" ))result = Utilities.replaceLast (value1,value2,value3);else

      if (Operation.equals("substring"   ))
         {
          if (Debug)System.err.println("Operation.process_ substring{"
                                      +"stack=" +Utilities.toString(stack)+"\t"
                                      +"value1="+value1                   +"\t"
                                      +"value2="+value2                   +"\t"
                                      +"value3="+value3                   +"}");
          int begin = Utilities.parseInt(value2);
          int end   = Utilities.parseInt(value3);
          int len   = value1.length();
          if (begin > len)begin = len;
          if (end   > len)end   = len;
          result = value1.substring(begin,end);
         }
      stack.remove(0);
      stack.remove(0);
      stack.set   (0,result);
      return true;
     }
      
  if (Operation.equals("parsecitystateabbrzip") ||
      Operation.equals("parsenumberstreet"    ))
     {
      if (Debug)System.err.println("Operation.process_ parsecitystateabbrzip note stack at start{"
                                  +"stack="+Utilities.toString(stack)+"}");
      checkStack(stack,1);
      String value1 = stack.get(0);
      stack.remove(0);
      if (Operation.equals("parsecitystateabbrzip"))
         {
          String[] strings = Utilities .parseCityStateAbbrZip(value1);
          if (strings == null)
             {
              for (int i=0;i<3;i++)stack.add(0,"");
              return true;
             }
          for (int i=0;i<strings.length;i++)stack.add(0,strings[i]);
          for (int i=strings.length;i<3;i++)stack.add(0,"");
          if (Debug)System.err.println("Operation.process_ parsecitystateabbrzip note stack{"
                                      +"stack="+Utilities.toString(stack)+"}");
         }else
      if (Operation.equals("parsenumberstreet"))
          {
           String[] strings = Utilities .parseNumberStreet(value1);
           if (strings == null)
              {
               for (int i=0;i<2;i++)stack.add(0,"");
               return true;
              }
           for (int i=0;i<strings.length;i++)stack.add(0,strings[i]);
           for (int i=strings.length;i<2;i++)stack.add(0,"");
          }
      return true;
     }

  if (Operation.equals("split"))
     {
      checkStack(stack,2);
      String value2 = stack.get(0);if (value2==null)value2="";
      String value1 = stack.get(1);if (value1==null)value1="";
      stack.remove(0);
      stack.remove(0);
      if (Operation.equals("split"))
         {
          String[] strings = Utilities.split(value1,value2);
          if (strings == null)
             {
              for (int i=0;i<2;i++)stack.add(0,"");
              return true;
             }
          for (int i=0;i<strings.length;i++)
              {
               stack.add(0,strings[i]);
               if (Debug)System.err.println("Operation.process_ split after add{"
                                            +"i="         +i                        +"\t"
                                            +"strings[i]="+strings[i]               +"\t"
                                            +"stack="     +Utilities.toString(stack)+"}");
              }
          for (int i=strings.length;i<2;i++)stack.add(0,"");
         }
      if (Debug)System.err.println("Operation.process_ split result{"
                                  +"stack="+Utilities.toString(stack)+"}");
      return true;
     }

  System.err.println("Operation.process_ unknown operation{"
             +"Name="    +Utilities.toString(Name )+"\t"
             +"Value="   +Utilities.toString(Value)+"\t"
             +"Operation="+Operation+"}");
  System.exit(1);
  return false;
 }

 public void reset()
 {
  if (LagReset)LagValues.clear();
 }

 public String  toString()
 {
  return "{Operation:"
         +"Debug="           +Debug                               +"\t"
         +"Format="          +Format                              +"\t"
         +"Lag="             +Lag                                 +"\t"
         +"LagValues="       +Utilities.toString(LagValues       )+"\t"
         +"Name="            +Utilities.toString(Name            )+"\t"
         +"Operation="       +Operation                           +"\t"
         +"OperationFieldNo="+Utilities.toString(OperationFieldNo)+"\t"
         +"Value="           +Utilities.toString(Value           )+"}";
 }
}

