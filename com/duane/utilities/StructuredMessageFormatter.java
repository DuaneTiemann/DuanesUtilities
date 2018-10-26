package com.duane.utilities;
import  java.io.StringReader;
import  java.lang.StringBuffer;

import com.duane.utilities.MyIOException;
import com.duane.utilities.MyParseException;
import com.duane.utilities.MyStreamTokenizer;
import com.duane.utilities.Utilities;

// sample structure:
// name=value
// name=value
// name
// name

// name=value
//      value
//      value
// name=value

// name={name:
//      }

// name={
//      }

// name
// {
// }


public class StructuredMessageFormatter
{
 private static boolean Debug = false;

 private static void doBrace(MyStreamTokenizer parms,StringBuffer result,int indent)
 throws MyIOException,MyParseException
 {
  if (Debug)Utilities.debug("StructuredMessageFormatter.doBrace enter{"
                           +"indent="+indent+"}");
  String token = getTokenOrDie(parms);
  if (parms.isSymbol() && token.equals("{"))
     { // It's another open brace. Forget it. We'll recurse on it below.
      if (result!=null)result.append("{\n").append(Utilities.spaces(indent+1));
      ungetToken(parms);
     }
  else
     {
      String nextToken = getTokenOrDie(parms);
      if (parms.isSymbol() && nextToken.equals(":"))
         { // It's {name:
          if (result != null)result.append("{").append(token).append(":").append("\n");
          String tokenAfterColon = getTokenOrDie(parms);
          if (tokenAfterColon.equals("{") && result!=null)result.append(Utilities.spaces(indent+1));
          parms.ungetToken();
         }
      else
         { // It's something else so put them both back and just do the brace.
          parms.ungetToken();
          parms.ungetToken();
          if (result != null)result.append("{\n");
         }
     }

// processing of original brace at entry is complete.

  String blockString = parseBlock(parms,indent+1,true);
  if (result!=null)result.append(blockString).append(Utilities.spaces(indent)).append("}\n");
//  String resultString = result==null?"null":result.toString();
//  if (Debug)Utilities.debug("StructuredMessageFormatter.dobrace add indent{"
//                           +"indent="+indent      +"\t"
//                           +"result="+resultString+"}");
  if (Debug)Utilities.debug("StructuredMessageFormatter.doBrace return");
 }

 public static String format(String message)
 throws MyIOException, MyParseException
 {
  if (Debug)Utilities.debug("StructuredMessageFormatter.format note message{"
                           +"message="+message+"}");
  return format(message, 0);
 }

 public static String format(String message,int indent)
 throws MyIOException, MyParseException
 {
  if (message == null)return null;
  MyStreamTokenizer parms = new MyStreamTokenizer(new StringReader(message), null);
  parms.setDebug          (Debug                               );
  parms.setSymbolChars    (new char[]{'\t', '{', '}', ':', '='});
  parms.setSeparatorChars (new char[]{'\t'                    });
  parms.setWhitespaceChars(new char[]{                        });
  String result = parseBlock(parms,indent,false);
  String token = parms.getToken();
  if (token != null)Utilities.error("StructuredMessageFormatter.format trailing data not formatted{"
                                   +"token="+token+"}");
  return result;
 }

 private static String getToken(MyStreamTokenizer parms)
 throws MyIOException,MyParseException
 {
  String token = getToken_(parms);
  if (token!=null&&token.startsWith("\"") && token.endsWith("\""))token = token.substring(1,token.length()-1);
  if (Debug) Utilities.debug("StructuredMessageFormatter.getToken note token{"
                            +"token="+token+"}");
  return token;
 }

 private static String getToken_(MyStreamTokenizer parms)
 throws MyIOException,MyParseException
 {
  String token = parms.getToken();
  if (token == null)return null;
  if (!parms.isSeparator(token))return token;
  String nextToken = parms.getToken();
  if (nextToken == null)return null;
  if (parms.isSeparator(nextToken))return "";
  return nextToken;
 }

 private static String getTokenOrDie(MyStreamTokenizer parms)
 throws MyIOException,MyParseException
 {
  String result = getToken(parms);
  if (result == null)throw new MyParseException("StructuredMessageFormatter.getTokenOrDie unexpected EOF");
  return result;
 }

 // calls otherparseBlock twice. Once to get the name lengths. Again to get the result.

 private static String parseBlock(MyStreamTokenizer parms, int indent, boolean parseToCloseBrace)
 throws MyIOException, MyParseException
 {
  parms.lockCurrentToken();
  long         id            = parms.getCurrentTokenId();
  int          maxNameLength = parseBlock(parms,indent,null,0,parseToCloseBrace);
  StringBuffer result        = new StringBuffer();
  parms.setCurrentToken(id);
  parms.unlockCurrentToken();
  parseBlock(parms,indent,result,maxNameLength,parseToCloseBrace);
//  if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock note result string{"
//                           +"indent="+indent           +"\t"
//                           +"result="+result.toString()+"}");
  return result.toString();
 }

 private static int parseBlockDepth=0;
 private static int parseBlock(MyStreamTokenizer parms, int indent, StringBuffer result, int maxNameLength, boolean parseToCloseBrace)
 throws MyIOException, MyParseException
 {
  parseBlockDepth++;
  mainParseLoop:
  for (String token=getToken(parms);token!=null;token=getToken(parms))
      {
       if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock note token{"
                                +"maxNameLength="    +maxNameLength    +"\t"
                                +"parseBlockDepth="  +parseBlockDepth  +"\t"
                                +"parseToCloseBrace="+parseToCloseBrace+"\t"
                                +"token="            +token            +"}");

       if (token.equals("{")) 
          {     // process {
           if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock found {{"
                                    +"parseBlockDepth="+parseBlockDepth+"}");
           doBrace(parms,result,indent);
           continue;
          }

       if (token.equals("}"))
          { // process }                Caller will add the closing brace
           parseBlockDepth--;
           if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock return 1{"
                                    +"parseBlockDepth="+parseBlockDepth+"}");
           if (parseToCloseBrace) return maxNameLength;
           throw new MyParseException("StructuredMessageFormatter.parseBlock unexpected close brace{"
                                     +"where="+parms.where()+"}");
          }

       if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock note potential name{"
                                +"token="+token+"}");
       // It's probably a name
       String nextToken = getToken(parms);
       if (nextToken == null)
          {   // EOF
           if (result!=null)result.append(Utilities.spaces(indent)).append(token);
           break;
          }

       if (parms.isSymbol()&&nextToken.equals("="))
          { //  name=
           if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock found ={"
                                    +"parseBlockDepth="+parseBlockDepth+"}");
           if (token.length() > maxNameLength)maxNameLength = token.length();
           if (result!=null)result.append(Utilities.spaces(indent)).append(Utilities.padRight(token,maxNameLength)).append("=");
           nextToken = getToken(parms);
           if (nextToken == null)break;

           if (parms.isSymbol())
              {
               if (nextToken.equals("{"))
                  { // name={
                   if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock found name={");
                   doBrace(parms,result,indent+maxNameLength+1);
                   continue; 
                  }
               if (nextToken.equals("}"))
                  { // name=}
                   if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock found name=}");
                   if (result!=null)result.append("\n");
                   ungetToken(parms); // handle at higher level to preserve DRYness.
                   continue; 
                  }
              }
           ungetToken(parms);
           // name=value
           int valueCount=0;                    // Value processing:
           for (token=getToken(parms);token!=null;token=getToken(parms))
               { // do all the values
                if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock note potential value{"
                                         +"token="+token+"}");
                nextToken = getToken(parms);
                if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock note nextToken{"
                                         +"nextToken="+nextToken+"}");
                if (nextToken == null)break;
                if (parms.isSymbol())
                   {
                    if (nextToken.equals("="))
                       { // name=value= so go back to original indent by backing up a couple and going around again.
                        if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock found value=");
                        ungetToken(parms);
                        ungetToken(parms);
                        if (valueCount == 0) //   name = name =
                            if (result!=null)result.append("\n");
                        continue mainParseLoop;
                       }
                    if (nextToken.equals("}"))
                       {
                        int valueIndent = valueCount==0?0:indent+maxNameLength+1;
                        if (result!=null)result.append(Utilities.spaces(valueIndent)).append(token).append("\n");
                        parseBlockDepth--;
                        if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock return 2{"
                                                 +"parseBlockDepth="+parseBlockDepth+"\t"
                                                 +"valueIndent="    +valueIndent    +"}");
                        if (!parseToCloseBrace)throw new MyParseException("StructuredMessageFormatter.parseBlock unexpected close brace{"
                                                                         +"where="+parms.where()+"}");
                        return maxNameLength;
                       }
                    ungetToken(parms);     // probably {
                    continue mainParseLoop;
                   }
                ungetToken(parms); // put nextToken back.
                // name=value  check for name=value=value
                int valueIndent = valueCount==0?0:indent+maxNameLength+1;
                if (result!=null)result.append(Utilities.spaces(valueIndent)).append(token).append("\n");
                valueCount++;
               }
           if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock out of value loop");
           continue;
          }

        // It's a standalone name
       ungetToken(parms);  // put nextToken back.
       if (result!=null)result.append(Utilities.spaces(indent)).append(token).append("\n");
      }
  parseBlockDepth--;
  if (Debug)Utilities.debug("StructuredMessageFormatter.parseBlock return 3{"
                           +"parseBlockDepth="+parseBlockDepth+"}");
  if (parseToCloseBrace)
     {
      String resultString = result==null?"null":result.toString();
      throw new MyParseException("StructuredMessageFormatter.parseBlock unexpected EOF{"
                                +"result="+resultString+"\t"
                                +"where="+parms.where()+"}");
     }
  return maxNameLength;
 }

 private static void ungetToken(MyStreamTokenizer parms)
 throws MyIOException,MyParseException
 {
  parms.ungetToken();
  String token = parms.getToken();
  if (Debug)Utilities.debug("StructuredMessageFormatter.ungetToken note token{"
                           +"token="+token+"}");
  parms.ungetToken();
  if (!parms.isSeparator(token))return; // It wasn't a tab.
  // It was a tab. Go back one more. it is either a value or tab. Doesn't matter. We're ready to get it again.
  parms.ungetToken();
  if (Debug)Utilities.debug("StructuredMessageFormatter.ungetToken note 2nd ungetToken");
 }
}

