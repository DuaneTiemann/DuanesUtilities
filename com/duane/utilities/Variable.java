package com.duane.utilities;

import java.util.Date;
import java.lang.String;
import java.lang.StringBuffer;

@ParmParserParm
public class Variable implements Initializable
{
                                                                           protected Date    DateValue   = null;
 @ParmParserParm("true|(false)"                                          ) protected boolean Debug       = false;
                                                                           protected double  DoubleValue = Double.NaN;
 @ParmParserParm("number|(string)"                                       ) public    String  Format      = "string";
                                                                           private   boolean IsValid     = false;
 @ParmParserParm("Column Name"                                           ) protected String  Name        = null;
                                                                           protected int     NameField   = -1;
 @ParmParserParm("Number of places in fraction for formatted numbers(-1)") protected int     Places      = -1;
                                                                           protected String  StringValue = null;
 public int compare(String value)
 {
  if (!IsValid)return -1;

  if (value == null && StringValue == null)return  0;
  if (value == null                       )return -1;
  if (                 StringValue == null)return  1;

  if (Format.equals("string"  ))return StringValue.compareTo(value);
  if (Format.equals("datetime"))return DateValue  .compareTo(Utilities.getDate(value));
  if (Format.equals("number"  ))
     {
      double doubleValue = Utilities.parseDouble(value);
      if (Utilities.isNaN(DoubleValue) && Utilities.isNaN(doubleValue))return  0;
      if (Utilities.isNaN(DoubleValue)                                )return -1; // NaN is considered to be less than a number
      if (                                Utilities.isNaN(doubleValue))return  1;
      if (DoubleValue < doubleValue)return -1;
      if (DoubleValue > doubleValue)return  1;
      return 0;
     }
  Utilities.fatalError("Variable.compare unknown format{"
                      +"Format="+Format+"}");
  return 0; 
 }

 public int compare(String[] values)
 {
  if (values == null)
      if (StringValue == null)return 0;
      else                    return 1;

  if (NameField >= values.length)
      Utilities.error("Variable.compare values too short for NameField{"
                     +"nest="  +Utilities.nest()          +"\t"
                     +"this="  +this                      +"\t"
                     +"values="+Utilities.toString(values)+"}");

  return compare(values[NameField]);
 }

 public boolean equals(String value)
 {
  if (value       == null && StringValue == null)return false;
  if (value       == null)return false;
  if (StringValue == null)return false;
  return value.equals(StringValue);
 }

 public boolean equals(String[] oldValues,String value)
 {
  if (oldValues == null)return false;
  if (value     == null)return false;
  if (NameField >= oldValues.length)
     {
      Utilities.error("Variable.equals NameField too large{"
                     +"nest="            +Utilities.nest()+"\t"
                     +"oldValues.length="+oldValues.length+"\t"
                     +"this="            +this            +"}");
     }
  if (oldValues[NameField].equals(value)) return true;
  if (Format.equals("string"  ))return false;
  if (Format.equals("datetime"))return Utilities.getDate    (oldValues[NameField]).equals(Utilities.getDate    (value));
  if (Format.equals("number"  ))return Utilities.parseDouble(oldValues[NameField]) ==     Utilities.parseDouble(value) ;
  Utilities.fatalError("Variable.equals unknown format{"
                    +"Format="+Format+"\t"
                    +"Name="  +Name  +"}");
  return false;
 }

 public boolean equals(String[] oldValues,String[] newValues)
 {
  if (oldValues == null && newValues == null             )return true;
  if (oldValues != null && newValues == null             )return false;
  if (oldValues == null && newValues != null             )return false;
  if (NameField >= oldValues.length || NameField >= newValues.length)
     {
      Utilities.fatalError("Variable.equals NameField too large{"
                        +"nest="            +Utilities.nest()             +"\t"
                        +"newValues="       +Utilities.toString(newValues)+"\t"
                        +"newValues.length="+newValues.length             +"\t"
                        +"oldValues="       +Utilities.toString(oldValues)+"\t"
                        +"oldValues.length="+oldValues.length             +"\t"
                        +"this="            +this                         +"}");
      System.exit(1);
     }
  if (oldValues[NameField] == null && newValues[NameField] == null)return true;
  if (oldValues[NameField] == null && newValues[NameField] != null)return false;
  if (oldValues[NameField] != null && newValues[NameField] == null)return false;
  if (oldValues[NameField].equals(newValues[NameField])) return true;
  if (IsValid && newValues[NameField].equals(StringValue))return compare(oldValues[NameField]) == 0;
  if (Format.equals("datetime")                          )return Utilities.getDate    (oldValues[NameField]).equals(Utilities.getDate    (newValues[NameField]));
  if (Format.equals("number"  )                          )return Utilities.parseDouble(oldValues[NameField]) ==     Utilities.parseDouble(newValues[NameField]) ;
  if (Format.equals("string"  )                          )return false;
  Utilities.fatalError("Variable.equals unknown format{"
                    +"Format="+Format+"\t"
                    +"Name="  +Name  +"}");
  return false;
 }

 public Date    getDateValue (String[] values)
 {
  noteFields_(values);
  return DateValue;
 }

 public Date    getDateValue  (){return DateValue  ;}
 public double  getDoubleValue(){return DoubleValue;}
 public double  getDoubleValue(String[] values){return getDoubleValue(null,-1,values);}
 public double  getDoubleValue(String fileName, long lineNo, String[] values)
 {
  setValue_(fileName,lineNo,values); // do not call child class method
  return DoubleValue;
 }

 public int     getFieldNo   (){return NameField   ;}
 public String  getFormat    (){return Format      ;} 
 public String  getName      (){return Name        ;} 

 public static String getNames(String name, Variable[] vars)
 {
  if (vars        == null)return "";
  if (vars.length == 0   )return "";
  StringBuffer sb = new StringBuffer();

  sb.append("\t").append(name).append("=");
  if (vars.length > 1)sb.append("{");
  boolean first = true;
  for (Variable var : vars)
      {
       if (first)first = false;
       else sb.append("\t");
       sb.append(var.getName());
      }
  if (vars.length > 1)sb.append("}");
  return sb.toString();
 }


 public String  getValue     (){return StringValue ;}

 public String getValue(String[] values)
 {
  noteFields_(values);
  return StringValue;
 }

 public String getValue(String fileName, long lineNo, String[] values)
 {
  setValue_(fileName,lineNo,values);
  return StringValue ;
 }

 public static String getValues(String name, Variable[] vars)
 {
  return name+"="+getValues(vars);
 }

 public static String getValues(Variable[] vars)
 {
  if (vars        == null)return "";
  if (vars.length == 0   )return "";
  StringBuffer sb = new StringBuffer();

  if (vars.length > 1)sb.append("{");
  boolean first = true;
  for (Variable var : vars)
      {
       if (first)first = false;
       else sb.append("\t");
       sb.append(var.getValue());
      }
  if (vars.length > 1)sb.append("}");
  return sb.toString();
 }

 public void    initialize   (){}
 public boolean isValid      (){return IsValid     ;}

 public String[] noteColumnNames(String[] columnNames)
 {
  if (Debug)Utilities.debug("Variable.noteColumnNames{"
                           +"columnNames="+Utilities.toString(columnNames)+"\t"
                           +"Name="       +Name                           +"\t"
                           +"Nest="       +Utilities.nest()               +"}");

  if (Name == null)return columnNames;

  for (int i = 0; i < columnNames.length; i++)
   {
    String columnName = columnNames[i];
    if (columnName.equals(Name))NameField  = i;
   }

  if (NameField  == -1){NameField  = columnNames.length;columnNames = Utilities.addStringToArray(columnNames,Name );}
  return columnNames;
 }

 public void noteFields(String[] values){noteFields_(values);}  

 private void noteFields_(String[] values) // needed to ensure we can call this method instead of child method.
 {
  if (values    == null)return;
  if (Name      == null)return;
  if (NameField == -1  )return;
  if (NameField >= values.length)
     {
      Utilities.error("Variable.noteFields NameField too large{"
                        +"Name="     +Name                      +"\t"
                        +"NameField="+NameField                 +"\t"
                        +"nest="     +Utilities.nest()          +"\t"
                        +"values="   +Utilities.toString(values)+"}");
      return;
     }
  setValue(values[NameField]);
 }

 public void resetValue()
 {
  IsValid = false;
 }

 public void setDebug(boolean debug){Debug=debug;}
 public void setFormat(String format)
 {
  Format = format;
  if (Format.equals("string"  ))return;
  if (Format.equals("datetime"))return;
  if (Format.equals("number"  ))return;
  Utilities.fatalError("Variable.setValue unknown format{"
                      +"Format="+Format+"}");
 }

 public void setValue(long value)
 {
  StringValue = Utilities.toString(value);
  if (Format.equals("string"  ))return;
  if (Format.equals("datetime")){DateValue   = Utilities.getDate(value);return;}
  if (Format.equals("number"  )){DoubleValue = value                   ;return;}
  Utilities.fatalError("Variable.setValue unknown format{"
                      +"Format="+Format+"}");
 }

 public void setValue(double value)
 {
  StringValue = Utilities.toString(value);
  if (Format.equals("string"  ))return;
  if (Format.equals("datetime")){DateValue   = Utilities.getDate((long)value);return;}
  if (Format.equals("number"  )){DoubleValue = value                         ;return;}
  Utilities.fatalError("Variable.setValue unknown format{"
                      +"Format="+Format+"}");
 }

 public void setValue(String value){setValue(null,-1,value);}
 public void setValue(String fileName, long lineNo, String value){setValue_(fileName,lineNo,value);}
 public void setValue_(String fileName, long lineNo, String value)
 {
  IsValid     = true;
  StringValue = value;
  if (Format.equals("string"  ))return;
  if (Format.equals("datetime"))
     {
      if (value == null)
         {
          DateValue = null;
          return;
         }
      DateValue   = Utilities.isGetDate(value);
      if (DateValue == null)
          DateValue   = Utilities.isGetTime(value);
      if (DateValue == null)
         {
          Utilities.fatalError("Variable.setValue invalid datetime format{"
                              +"fileName="+fileName+"\t"
                              +"Format="  +Format  +"\t"
                              +"lineNo="  +lineNo  +"\t"
                              +"value="   +value   +"}");
         }
      DoubleValue=DateValue.getTime();
      return;
     }
  if (Format.equals("number"  ))
     {
      if (value          == null ||
          value.length() == 0    ||
          value.equals("."))
         {
          DoubleValue = Double.NaN;
          return;
         }
      DoubleValue = Utilities.parseDouble(value);
      return;
     }
  Utilities.fatalError("Variable.setValue unknown format{"
                      +"fileName="+fileName+"\t"
                      +"Format="  +Format  +"\t"
                      +"lineNo="  +lineNo  +"\t"
                      +"value="   +value   +"}");
 }

 public  void setValue (String[] values){setValue(null,-1,values);}
 public  void setValue (String fileName, long lineNo, String[] values){setValue_(fileName,lineNo,values);}
 private void setValue_(String fileName, long lineNo, String[] values)
 {
  if (values == null)
     {
      StringValue = null;
      return;
     }
  if (NameField >= values.length)
     {
      Utilities.error("Variable.setValue_ NameField too large{"
                     +"fileName=" +fileName +"\t"
                     +"lineNo="   +lineNo   +"\t"
                     +"Name="     +Name     +"\t"
                     +"NameField="+NameField+"}");
      return;
     }
  if (NameField == -1)
     {
      Utilities.fatalError("Variable.setValue_ NameField is -1{"
                          +"fileName=" +fileName +"\t"
                          +"lineNo="   +lineNo   +"\t"
                          +"Name="     +Name     +"\t"
                          +"NameField="+NameField+"}");
     }
  setValue(fileName, lineNo, values[NameField]);
 }

 public void setValue(String[] values, double value)
 {
  if (Utilities.isNaN(value))
     {
      setValue(values,".");
      return;
     }
  if (Places == -1)setValue(values,Utilities.toString(value));
  else             setValue(values,Utilities.toString(value,Places));
 }

 public void setValue(String[] values, String value)
 {
  if (NameField == -1  )return;
  if (values    == null)return;
  if (NameField >= values.length)
     {
      Utilities.error("Variable.setValue NameField too large{"
                     +"Name="     +Name     +"\t"
                     +"NameField="+NameField+"}");
      return;
     }
  values[NameField]=value;
  if (Debug)
      if (value.equals("-"))Utilities.debug("Variable.setValue note -{"
                                           +"nest="+Utilities.nest()+"}");
 }

//  These don't make sense to me.
//  public void setValue(String[] values, double value)
//  {
//   setValue(value);
//   setValue(values);
//  }
// 
//  public void setValue(String[] values, long value)
//  {
//   setValue(value);
//   setValue(values);
//  }
// 
//  public void setValue(String[] values, String value)
//  {
//   setValue(value);
//   setValue(values);
//  }

 public String toString()
 {
  return "{Variable:"
         +"DateValue="  +DateValue  +"\t"
         +"DoubleValue="+DoubleValue+"\t"
         +"Format="     +Format     +"\t"
         +"IsValid="    +IsValid    +"\t"
         +"Name="       +Name       +"\t"
         +"NameField="  +NameField  +"\t"
         +"StringValue="+StringValue+"}";
 }

 public Variable(){}
 public Variable(String s){Name=s;}
 public Variable(Variable var)
 {
  DateValue   = var.DateValue  ;
  Debug       = var.Debug      ;
  DoubleValue = var.DoubleValue;
  Format      = var.Format     ;
  IsValid     = var.IsValid    ;
  Name        = var.Name       ;
  NameField   = var.NameField  ;
  StringValue = var.StringValue;
 }
}
