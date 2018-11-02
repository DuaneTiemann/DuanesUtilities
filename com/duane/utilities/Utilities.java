package com.duane.utilities;
import  java.io.File;
import  java.io.IOException;
import  java.io.PrintStream;
import  java.lang.Character;
import  java.lang.Exception;
import  java.lang.StackTraceElement;
import  java.lang.Throwable;
import  java.lang.reflect.Array;
import  java.math.BigInteger;
import  java.net.InetAddress;
import  java.net.UnknownHostException;
import  java.text.DecimalFormat;
import  java.text.FieldPosition;
import  java.text.NumberFormat;
import  java.text.ParseException;
import  java.text.SimpleDateFormat;
import  java.util.AbstractCollection;
import  java.util.AbstractMap;
import  java.util.ArrayList;
import  java.util.Collection;
import  java.util.Date;
import  java.util.HashMap;
import  java.util.Map;
import  java.util.Map.Entry;

import  com.duane.utilities.InputTsvFile;
import  com.duane.utilities.MyIOException;
import  com.duane.utilities.MyParseException;
import  com.duane.utilities.MyInputFile;
import  com.duane.utilities.MyOutputFile;
import  com.duane.utilities.OutputTsvFile;
import  com.duane.utilities.StructuredMessageFormatter2;

public class Utilities
{
 public static class LengthException extends Exception
 {
  static final long serialVersionUID = 0; 
 }

 private static boolean          Debug                        =false;
 private static SimpleDateFormat SDF3                         =new SimpleDateFormat("dd-MMM-yy HH:mm:ss+SSS'ms' z");
 private static SimpleDateFormat SDF3dmyhmsz                  =new SimpleDateFormat("dd-MMM-yy HH:mm:ss z"        );
 private static SimpleDateFormat SDF3ymdhmsz                  =new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z"       );
 private static SimpleDateFormat SDFymdhmampm                 =new SimpleDateFormat("yyyy/MM/dd hh:mm a"          );
 private static SimpleDateFormat SDFDate                      =new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"         );
 private static SimpleDateFormat SDFDayDateTime               =new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy"  );
 private static SimpleDateFormat SDFDayDateTime5              =new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"    );
 private static SimpleDateFormat SDFDayDateTimeZ              =new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z"  );
 private static SimpleDateFormat SDF_d_hh_mm_ss_SSS           =new SimpleDateFormat("d HH:mm:ss.SSS"              );
 private static SimpleDateFormat SDF_hh_mm_ss_SSS             =new SimpleDateFormat("HH:mm:ss.SSS"                );
 private static SimpleDateFormat SDFddmmmyyhhmmss             =new SimpleDateFormat("ddMMMyy:HH:mm:ss"            );
 private static SimpleDateFormat SDFdhms                      =new SimpleDateFormat("dd-HH:mm:ss"                 );
 private static SimpleDateFormat SDFhm                        =new SimpleDateFormat("HH:mm"                       );
 private static SimpleDateFormat SDFhma                       =new SimpleDateFormat("hh:mm a"                     );
 private static SimpleDateFormat SDFhms                       =new SimpleDateFormat("HH:mm:ss"                    );
 private static SimpleDateFormat SDFmdy                       =new SimpleDateFormat("MM/dd/yyyy"                  );
 private static SimpleDateFormat SDFmdyhma                    =new SimpleDateFormat("MM/dd/yyyy hh:mm a"          );
 private static SimpleDateFormat SDFmdy2                      =new SimpleDateFormat("MMM dd, yyyy"                );
 private static SimpleDateFormat SDFmdy3                      =new SimpleDateFormat("MMM dd yyyy"                 );
 private static SimpleDateFormat SDFmdy4                      =new SimpleDateFormat("MM/dd/yy"                    );
 private static SimpleDateFormat SDFms                        =new SimpleDateFormat("mm:ss"                       );
 private static SimpleDateFormat SDFymdhm                     =new SimpleDateFormat("yyyy-MM-dd HH:mm"            );
 private static SimpleDateFormat SDFymdhms                    =new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS"     );
 private static SimpleDateFormat SDFymdhms2                   =new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"         );
 private static SimpleDateFormat SDFymdhms3                   =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"         );
 private static SimpleDateFormat SDFymdhms4                   =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS"     );
 private static SimpleDateFormat SDFymdhms5                   =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS z"   );
 private static SimpleDateFormat SDFymdhms6                   =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"     );
 private static SimpleDateFormat SDFymdhms7                   =new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS XXX" );
 private static SimpleDateFormat SDFyyyy_mm_dd                =new SimpleDateFormat("yyyy-MM-dd"                  );
 private static SimpleDateFormat SDFyyyy_mm_dd_hh_mm_ss_SSS_z =new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z"   );
 private static SimpleDateFormat SDFyyyy_mm_dd_hh_mm_ss_z     =new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z"       );
 private static SimpleDateFormat SDFyyyy_mm_dd_slash          =new SimpleDateFormat("yyyy/MM/dd"                  );
 private static SimpleDateFormat SDFyyyy_mm_dd_hh_mm_ss_SSS_z2=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z"   );
 private static SimpleDateFormat SDFyyyymmddThhmmssSSS        =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"   );
 private static SimpleDateFormat SDFyyyymmddThhmmssSSSZ       =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"  );
 private static SimpleDateFormat SDFyyyymmddThhmmssSSSz       =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz"  );
 private static SimpleDateFormat SDFyyyymmddhhmmssz           =new SimpleDateFormat("yyyyMMddHHmmssz"             );
 private static StringBuffer     Spaces                       =new StringBuffer();
 private static StringBuffer     Zeros                        =new StringBuffer();

// public static <E> E[] addToArray(E[] array, E e)
// {
//  E[] result = new E[array.length+2];
//  for (int i=0;i<array.length;i++)result[i] = array[i];
//  result[array.length] = e;
//  return result;
// }

 public static String[] addStringToArray(String[] array,String s)
 {
  String[] result = new String[array.length+1];
  for (int i=0;i<array.length;i++)result[i] = array[i];
  result[array.length] = s;
  return result;
 }

 public static String bytesToHex(byte[] bytes)
 {
  if (bytes == null)return null;
  StringBuffer result = new StringBuffer();
  for (int i=0;i<bytes.length;i++)
      {
       char h1 = hexChars[(bytes[i] >> 4) & 0x0f];
       char h2 = hexChars[ bytes[i]       & 0x0f];
       result.append(h1).append(h2);
      }
  return result.toString();
 }

 public static int bytesToInt(byte[] bytes){return bytesToInt(bytes,true);}
 public static int bytesToInt(byte[] bytes, boolean bigEndian)
 {
  if (bytes.length !=4)fatalError("bytesToInt byte array length != 4{"
                                 +"bytes.length="+bytes.length+"\t"
                                 +"nest="        +nest()      +"}");
  byte[] orderedBytes = new byte[4];
  if (bigEndian)orderedBytes = bytes;
  else          for (int i=0;i<4;i++)orderedBytes[i] = bytes[3-i];

//  System.err.println("Utilities.bytesToInt{"
//                    +"orderedBytes="+toString(orderedBytes)+"}");
  if ((orderedBytes[0] & 0x80) != 0)return -(((~orderedBytes[0] & 0xff) << 24) // for negative number
                                            +((~orderedBytes[1] & 0xff) << 16)
                                            +((~orderedBytes[2] & 0xff) <<  8)
                                            +((~orderedBytes[3] & 0xff)      )    
                                            +1);
//  System.err.println("Utilities.bytesToInt 2{"
//                    +"orderedBytes[3]"+(orderedBytes[3] & 0xff)+"}");
 return ((orderedBytes[0] & 0xff) << 24)
       +((orderedBytes[1] & 0xff) << 16)
       +((orderedBytes[2] & 0xff) <<  8)
       +((orderedBytes[3] & 0xff)      );          
 }

 public static long bytesToLong(byte[] bytes){return bytesToLong(bytes,true);}
 public static long bytesToLong(byte[] bytes, boolean bigEndian)
 {
  if (bytes.length !=8)fatalError("bytesToInt byte array length != 8{"
                                 +"bytes.length="+bytes.length+"\t"
                                 +"nest="        +nest()      +"}");
  byte[] orderedBytes = new byte[8];
  if (bigEndian)orderedBytes = bytes;
  else          for (int i=0;i<8;i++)orderedBytes[i] = bytes[7-i];

  if ((orderedBytes[0] & 0x80) != 0)return -(((~orderedBytes[0] & 0xff) << 56) // for negative number
                                            +((~orderedBytes[1] & 0xff) << 48)
                                            +((~orderedBytes[2] & 0xff) << 40)
                                            +((~orderedBytes[3] & 0xff) << 32)    
                                            +((~orderedBytes[4] & 0xff) << 24) // for negative number
                                            +((~orderedBytes[5] & 0xff) << 16)
                                            +((~orderedBytes[6] & 0xff) <<  8)
                                            +((~orderedBytes[7] & 0xff)      )    
                                            +1);
 return ((orderedBytes[0] & 0xff) << 56)
       +((orderedBytes[1] & 0xff) << 48)
       +((orderedBytes[2] & 0xff) << 40)
       +((orderedBytes[3] & 0xff) << 32)           
       +((orderedBytes[4] & 0xff) << 24)
       +((orderedBytes[5] & 0xff) << 16)
       +((orderedBytes[6] & 0xff) <<  8)
       +((orderedBytes[7] & 0xff)      );          
 }

 public static String[] clone(String[] s)
 {
  if (s == null)return null;
  String result[] = new String[s.length];
  for (int i=0;i<result.length;i++)result[i]=s[i];
  return result;
 }

 public static String commas(String s)
 {
  int    pos           = s.indexOf(".");
  String fraction      = pos==-1?"":s.substring(pos);
  String integer       = pos==-1?s :s.substring(0,pos);
  int    nCommas       = (integer.length()-(s.startsWith("-")?2:1))/3;
  int    offset        = integer.length()-3;
  StringBuffer integerSB = new StringBuffer(integer);
  for (int i=0;i<nCommas;i++)
      {
       integerSB.insert(offset,',');
       offset -= 3;
      }
  integerSB.append(fraction);
  return integerSB.toString();
 }

 public static int compare(byte[] ba1, byte[] ba2)
 {
  return compare(ba1,ba2,0);
 }

 public static int compare(byte[] ba1, byte[] ba2, int offset)
 {
  if (ba1 == null && ba2 == null)return  0;
  if (ba1 == null && ba2 != null)return -1;
  if (ba1 != null && ba2 == null)return  1;

  int length = ba1.length < ba2.length ? ba1.length : ba2.length;
  if (offset < length)
      for (int i = offset; i < length; i++)
          {
           if (ba1[i] < ba2[i])return -1;
           if (ba1[i] > ba2[i])return  1;
          }
  if (ba1.length < ba2.length)return -1;
  if (ba1.length > ba2.length)return  1;
  return 0;
 }

 public static int compare(double d1,double d2)
 {
  if (Double.isNaN(d1) && Double.isNaN(d2))return  0;
  if (Double.isNaN(d1)                    )return -1;
  if (                    Double.isNaN(d2))return  1;
  if (d1 == d2                            )return  0;
  if (d1 <  d2                            )return -1;
  return 1;
 }

 public static int compare(String[] s1,String[] s2)
 {
  if (s1 == null && s2 != null)return -1;
  if (s1 != null && s2 == null)return  1;
  if (s1 == null && s2 == null)return  0;
  int len = s1.length<s2.length?s1.length:s2.length;  
  for (int i=0;i<len;i++) 
      {
       int result = s1[i].compareTo(s2[i]);
       if (result != 0)return result;
      }
  if (s1.length < s2.length)return -1;
  if (s1.length > s2.length)return  1;
  return 0;
 }

 public static int compareDouble(double d1,double d2){return compare(d1,d2);}

 public static byte[] concatenateArrays(byte[] a, byte[] b)
 {
  if (b==null)b=new byte[0];
  return concatenateArrays(a,b,b.length);
 }

 public static byte[] concatenateArrays(byte[] a, byte[] b, int len)
 {
  if (a  == null    )a   = new byte[0];
  if (b  == null    )b   = new byte[0];
  if (len > b.length)len = b.length;
  byte[] result = new byte[a.length + len];
  for (int i=0;i<a.length;i++)result[i         ]=a[i];
  for (int i=0;i<len     ;i++)result[a.length+i]=b[i];
  return result;
 }

 public static String[] concatenateArrays(String[] a, String b)
 {
  if (a == null)return new String[]{b};
  if (b == null)return a;
  String[] result  = new String[a.length + 1];
  for (int i=0;i<a.length;i++)result[i] = a[i];
  result[a.length] = b;
  return result;
 }

 public static String[] concatenateArrays(String[] a, String[] b)
 {
  if (a == null)return b;
  if (b == null)return a;
  String[] result = new String[a.length+b.length];
  for (int i=0;i<a.length;i++)result[         i] = a[i];
  for (int i=0;i<b.length;i++)result[a.length+i] = b[i];
  return result;
 }

 public static Object[] concatenateArrays(Object[] a, Object[] b)
 {
  if (a == null)return b;
  if (b == null)return a;
  Object[] result = new Object[a.length+b.length];
  for (int i=0;i<a.length;i++)result[         i] = a[i];
  for (int i=0;i<b.length;i++)result[a.length+i] = b[i];
  return result;
 }

 public static String[] concatenateArrays(String[] a, String[] b, String[] c)
 {
  if (a == null)return concatenateArrays(b,c);
  if (b == null)return concatenateArrays(a,c);
  if (c == null)return concatenateArrays(a,b);
  String[] result = new String[a.length+b.length+c.length];
  for (int i=0;i<a.length;i++)result[                  i] = a[i];
  for (int i=0;i<b.length;i++)result[a.length         +i] = b[i];
  for (int i=0;i<c.length;i++)result[a.length+b.length+i] = c[i];
  return result;
 }

 public static void    debug(boolean value){Debug=value;}
 public static boolean debug(){return Debug;}
 public static void debug(String msg)
 {
  log(System.err,"Debug\t"+msg);
 }

 public static byte[] decryptKasaBytes(byte[] bytes)
 {
  byte[] result = new byte[bytes.length];
  byte   prev = -85;
  for (int i=0;i<bytes.length;i++)
      {
       result[i] = (byte)(bytes[i] ^ prev);
       prev      = bytes[i];     // Note this line is the difference between encryption and decryption
      }
  return result;
 }

 public static byte[] encryptKasaBytes(byte[] bytes)
 {
  byte[] result = new byte[bytes.length];
  byte   prev = -85;
  for (int i=0;i<bytes.length;i++)
      {
       result[i] = (byte)(bytes[i] ^ prev);
       prev      = result[i];    // Note this line is the difference between encryption and decryption   
      }
  return result;
 }

 public static String dequote(String source)
 throws MyParseException
 {
//  if (debug())System.err.println("Utilities.dequote note source{"
//                                +"source="+source+"}");
  if (source == null          )return null;
  if (!source.startsWith("\""))return source;
  StringBuffer result = new StringBuffer();
  int pos = 1;
  for (int newPos=source.indexOf('\\',pos);newPos!=-1;pos=newPos+2,newPos=source.indexOf('\\',pos))
      {
//       if (debug())System.err.println("Utilities.dequote{"
//                                     +"newPos="+newPos+"\t"
//                                     +"pos="   +pos   +"}");
       result.append(source.substring(pos, newPos));
       if (newPos >= source.length()-1)throw new MyParseException("Utilities.dequote invalid transparent string. backslash at end of string{"
                                                                 +"source="+source+"}");
       switch (source.charAt(newPos+1))
              {
               case 'b' : result.append('\b');continue;
               case 'f' : result.append('\f');continue;
               case 'r' : result.append('\r');continue;
               case 'n' : result.append('\n');continue;
               case 't' : result.append('\t');continue;
               case 'u' :
                    if (newPos > source.length()-6)throw new MyParseException("Utilities.dequote truncated hex number{"
                                                                             +"offset="+newPos+"\t"
                                                                             +"source="+source+"}");
                    String hexNumber = source.substring(newPos+2,newPos+6);
                    try {result.append((char)Integer.parseInt(source.substring(newPos+2,newPos+6),16)); newPos+=4;continue;}
                    catch (NumberFormatException e){throw new MyParseException("Utilities.dequote invalid hex number{"
                                                                              +"exception="+e.getMessage()+"\t"
                                                                              +"offset="   +newPos        +"\t"
                                                                              +"source="   +source        +"}");}

               case '"' : result.append('"' );continue;
               default  : result.append(source.charAt(newPos+1));continue;
              }
      }
  result.append(source.substring(pos,source.length()-1));
  if (pos >= source.length() || !source.endsWith("\""))throw new MyParseException("Utilities.dequote invalid transparent string. does not end with double quote{"
                                                                                 +"source="+source+"}");
  return result.toString();
 }

 public static String endWith(String s, String e)
 {
  if (s == null    )return e;
  if (e == null    )return s;
  if (s.endsWith(e))return s;
  return s+e;
 }

 public static void fatalError(String msg)
 {
  log(System.err,"Fatal Error\t"+msg);
  System.exit(1);
 }

 public static double floor(double value, double mod)
 {
  if (mod == 0)return value;
  return value - value % mod;
 }

 public static long floor(long value, long mod)
 {
  if (mod == 0)return value;
  return value-value%mod;
 }

 public static String getCompileDate(){return CompileDate.getDate();}

 public static Date getDate(long timeInSecs)
 {
  return new Date(timeInSecs*1000);
 }

 public static Date getDate(String s)
 {
  if (s == null)return null;
  Date result = isGetDate(s);
  if (result == null)result = getTime(s);
  if (result == null)
     {
      if (isNumericInteger(s))return new Date(parseLong(s));
      System.err.println("Utilities.getDate unrecognized date format{"
                 +"s="+s+"}");
      System.exit(1);
     }
  return result;
 }

 /*
    Time is time of day or span of time.
    Note that SimpleDateFormat will not respect daylight savings time so direct calculations are done.
  
    Formats:
     mm:ss.SSS
     hh am/pm
     hh:mm am/pm
     d-hh:mm:ss.SSS 
 */

 public static Date getTime(String s)
 {
  String[] strings = s.trim().split(" +");
  if (strings.length == 2)
     {
      long     hours    = 0;
      long     minutes  = 0;
      double   seconds  = 0;
      String[] strings2 = strings[0].split(":");
      switch (strings2.length)
             {
               case 1:
                    hours = parseLong(strings2[0]);
                    minutes = 0;
                    seconds = 0;
                    break;

               case 2:
                    hours   = parseLong(strings2[0]);
                    minutes = parseLong(strings2[1]);
                    seconds = 0;
                    break;

              case 3:
                   hours   = parseLong(strings2[0]);
                   minutes = parseLong(strings2[1]);
                   seconds = parseLong(strings2[2]);
                   break;

              default:
                   System.err.println("Utilities.getTime unrecognized time format_{"
                                     +"nest="  +nest()+"\t"
                                     +"string="+s     +"}");
                   break;
             }
      String ampm = strings[1].toLowerCase();
      if (ampm.equals("pm") && hours < 12)hours += 12;
      return new Date((long)((hours*3600+minutes*60+seconds)*1000));
     }

  strings = s.trim().split(":");

  if (strings.length == 2)
     {
      long   minutes = parseLong(strings[0]);
      double seconds = parseDouble(strings[1]);
      return new Date((long)((minutes*60+seconds)*1000));
     }

  if (strings.length == 3)
     {
      long   days    = 0;
      String[] dhStrings = strings[0].split("-");
      if (dhStrings.length == 2)
         {
          days       = parseLong  (dhStrings[0]);
          strings[0] = dhStrings[1];
         }
      String[] secAMPMStrings = strings[2].split(" +");
      long pm = 0;
      if (secAMPMStrings.length == 2)
         {
          strings[2] = secAMPMStrings[0];
          if (secAMPMStrings[1].toUpperCase().equals("PM"))pm = 3600*12;
         }
      long   hours   = parseLong  (strings[0]);
      long   minutes = parseLong  (strings[1]);
      double seconds = parseDouble(strings[2]);
      return new Date((long)((pm+days*3600*24+hours*3600+minutes*60+seconds)*1000));
     }

  System.err.println("Utilities.getTime unrecognized time format{"
                    +"nest="  +nest()+"\t"
                    +"string="+s     +"}");
  return null;
 }

 public static void help(String[] argv, Class<?> class_)
 {
  boolean doHelp = false;
  if (argv        != null &&
      argv.length !=0    )
     {
      if (argv[0].startsWith("--?") ||
          argv[0].startsWith("--h") ||
          argv[0].startsWith("-?" ) || 
          argv[0].startsWith("-h" ) || 
          argv[0].startsWith("/?" ) || 
          argv[0].startsWith("/h" ) ||
          argv[0].startsWith("?"  ))doHelp = true;
     }
  else
     {
      doHelp = true;
      for (long ms = 0;ms<500;ms+=50)
          {
           int inBytes = 0;
           try {inBytes = System.in.available();}
           catch (IOException e){fatalError("Utilities.help IOException{"
                                           +"e="+toString(e)+"}");}
           if (inBytes > 0)
              {
               doHelp = false;
               break;
              }
          }
     }

  if (doHelp)
     {
      System.out.println(ParmParser.usage(class_));
      System.exit(0);
     }
 }

 public static byte[] hexToBytes(String s)
 {
  byte[] result = new byte[s.length()/2];
  for (int i=0;i<result.length;i++)
      {
       String hexString = s.substring(i*2,i*2+2);
       int hexInt = Integer.parseInt(hexString,16);
       result[i] = (byte)hexInt;
      }
  return result;
 }

 public static String hexToDec(String x)
 {
  if (x.startsWith("0x"))x = x.substring(2);
  if (x.endsWith  ("x" ))x = x.substring(0,x.length()-1);
  try {return Long.toString(Long.parseLong(x,16));}
  catch (NumberFormatException e){return ".";}
 }

 public static int indexOf(String s, char[] chars)
 {
  return indexOf(s,0,chars);
 }

 public static int indexOf(String s, int pos, char[] chars)
 {
  int result = -1;

  for (char c : chars)
      {
       int pos2 = s.indexOf(c,pos);
       if (pos2 == -1)continue;
       if (result == -1 || result > pos2)result = pos2;
      }
  return result;
 }

 public static int indexOf(String[] ss, String s)
 {
  for (int i=0;i<ss.length;i++)if (ss[i].equals(s))return i;
  return -1;
 }

 public static int indexOf(ArrayList<String> ss, String s)
 {
  for (int i=0;i<ss.size();i++)
      {
       String ssString = ss.get(i);
       if (ssString != null && ssString.equals(s))return i;
      }
  return -1;
 }

 public static String[] insertArrayElement(String[] array, int index, String element)
 {
  String[] newArray = new String[array.length+1];

  for (int i=0;i<index;i++)newArray[i] = array[i];
  newArray[index] = element;
  for (int i=index;i<array.length;i++)newArray[i+1] = array[i];
  return newArray;
 }

 public static boolean isFloat(String s)
 {
  try {Double.parseDouble(s.replace(",",""));}
  catch (NumberFormatException e){return false;}
  return true;
 }

 public static Date isGetDate(String s)
 {
//  System.err.println("Utilities.isGetDate 0{"
//                    +"s="+s+"}");
  if (s == null)return null;
  s = s.trim();
  String strings[] = null;
  try  {
        int pos = s.indexOf(".");
        if (pos != -1)
           {                                                   // trim fraction to milliseconds
            int lastDigit = pos;
            for (int i=pos+1;i<s.length();i++)
                 if (Character.isDigit(s.charAt(i)))lastDigit=i;
                 else break;
            if (lastDigit > pos+4)
                s = s.substring(0,pos+4) + s.substring(lastDigit+1);
//            debug("Utilities.isGetDate period{"
//                 +"s="+s+"}");
           }
       
        strings = s.split(" +");
//        System.err.println("Utilities.isGetDate 5{"
//                          +"s="      +s                          +"\t"
//                          +"strings="+Utilities.toString(strings)+"}");
        if (strings.length == 1)
           {
            if (s.length() >= 23 && s.charAt(10) == 'T')
                if (s.endsWith("z") || s.endsWith("Z"))return SDFyyyymmddThhmmssSSS .parse(s);
                else                                   return SDFyyyymmddThhmmssSSSZ.parse(s);

            strings = s.split("-");
            if (strings.length == 3)
                return SDFyyyy_mm_dd.parse(s);

            strings = s.split(":");
            if (strings.length == 1)
               {
                strings = s.split("/");
                if (strings.length==3)
                    if (strings[0].length() == 4)return SDFyyyy_mm_dd_slash.parse(s);
                    else if (strings[2].length()==2)return SDFmdy4.parse(s);
                         else                       return SDFmdy .parse(s);
                if (!isNumericInteger(s))return null;
                return new Date(parseLong(s));
               }
            if (strings.length == 4)return SDFddmmmyyhhmmss.parse(s);
//            if (IsDebug())System.err.println("Utilities.isGetDate note unknown length{"
//                                   +"s="             +s             +"\t"
//                                   +"strings.length="+strings.length+"}");
            return isGetTime(s);
           }
//        System.err.println("Utilities.isGetDate 4{"
//                          +"s="      +s                          +"\t"
//                          +"strings="+Utilities.toString(strings)+"}");

        if (strings.length == 2)
            if (strings[0].indexOf("/") == 4)
                if (strings[1].indexOf(".") != -1)return SDFymdhms .parse(s);
                else                              return SDFymdhms2.parse(s); 
            else
            if (strings[0].indexOf("-") == 4)
               {
                String[] strings1 = strings[1].split(":");
                if (strings1.length==2)return SDFymdhm.parse(s);
                if (strings1.length==3)
                   {
//                    debug("Utilities.isGetDate SDFymdhms6{"
//                         +"s="+s+"}");
                    if (strings1[2].indexOf(".") != -1)return SDFymdhms6.parse(s);
                    if (strings1[2].indexOf(",") != -1)return SDFymdhms4.parse(s);
                    return SDFymdhms3.parse(s);
                   }
               }
             else
            if (strings[1].indexOf("/") >  0)     return SDFmdy    .parse(strings[1]);

                    
//        System.err.println("Utilities.isGetDate 1{"
//                          +"s="+s+"}");
        if (strings.length == 3)
           {
            if (strings[0].indexOf("/") == 4)
               {
                String[] hms = strings[1].split(":") ;
                if (hms.length == 2)return SDFymdhmampm.parse(s); else
                if (hms.length == 3)
                   {
                     if (strings[1].contains("."))
                         if (strings[2].startsWith("-")             ||
                             strings[2].startsWith("+")             ||
                             Character.isDigit(strings[2].charAt(0)))
                             return SDFymdhms7.parse(s);
                         else
                             return SDFyyyy_mm_dd_hh_mm_ss_SSS_z.parse(s);
                     else  
                         return SDF3ymdhmsz.parse(s);
                   }
                else return null;
               }
            if (strings[0].indexOf("-") == 4)
               {
                if (strings[1].contains(","))return SDFymdhms5.parse(s);

                return SDFyyyy_mm_dd_hh_mm_ss_SSS_z2.parse(s);
               }

            if (strings[1].indexOf(",") != -1)return SDFmdy2.parse(s);
            if (strings[0].length() == 3 &&
                strings[1].length() == 2)return SDFmdy3.parse(s);
//            System.err.println("Utilities.isGetDate 2{"
//                              +"s="+s+"}");
            if (strings[0].indexOf("/")       >  0 &&
                strings[1].split(":" ).length == 2)return SDFmdyhma.parse(s);
//            System.err.println("Utilities.isGetDate 3{"
//                              +"s="     +s                          +"\t"
//                              +"srings="+Utilities.toString(strings)+"}");
            return SDF3.parse(s);
           }
        if (strings.length == 4)
           {         // Thu 03/15/2018 11:06 PM
            s = s.substring(strings[0].length()+1); 
            strings = s.split(" +");
           }

        if (strings.length == 5)return SDFDayDateTime5.parse(s);
        if (strings.length == 6)
            if (strings[4].contains(":"))
               {
                if (strings[5].contains("GMT")) // SimpleDateFormat doesn't like GMT any more.
                    s = s.replace("GMT","UTC");
                return SDFDayDateTimeZ.parse(s);
               }
            else    
                return SDFDayDateTime .parse(s);
        return SDFDate.parse(s);
       }
  catch(ParseException e)
       {
        Date result = isGetTime(s);
        if (result != null)return result;
        System.err.println("Utilities.isGetDate note parse exeption{"
                   +"e="   +toString(e)     +"\t"
                   +"nest="+Utilities.nest()+"\t"
                   +"s="   +s               +"}");
        try {if (strings.length == 3)return SDF3dmyhmsz.parse(s);}
        catch (ParseException e1){}
       }
  return null;
 }

 public static Date isGetTime(String s)
 {
  s = s.trim();

  String[] strings = s.split(":");
  if (strings.length == 2)
      try {
           Date time = null;
           if (s.endsWith("M") ||
               s.endsWith("m"))time = SDFhma.parse(s);
           else                time = SDFhm .parse(s);

           Date time0 = SDFhm.parse("00:00"); // because time zone.
           Date result = new Date(time.getTime()-time0.getTime());
//           debug("Utilities.isGetTime{"
//                +"result="+result+"\t"
//                +"time="  +time  +"\t"
//                +"time0=" +time0 +"}");
           return result;
          }
      catch (ParseException e){Utilities.error("Utilities.isGetTime ParseException{"
                                       +"e="+Utilities.toString(e)+"\t"
                                       +"s="+s                    +"}");}
  if (strings.length !=  3)return null;
  if (s.indexOf(" ") != -1)return null;

  if (!isNumeric(strings[0]))return null;
  if (!isNumeric(strings[1]))return null;
  if (!isFloat  (strings[2]))return null;

  long hours   = parseLong(strings[0]);
  long minutes = parseLong(strings[1]);
  if (isNumeric(strings[2]))
     {
      long seconds = parseLong(strings[2]);
      return new Date((hours*3600+minutes*60+seconds)*1000);
     }
  else 
     {
      double seconds = parseDouble(strings[2]);
      return new Date((long)(((double)hours*3600+(double)minutes*60+seconds)*1000));
     }
 }

 public static boolean isHex(String s)
 {
  if (s==null)return false;
  for (int i=0;i<s.length();i++)
      {
       char c = s.charAt(i);
       if (!((c >= 'a' && c <= 'f') ||
             (c >= 'A' && c <= 'F') ||
             (c >= '0' && c <= '9')))return false;  
      }
  return true;
 }

 public static boolean isNaN(double v)
 {
  return v != v;
 }

 public static boolean isNaN(float v)
 {
  return v != v;
 }

 public static boolean isNumeric(String s)
 {
  if (isNumericInteger(s) || isNumericFloat(s))return true;
  return false;
 }

 public static boolean isNumericFloat(String s)
 {
  return !isNaN(parseDouble(s));
 }

 public static boolean isNumericInteger(String s)
 {
  if (s==null)return false;
  try {Long.parseLong(s.replace(",",""));}
  catch (NumberFormatException e){return false;}
  return true;
 }

 public static void log(PrintStream ps,String msg)
 {
//  System.err.println("Utilities.log enter{"
//                   +"nest="  +nest()                        +"\t"
//                   +"thread="+Thread.currentThread().getId()+"}");
  if (msg == null)
     {
      ps.println(toStringMillis(new Date())+"\tnull");
      return;
     }
  String[] strings = msg.replace("\r","").split("\n",-1);
  for (String string : strings)
       ps.println(toStringMillis(new Date()) + "\t" + string);
//  System.err.println("Utilities.log return");
 }

 public static void log(String msg,boolean expand)
 {
  log(msg);
  if (expand)
      try {log(new StructuredMessageFormatter2().format(msg));}
      catch (MyIOException    e){log(toString(e));}
      catch (MyParseException e){log(toString(e));}
 }

 public static void log(String msg)
 {
  log(System.out,msg);
 }

 public static void error(String msg)
 {
  log(System.err,"Error\t"+msg);
 }

 public static String nest()
 {
  return nest(new Exception());
 }

 public static String nest(Throwable t)
 {
  StackTraceElement[] elements    =t.getStackTrace();
  StringBuffer        result      =new StringBuffer("{");
  boolean             first       =true;
  boolean             firstElement=true;
  for (StackTraceElement element : elements)
  {
   if (firstElement){firstElement=false;continue;}
   if (first)first=false;
   else      result.append("\t");
   String method        = element.toString();
   String quotedElement = null;
   try {quotedElement = quote(method);}
   catch (MyParseException e)
         {
          System.err.println("Utilities.nest invalid unable to quote method name"
                            +"method="+method+"}");
          quotedElement = method;
         }
   result.append(quotedElement);
  }
  result.append("}");
  return result.toString();
 }

 public static String padLeftZero(String s, int length)
 {
  return s.length() >= length?s:Zeros(length-s.length())+s;
 }

 public static String padLeftZero(long l, int length)
 {
  return padLeftZero(""+l,length);
 }

 public static String padRight(String source, int length)
 {
  if (source == null)return length>4?"null"+spaces(length-4):"null";
  int slen = source.length();
  return slen < length?source+spaces(length-slen):source;
 }

 public static Date parseDateTime(String s)
 {
  SimpleDateFormat sdfDate=new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
  try {return sdfDate.parse(s.trim());}catch (ParseException e){}
  return null;
 }

 public static boolean parseBoolean(String s)
 {
     if (s == null)
         System.err.println("Utilities.parseBoolean string is null{stack=" + nest() + "}");
     if (s.toUpperCase().equals("TRUE"))
         return true;
     if (s.toUpperCase().equals("FALSE"))
     {
         return false;
     } else
     {
         System.err.println("Utilities.parseBoolean Unrecognized true/false string{string=" + s + "}");
         return false;
     }
 }

 public static String[] parseCityStateAbbrZip(String s)
 {
  System.err.println("Utilities.parseCityStateApprZip enter{"
                    +"s="+s+"}");
  if (s==null)return null;
  s                = s.trim();
  int    pos       = s.lastIndexOf(" ");
  if (pos == -1)
     {
      System.err.println("Utilities.parseCityStateApprZip unable to parse string - no spaces{"
                        +"s="+s+"}");
      return new String[]{s};
     }
  String zip       = s.substring(pos+1);
  s                = s.substring(0,pos);
  pos              = s.lastIndexOf(" ");
  if (pos == -1)
     {
      System.err.println("Utilities.parseCityStateApprZip unable to parse string - only 1 space{"
                        +"s="+s+"}");
      return new String[]{s,zip};
     }
  String stateAbbr = s.substring(pos+1);
  String city      = s.substring(0,pos);

  String[] result  = new String[]{city,stateAbbr,zip};
  System.err.println("Utilities.parseCityStateApprZip return{"
                    +"result="+toString(result)+"}");
  return result;
 }

 public static Date parseDaysHoursMinutesSeconds(String s)
 {
  SimpleDateFormat sdfDate=new SimpleDateFormat("D HH:mm:ss z");
  try {
       Date result = sdfDate.parse(s.trim()+" GMT");
       result.setTime(result.getTime()+24*3600*1000); // adjust for 0 based day.
       return result;
      }
  catch (ParseException e){System.err.println("Utilities.parseDaysHoursMinutesSeconds ParseException{"
                                     +"string="   +s          +"\t"
                                     +"exception="+toString(e)+"}");
                           System.exit(16);}
  return null;
 }

 public static String remove(String s, char c)
 {
  if (s == null)return null;
  while (true)
        {
         int pos = s.indexOf(c);
         if (pos == -1)return s;
         s = s.substring(0,pos)+s.substring(pos+1);
        }
 }

 public static double parseDouble(String s)
 {
  if (s         == null)return Double.NaN;
  s = s.trim();
  if (s.equals("nan")  )return Double.NaN;
  if (s.equals("NaN")  )return Double.NaN;
  if (s.length()== 0   )return Double.NaN;
  if (s.equals(".")    )return Double.NaN;
  s = remove(s,','); // for speed.
//  s = s.replace(",", "").trim();
  boolean negFlag = false;
  if (s.startsWith("(") && s.endsWith(")"))
     {
      negFlag = true;
      s       = s.substring(1,s.length()-1);
     }
  if (s.startsWith("$"))s = s.substring(1);
  s = s.toLowerCase().trim();
  if (s.startsWith("0x") &&
      s.indexOf("p")==-1)s += "p0";

  double result = Double.NaN;
  try {result = Double.parseDouble(s);}
  catch (NumberFormatException e)
        {
         System.err.println("Utilities.parseDouble exception{"
                           +"e="+toString(e)+"\t"
                           +"s="+s          +"}");
         return Double.NaN;
        }

  if (negFlag) return -result;
  return result;
 }

 public static float parseFloat(String s)
 {
  return (float)parseDouble(s);
 }

 public static String[] parseNumberStreet(String s)
 {
  if (s==null)return null;
  s       = s.trim();
  int pos = s.indexOf(" ");
  if (pos == -1)return new String[]{"",s};
  String number = s.substring(0,pos);
  String street = s.substring(pos+1).trim();
  if (!isNumeric(number))return new String[]{"",s};
  return new String[]{number,street};
 }

 public static int parseHexInt(String s)
 {
  try {return Integer.valueOf(s,16).intValue();}
  catch (NumberFormatException e){System.err.println("parseHexInt invalid hex string{"
                                                    +"nest="+nest()+"\t"
                                                    +"s="   +s     +"}");
                                  System.exit(1);}
  return 0;
 }

 public static long parseHexLong(String s) throws LengthException
 {
  if (s.startsWith("0x"))s = s.substring(2);
  if (s.startsWith("0X"))s = s.substring(2);
  try {return Long.valueOf(s,16).longValue();}
  catch (NumberFormatException e)
  {
   try {
        if (s.length() == 0) throw new LengthException();
        if (s.length() > 16)
           {
            // Print some diagnostics
            System.err.println("Exception handler parseHexLong length="+s.length()+" string: "+s);
	        s = s.substring(s.length()-16);
           // Turn off first bit		
	       }
	     return Long.valueOf(s,16).longValue();
       }
   catch (NumberFormatException f)
   {
    try {return new BigInteger(s,16).longValue();}
    catch(NumberFormatException g){System.err.println("parseHexLong invalid hex string{"
                                                     +"nest="+nest()+"\t"
                                                     +"s="   +s     +"}");}
   }
  }
  return -1;
 }

 public static int parseInt(String s)
 {
  try {return Integer.parseInt(s.replace(",","").trim());}
  catch (NumberFormatException e){System.err.println("parseInt NumberFormatException{"
                                           +"e="         +toString(e) +"\t"
                                           +"s="         +s           +"\t"
                                           +"stacktrace="+nest()      +"}");}
  return 0;
 }

 public static long parseLong(String s)
 {
  try {return Long.parseLong(s.replace(",","").trim());}
  catch (NumberFormatException e){System.err.println("parseLong NumberFormatException{"
                                           +"e="         +toString(e) +"\t"
                                           +"s="         +s           +"\t"
                                           +"stacktrace="+nest()      +"}");}
  return 0;
 }


 private static char[] TransparentChars={'\b', '\f', '\r', '\n', '\t', '\"', '\\'};
 public static String quote(String source)
 throws MyParseException
 {
//  if (debug())System.err.println("Utilities.quote note source{"
//                                +"source="+source+"}");
  if (source == null          )return null;
  StringBuffer result = new StringBuffer("\"");
  int pos = 0;
  for (int newPos=indexOf(source,pos,TransparentChars);newPos!=-1;pos=newPos+1,newPos=indexOf(source,pos,TransparentChars))
      {
//       if (debug())System.err.println("Utilities.quote{"
//                                     +"newPos="+newPos+"\t"
//                                     +"pos="   +pos   +"}");
       result.append(source.substring(pos, newPos));
       switch (source.charAt(newPos))
              {
               case '\b' : result.append("\\b" );continue;
               case '\f' : result.append("\\f" );continue;
               case '\r' : result.append("\\r" );continue;
               case '\n' : result.append("\\n" );continue;
               case '\t' : result.append("\\t" );continue;
               case '\"' : result.append("\\\"");continue;
               case '\\' : result.append("\\\\");continue;
               default   : throw new MyParseException("Utilities.quote internal error. unexpected char{"
                                                     +"char="  +source.charAt(newPos)+"\t"
                                                     +"newPos="+newPos               +"\t"
                                                     +"pos="   +pos                  +"\t"
                                                     +"source="+source               +"}");
              }
      }
  result.append(source.substring(pos,source.length())).append("\"");
  return result.toString();
 }

 public static int quotedOffset(String source,int offset)
 {
  offset += 1;
  for (int pos=source.indexOf("\\");pos!=-1;pos=source.indexOf("\\",pos+2))
      {
       if (offset <= pos)break;
       offset ++;
      }
  return offset;
 }

 public static String repeat(String s, int count)
 {
  StringBuffer result = new StringBuffer();
  for (int i=0;i<count;i++)result.append(s);
  return result.toString();
 }

 public static String replaceFirst(String s, String o, String r)
 {
  if (s == null)return null;
  int pos = s.indexOf(o);
  if (pos == -1)return s;
  return s.substring(0,pos)+r+s.substring(pos+o.length());
 }

 public static String replaceLast(String s, String o, String r)
 {
  if (s == null)return null;
  int pos = s.lastIndexOf(o);
  if (pos == -1)return s;
  return s.substring(0,pos)+r+s.substring(pos+o.length());
 }

 public static void sleepMs(long ms)
 {
  long endTime = new Date().getTime()+ms;
  while (ms > 0)
        {
         try   {Thread.sleep(ms);}
         catch (InterruptedException e){System.err.println("Utilities.sleep sleep interrupted "+e);}
         ms = (int)(endTime - new Date().getTime());
        }
 }

 private static HashMap<Integer,String> SpaceCache = new HashMap<Integer,String>();
 public static String spaces(int size)
 {
  if (size < 0)
     {
      error("Utilities.spaces negative size{"
           +"size="+size  +"\t"
           +"nest="+nest()+"}");
      return "";
     }
  if (Spaces.length() < size)
   synchronized(Spaces)
   {
    while (Spaces.length()<size)Spaces.append(' ');
   }
  String result = SpaceCache.get(new Integer(size));
  if (result != null)return result;
  result = Spaces.substring(0, size);
  SpaceCache.put(new Integer(size),result);
  return result;
 }

 // because String.split takes a regex and that's too complicated for me.
 public static String[] split(String s,String split)
 {
  if (s == null)return null;
  StringBuffer      stringBuffer = new StringBuffer(s);
  ArrayList<String> result       = new ArrayList<String>();
  int               splitLength  = split.length();
  for (int offset=stringBuffer.indexOf(split);offset!=-1;offset=stringBuffer.indexOf(split))
      {
       result.add(stringBuffer.substring(0,offset));
       stringBuffer.delete(0,offset+splitLength);
      }
  result.add(stringBuffer.toString());
  return result.toArray(new String[0]);
 }

 public static String startWith(String s, String p)
 {
  if (s==null        )return null;
  if (s.startsWith(p))return s;
  return p+s;
 }

 public static String substring(String s, int begin, int end)
 {
  if (s == null)return null;
  if (begin >= s.length())return "";
  if (end   >= s.length())return s.substring(begin);
  return s.substring(begin,end);
 }

 static char[] hexChars = new char[]{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
 public static String toHex(String s)
 {
  if (s == null)return null;
  StringBuffer result = new StringBuffer();
  for (int i=0;i<s.length();i++)
      {
       char c  = s.charAt(i);
       char h1 = hexChars[(c >> 4) & 0x0f];
       char h2 = hexChars[ c       & 0x0f];
       if (i > 0)result.append(' ');
       result.append(h1).append(h2);
      }
  return result.toString();
 }

 public static String toString(AbstractCollection<?> a)
 {
//  if (debug())System.err.println("toString(AbstractCollection)");
  if (a==null)return "null";
  StringBuffer result = new StringBuffer();
  result.append("{AbstractCollection:");
  boolean first=true;
  for (Object o:a)
      {
       if (first)first = false;
       else      result.append("\t");
       if (o == null)result.append("null");
       else          result.append(toString(o));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(AbstractMap<?,?> a)
 {
//  if (debug())System.err.println("toString(AbstractMap)");
  if (a==null)return "null";
  StringBuffer result = new StringBuffer();
  result.append("{AbstractMap:");
  boolean first=true;
  for (Map.Entry<?,?> entry:a.entrySet())
      {
       if (first)first = false;
       else      result.append("\t");
       if (entry == null)result.append("null");
       else          
           {
            String key   = toString(entry.getKey  ());
            String value = toString(entry.getValue()); 
            result.append(key+"="+value);
           }
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(boolean[]o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{boolean[]:");
  boolean      first  = true;
  for (boolean v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(Boolean.toString(v));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(byte   []o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{byte[]:");
  boolean      first  = true;
  for (byte v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(Byte.toString(v));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(char   []o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{char[]:");
  boolean      first  = true;
  for (char v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(v);
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(Date value)
 {
  if (value == null)return null;
  return SDFyyyy_mm_dd_hh_mm_ss_z.format(value);
 }

 public static String toString(double number)
 {
  String result = toString_(number);
//  if (result.equals("-"))System.err.println("Utilities.toString -{"
//                                           +"number="+number+"\t"
//                                           +"result="+result+"}");
  return result;
 }

 public static String toString_(double number)
 {
  if (number == Double.NaN)return "NaN";
  if (number == 0         )return "0";
  String resultString = toString(number, 9);
  if (!resultString.contains("."))return resultString;
  StringBuffer result = new StringBuffer(resultString);

  while (result.charAt(result.length()-1) == '0')result.deleteCharAt(result.length()-1);
  if    (result.charAt(result.length()-1) == '.')result.deleteCharAt(result.length()-1);
  String resultString2 = result.toString();
  if (resultString2.equals("" ))resultString2 = "0";  // Can happen if very small number .00000000000000000000000005
  if (resultString2.equals("-"))resultString2 = "0";
  return resultString2;
 }

 public static String toString(String number, int places)
 {
  if (number.indexOf('.')==-1 && places == 0)return number;
  if (!isNumeric(number))return number;
  return toString(parseDouble(number),places);
 }

 public static String toString(double number, int places)
 {
//  System.err.println("toString number places{"
//                    +"nest="  +nest()+"\t"
//                    +"number="+number+"\t"
//                    +"places="+places+"}");
  if (Double.isNaN(number))return "NaN";
  DecimalFormat decimalFormat = new DecimalFormat("####################.000000000");
  decimalFormat.setMaximumIntegerDigits(20);
  decimalFormat.setMaximumFractionDigits(places);
  StringBuffer  resultStringBuffer  = new StringBuffer();
  FieldPosition resultFieldPosition = new FieldPosition(NumberFormat.FRACTION_FIELD);
  String result = decimalFormat.format(number,resultStringBuffer,resultFieldPosition).toString();
//  String result =Double.toString(number).trim();
  if (result.indexOf("E") != -1)result="0.0";
  if (result.startsWith("."))result ="0"+result;
  int pos = result.indexOf(".");
  if (pos == -1)
     {
      if (places==0)return result;
      return result+"."+Zeros(places);
     }

  if (places==0)
      return result.substring(0,pos);

  int nplaces = result.length()-pos-1;
  if (nplaces == places)return result;
  if (nplaces  > places)return result.substring(0,result.length()-(nplaces-places));
  int pad = places-nplaces;
  result += Zeros(pad);
  return result;
 }

 public static String toString(double []o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{double[]:");
  boolean      first  = true;
  for (double v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(Double.toString(v));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(float  []o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{float[]:");
  boolean      first  = true;
  for (float v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(Float.toString(v));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(int    []o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{int[]:");
  boolean      first  = true;
  for (int v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(Integer.toString(v));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(long l)
 {
  return Long.toString(l);
 }

 public static String toString(long l, int places)
 {
  StringBuffer result = new StringBuffer(padLeftZero(Long.toString(l),places));
  return result.insert(result.length()-places,'.').toString();
 }

 public static String toString(long   []o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{long[]:");
  boolean      first  = true;
  for (long v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(Long.toString(v));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(short  []o)
 {
  if (o==null)return "null";
  StringBuffer result = new StringBuffer("{short[]:");
  boolean      first  = true;
  for (short v:o)
      {
       if (first)first=false;
       else      result.append("\t");
       result.append(Short.toString(v));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(Object o)
 {
//  if (debug())System.err.println("toString(Object)");
  if (o==null                       )return "null"                ;
  Class<?> class_=o.getClass();
  if (class_.isArray())
     {
      Class<?> componentClass = class_.getComponentType();
      if (componentClass.isPrimitive())
         {
          if (componentClass.equals(boolean.class))return toString((boolean[])o);
          if (componentClass.equals(byte.class   ))return toString((byte   [])o);
          if (componentClass.equals(char.class   ))return toString((char   [])o);
          if (componentClass.equals(double.class ))return toString((double [])o);
          if (componentClass.equals(float.class  ))return toString((float  [])o);
          if (componentClass.equals(int.class    ))return toString((int    [])o);
          if (componentClass.equals(long.class   ))return toString((long   [])o);
          if (componentClass.equals(short.class  ))return toString((short  [])o);
         }
      return toString((Object[])o);
     }
  if (o instanceof AbstractCollection<?>)return toString((AbstractCollection<?>)o);
  if (o instanceof AbstractMap<?,?>     )return toString((AbstractMap<?,?>     )o);
  if (o instanceof Throwable            )return toString((Throwable            )o);
  return o.toString();
 }

 public static String toString(Object[] a)
 {
//  if (debug())System.err.println("toString(Object[])");
  if (a==null)return "null";
  StringBuffer result = new StringBuffer();
  result.append("{Array:");
  boolean first=true;
  for (Object o:a)
      {
       if (first)first = false;
       else      result.append("\t");
       result.append(toString(o));
      }
  result.append("}");
  return result.toString();
 }

 public static String toString(Throwable t)
 {
//  if (debug())System.err.println("toString(Throwable)");
  if (t == null) return "null";
  StackTraceElement frames[] = t.getStackTrace();
  StringBuffer      result   = new StringBuffer("{Throwable:\t");
  result.append("Message="+t.toString()+"\t");
  result.append("Stack={");
  if (frames.length > 0)
     {
      result.append(frames[0].toString());
      for (int i=1;i<frames.length;i++)result.append("\t"+frames[i].toString());
     }
  result.append("}}");
  return result.toString();
 }

 public static String toStringMillis(Date value)
 {
  if (value == null)return null;
  return SDFymdhms7.format(value);
 }

 public static String[] toStrings(double[] a)
 {
  if (a == null)return null;
  String[] result = new String[a.length];
  for (int i=0;i<a.length;i++)result[i] = toString(a[i]);
  return result;
 }

 public static String toStringTime(Date value)
 {
  if (value == null)return null;
  long time    = value.getTime();
  long ms      = time % 1000; time = time / 1000;
  long secs    = time % 60;   time = time /   60;
  long minutes = time % 60;   time = time /   60;
  long hours   = time % 24;   time = time /   24;
  long days    = time;
  return ""+days+" "+hours+":"+padLeftZero(minutes,2)+":"+padLeftZero(secs,2)+"."+padLeftZero(ms,3);
 }

 public static String toStringYMD(Date value)
 {
  if (value == null)return null;
  return SDFyyyy_mm_dd_slash.format(value);
 }

 public static String trimSpaces(String s)
 {
  if (s == null)return null;
  if (!s.startsWith(" ") && !s.endsWith(" ")) return s;

  StringBuffer sb = new StringBuffer(s);
  while (sb.charAt(0            ) == ' ')sb.deleteCharAt(0            );
  while (sb.charAt(sb.length()-1) == ' ')sb.deleteCharAt(sb.length()-1);
  return sb.toString();
 }

 public static String upcaseFirst(String s)
 {
  if (s         ==null)return null;
  if (s.length()==0   )return s;
  return Character.toUpperCase(s.charAt(0)) + s.substring(1);
 }

 public static String upcaseFirstOfWords(String s)
 {
  if (s         ==null)return null;
  if (s.length()==0   )return s;
  String[] strings = s.split(" ",-1);
  StringBuffer result = new StringBuffer();
  for (int i=0;i<strings.length;i++)
      {
       if (strings[i].length()==0){result.append(" ");continue;}
       if (i>0)result.append(" ");
       result.append(upcaseFirst(strings[i]));
      }
  return result.toString();
 }

 public static void vetInputFile(MyInputFile inputFile)
 throws MyIOException
 {
  if (inputFile ==null){System.err.println("vetInputFile InputFile is missing" );System.exit(1);}
  vetInputFile(inputFile.getFileName());
 }

 public static void vetInputFile(String fileName)
 throws MyIOException
 {
  if (fileName == null)throw new MyIOException("vetInputFile File not provided");
  File file = new File(fileName);
  if (!file.exists () )throw new MyIOException("vetInputFile File doesn't exist{"      +"File="+fileName+"}");
  if (!file.isFile () )throw new MyIOException("vetInputFile File isn't a normal file{"+"File="+fileName+"}");
  if (!file.canRead() )throw new MyIOException("vetInputFile File isn't readable{"     +"File="+fileName+"}");
 }

 public static void vetInputTsvFiles(InputTsvFile[] inputFiles)
 throws MyIOException
 {
  if (inputFiles==null || inputFiles.length == 0)
      {System.err.println("vetInputTsvFiles no InputFile provided");System.exit(1);}
  for (InputTsvFile inputFile : inputFiles)vetInputFile(inputFile.getFileName());
 }

 public static InetAddress vetIP(String ip)
 {
  if (ip==null)fatalError("Utilities.VetIP ip is null");
  String[] strings    = split(ip,"."); // because . is a regex thing.
  int[]    ipIntArray = new int[4];
  if (strings.length != 4)Utilities.fatalError("vetIP ip is not ipv4 format{"
                                              +"ip="+ip+"}");
  for (int i=0;i<strings.length;i++)
      {
       if (!isNumericInteger(strings[i]))fatalError("vetIP ip is not ipv4 format element not integer{"
                                                   +"ip="+ip+"}");
       ipIntArray[i] = parseInt(strings[i]);
       if (ipIntArray[i]>255)fatalError("Utilities.vetIP ip is not ipv4 format element > 255{"
                                       +"ip="+ip+"}");
      }
  byte[] ipByteArray = new byte[4];
  for (int i=0;i<4;i++)ipByteArray[i] = (byte)ipIntArray[i];
  try {return InetAddress.getByAddress(ipByteArray);}
  catch (UnknownHostException e){fatalError("vetIP ip is not ipv4 format UnknownHostException{"
                                           +"ip="+ip         +"\t"
                                           +"e=" +toString(e)+"}");}
  return null;
 }

 public static void vetOutputFile(MyOutputFile outputFile)
 throws MyIOException
 {
  if (outputFile ==null){System.err.println("vetOutputFile OutputFile is missing" );System.exit(1);}
  vetOutputFile(outputFile.getFileName());
 }

 public static void vetOutputFile(String fileName)
 throws MyIOException
 {
  if (fileName == null               )throw new MyIOException("vetOutputFile File not provided");
  File file = new File(fileName);
  if (file.exists() && !file.isFile())throw new MyIOException("vetOutputFile isn't a normal file{"+"File="+fileName+"}");
 }

 public static void vetOutputTsvFiles(OutputTsvFile[] outputFiles)
 throws MyIOException
 {
  if (outputFiles == null || outputFiles.length == 0)
     {
      System.err.println("vetOutputTsvFiles no OutputFile provided.");
      System.exit(1);
     }
  for (int i=0;i<outputFiles.length;i++)
      {
       vetOutputFile(outputFiles[i].getFileName());
       for (int j=i+1;j<outputFiles.length;j++)
           {
            if (outputFiles[i].getFileName().equals(outputFiles[j].getFileName()))
               {
                System.err.println("vetOutputTsvFiles duplicate OutputFile{"
                                  +"FileName="+outputFiles[i].getFileName()+"}");
                System.exit(1);
               }
           }
      }
 }

 public static void vetPort(Integer port)
 {
  if (port==null )fatalError("vetPort Port is missing");
  if (port< 0    )fatalError("vetPort Port is negative{Port="+port+"}");
  if (port> 65765)fatalError("vetPort Port >65765{Port="     +port+"}");
 }

 public static String Zeros(int size)
 {
  if (Zeros.length() < size)
      synchronized(Zeros)
      {
       while (Zeros.length()<size)Zeros.append('0');
      }
  return Zeros.substring(0,size);
 }

}
