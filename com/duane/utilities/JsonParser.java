package com.duane.utilities;

import java.io.StringReader;
import java.lang.Integer;
import java.lang.String;
import java.lang.StringBuffer;
import java.util.ArrayList;
import java.util.Date;

import com.duane.utilities.MyParseException;
import com.duane.utilities.MyStreamTokenizer;
import com.duane.utilities.Utilities;

public class JsonParser
{
 private boolean             Debug      = false;
 private ObjectId            ObjectId   = new ObjectId();
 private static String       Separator  = ":";
 private MyStreamTokenizer   Tokenizer  = null;
 private final static String Version    = "1.0.0";

 private static class ObjectId
 {
  ArrayList<Integer> ObjectId = new ArrayList<Integer>();
  int                Level    = -1;

  public void bumpId()
  {
   ObjectId.set(Level,ObjectId.get(Level)+1);
   if (ObjectId.size() > Level+1)ObjectId.set(Level+1,0);
  }

  public void decreaseLevel()
  {
   Level--;
  }

  public String getId()
  {
   StringBuffer result = new StringBuffer();

   for (int i=0;i<=Level;i++)
       {
        if (i > 0)result.append(Separator);
        result.append(Utilities.toString(ObjectId.get(i)));
       }
   return result.toString();
  }

  public void increaseLevel()
  {
   Level++;
   if (ObjectId.size() <= Level)ObjectId.add(new Integer(0));
  }

  public String toString()
  {
   return "{ObjectId:"
          +"Level="   +Level                       +"\t"
          +"ObjectId="+Utilities.toString(ObjectId)+"}";
  }
 }

 private void getArray(ArrayList<String[]> result, String name)
 throws MyIOException, MyParseException
 {
  if (Debug)Utilities.debug("JsonParser.getArray enter{"
                           +"name="    +name    +"\t"
                           +"ObjectId="+ObjectId+"}");
  StringBuffer value = new StringBuffer();
  while (true)
        {
         String token = Tokenizer.getTokenOrDie();
         if (Debug)Utilities.debug("JsonParser.getArray"
                                  +"token="+token+"}");
         if (token.equals("{"))
            {
             getObject(result,name);
             continue;
            }
         if (token.equals("]"))
            {
             if (value.length()>0)
                 result.add((new String[]{ObjectId.getId(),name,value.toString()}));
             if (Debug)Utilities.debug("JsonParser.getArray return 1");
             return;
            }
         if (token.equals("["))
            {
             getArray(result,name);
             if (Debug)Utilities.debug("JsonParser.getArray return 2");
             continue;
            }
         if (!Tokenizer.isSeparator(token))
              value.append(token);
         else
             if (value.length() > 0)value.append(token);
      }
 }

 private void getObject(ArrayList<String[]> result, String name)
 throws MyIOException, MyParseException
 {
  if (Debug)Utilities.debug("JsonParser.getObject enter{"
                           +"name="    +name    +"\t"
                           +"ObjectId="+ObjectId+"}");
  ObjectId.increaseLevel();
  ObjectId.bumpId();
  while (true)
        {
         String token = Tokenizer.getTokenOrDie();
         if (Debug)Utilities.debug("JsonParser.getObject debug{"
                                  +"token="+token+"}");
         if (Tokenizer.isSeparator(token))continue;
         if (token.equals("}"))
            {
             ObjectId.decreaseLevel();
             if (Debug)Utilities.debug("JsonParser.getObject return 1");
             return;
            }
         String longName = name==null?Utilities.dequote(token):name+Separator+Utilities.dequote(token);
         token = Tokenizer.getTokenOrDie();
         if (Debug)Utilities.debug("JsonParser.getObject{"
                                  +"token="+token+"}");
         if (!token.equals(":"))
            {
             Utilities.error("JsonParser.getObject missing :{"
                             +"lineNo="  +Tokenizer.getCurrentTokenLineNo()      +"\t"
                             +"longName="+longName                               +"\t"
                             +"name="    +name                                   +"\t"
                             +"ObjectId="+ObjectId                               +"\t"
                             +"offset="  +Tokenizer.getCurrentTokenOffsetInLine()+"\t"
                             +"token="   +token                                  +"}");
             throw new MyParseException("JsonParser.getObject missing :");
            }
         getValue(result,longName);
        }
 }

 private void getValue(ArrayList<String[]> result, String name)
 throws MyIOException, MyParseException
 {
  if (Debug)Utilities.debug("JsonParser.getValue enter{"
                           +"name="    +name    +"\t"
                           +"ObjectId="+ObjectId+"}");
  String token = Tokenizer.getTokenOrDie();
  if (Debug)Utilities.debug("JsonParser.getValue debug{"
                           +"name=" +name +"\t"
                           +"token="+token+"}");

  if (token.equals("{"))
     {
      getObject(result,name);
      if (Debug)Utilities.debug("JsonParser.getValue return 1");
      return;
     }
  if (token.equals("["))
     {
      getArray(result,name);
      if (Debug)Utilities.debug("JsonParser.getValue return 2");
      return;
     }
  result.add(new String[]{ObjectId.getId(),name,Utilities.dequote(token)});
  if (Debug)Utilities.debug("JsonParser.getValue return 3");
 }

 public JsonParser(){}

 public String[][] parse(String parm)
 throws MyIOException, MyParseException
 {
  ArrayList<String[]> result = new ArrayList<String[]>();
  Tokenizer = new MyStreamTokenizer(new StringReader(parm), null);
  Tokenizer.setDebug(false);
  Tokenizer.setSeparatorChars(new char[]{','});
  Tokenizer.setSymbolChars   (new char[]{',','[',']','{','}',':'});

  for (String token = Tokenizer.getToken(); token != null; token = Tokenizer.getToken())
      {
       if (Debug)Utilities.debug("JsonParser.parse{"
                                +"token="+token+"}");
       if (Debug)Utilities.debug("JsonParser.parse{"
                                 +"ungettoken="+token+"}");
       Tokenizer.ungetToken();
       getValue(result,null);
      }
  String[][] resultArray = result.toArray(new String[0][0]);
  return resultArray;
 }
}

