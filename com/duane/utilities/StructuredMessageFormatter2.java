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


public class StructuredMessageFormatter2
{
 private boolean           Debug           = false;
 private MyResult          EmptyResult     = new MyResult();
 private MyStreamTokenizer Parms           = null;
 private int               ShortBlockDepth = 15;

 private class MyResult
 {
  private StringBuffer Result = null;

  public MyResult(){}
  public MyResult(StringBuffer result){Result=result;}

  public MyResult append(String s)
  {
   if (Result != null)Result.append(s);
   return this;
  }

  public int getCurrentLineLength()
  {
   if (isNull())return 0;
   int offset=Result.lastIndexOf("\n");
   if (offset == -1)return Result.length();
   return Result.length()-(offset+1);
  }

  public boolean isNull()
  {
   return Result == null;
  }

  public String toString()
  {
   if (Result != null)return Result.toString();
   return "";
  }
 }

 private String doShortBlock(MyResult result)
 throws MyIOException,MyParseException
 {
  if (Debug)Utilities.debug("StructuredMessageFormatter2.doShortBlock enter");
  String lagToken = "{";
  for (int i=0;i<ShortBlockDepth;i++)
      {
       String token = getToken();
       switch (token)
              {
               case "{":
               case "}":
               case "=":
               case ":":
                    break;
               default:
                    switch (lagToken)
                           {
                            case "{":
                            case "}":
                            case "=":
                            case ":":
                                 break;
                            default:
                                 result.append(" ");
                           }
           }
       lagToken = token;
       result.append(token);
       if (token == null){throw new MyParseException("StructuredMessageFormatter2.doShortBlock Unexpected EOF");}
       if (token.equals("}"))
          {
           if (Debug)Utilities.debug("StructuredMessageFormatter2.doShortBlock return");
           return result.toString();
          }
      }
  throw new MyParseException("StructuredMessageFormatter2.doShortBlock short block not found");
 }

 public String format(String message)
 throws MyIOException, MyParseException
 {
  if (Debug)Utilities.debug("StructuredMessageFormatter2.format note message{"
                           +"message="+message+"}");
  return format(message, 0);
 }

 public String format(String message,int indent)
 throws MyIOException, MyParseException
 {
  if (message == null)return null;
  Parms = new MyStreamTokenizer(new StringReader(message), null);
  Parms.setDebug          (false                               );
  Parms.setSymbolChars    (new char[]{'\t', '{', '}', ':', '='});
  Parms.setSeparatorChars (new char[]{'\t'                    });
  Parms.setWhitespaceChars(new char[]{                        });
  MyResult result = new MyResult(new StringBuffer());
  parseBlock(indent,result);
  String token  = getToken();
  if (token != null)Utilities.error("StructuredMessageFormatter2.format trailing data not formatted{"
                                   +"token="+token+"}");
  return result.toString();
 }

 private String getToken()
 throws MyIOException,MyParseException
 {
  String token = getToken_();
  if (token!=null&&token.startsWith("\"") && token.endsWith("\""))token = token.substring(1,token.length()-1);
  if (Debug) Utilities.debug("StructuredMessageFormatter2.getToken note token{"
                            +"token=" +token                              +"\t"
                            +"offset="+Parms.getCurrentTokenOffsetInLine()+"\t"
                            +"nest="  +Utilities.nest()                   +"}");
  return token;
 }

 private String getToken_()
 throws MyIOException,MyParseException
 {
  String token = Parms.getToken();
  if (token == null)return null;
  if (!Parms.isSeparator(token))return token;
  String nextToken = Parms.getToken();
  if (nextToken == null)return null;
  if (Parms.isSeparator(nextToken))return "";
  return nextToken;
 }

 private String getTokenOrDie()
 throws MyIOException,MyParseException
 {
  String result = getToken();
  if (result == null)throw new MyParseException("StructuredMessageFormatter2.getTokenOrDie unexpected EOF");
  return result;
 }

 private boolean isShortBlock()
 throws MyIOException,MyParseException
 {
  if (Debug)Utilities.debug("StructuredMessageFormatter2.isShortBlock enter");
  int equalsCount = 0;
  int i = 0;
  Boolean result = null;
  forBlock:
  for (i=0;i<ShortBlockDepth;i++)
      {
       String token = getToken();
       if (token == null){ungetTokens(i);return false;}  // EOF
       switch (token)
              {
               case "{" :                                   result = false;break forBlock; 
               case "}" :                                   result = true ;break forBlock; 
               case "=" :equalsCount++;if (equalsCount > 2){result = false;break forBlock;}
              }
      }
  if (result!=null)
      ungetTokens(i+1);
  else
     {
      ungetTokens(i);
      result = false;
     }
  if (Debug)Utilities.debug("StructuredMessageFormatter2.isShortBlock return{"
                           +"result="+result+"}");
  return result;
 }

 // calls other parseBlock twice. Once to get the name lengths. Again to get the result.

 private int parseBlockDepth=0;

 private void parseBlock(int indent, MyResult result)
 throws MyIOException, MyParseException
 {
  parseBlockDepth++;
  long id = 0;
  if (!result.isNull())
      {
       Parms.lockCurrentToken();
       id  = Parms.getCurrentTokenId();
      }
  if (Debug)Utilities.debug("StructuredMessageFormatter2.parseBlock parent first{"
                           +"id="    +id                                 +"\t"
                           +"offset="+Parms.getCurrentTokenOffsetInLine()+"\t"
                           +"depth=" +parseBlockDepth                    +"}");

  int  maxNameLength = parseBlock(indent,EmptyResult,0);
  if (!result.isNull())
     {
      Parms.setCurrentToken(id);
      Parms.unlockCurrentToken();
      if (Debug)Utilities.debug("StructuredMessageFormatter2.parseBlock parent second{"
                               +"id="    +id                                 +"\t"
                               +"offset="+Parms.getCurrentTokenOffsetInLine()+"\t"
                               +"depth=" +parseBlockDepth                    +"}");
      parseBlock(indent,result,maxNameLength);
     }
  if (Debug)Utilities.debug("StructuredMessageFormatter2.parseBlock parent return{"
                           +"id="    +id                                 +"\t"
                           +"offset="+Parms.getCurrentTokenOffsetInLine()+"\t"
                           +"depth=" +parseBlockDepth                    +"}");
  parseBlockDepth--;
 }

// callee is responsible for opening brace to closing brace only.
// caller is responsible for leading and trailing data (indent spaces and new lines).

 private int parseBlock(int indent, MyResult result, int maxNameLength)
 throws MyIOException,MyParseException
 {
  boolean foundBrace = false;
  mainParseLoop:
  for (String token=getToken();token!=null;token=getToken())
      {
       if (Debug)Utilities.debug("StructuredMessageFormatter2.parseBlock top of loop{"
                                +"indent="+indent+"\t"
                                +"token=" +token +"}");
       String token2 = null;
       String token3 = null;
       String token4 = null;
       String token5 = null;
       switch (token)
              {
               case "{":
                   if (foundBrace) // nested brace as in {name:{ or {...}= 
                      {
                       ungetToken();
                       result.append(Utilities.spaces(indent));
                       parseBlock(indent,result);
                       token2 = getToken();
                       if (token2 == null)return maxNameLength;
                       switch (token2)
                              {
                               case "=":
                                    result.append(token2);
                                    token3 = getTokenOrDie();
                                    switch (token3)
                                           {
                                            case "{":          // {{...}={
                                                 int offset = result.getCurrentLineLength();
                                                 ungetToken();
                                                 parseBlock(offset,result);
                                                 result.append("\n");
                                                 break;
                                            default:           // {...}=value
                                                 result.append(token3).append("\n");
                                           }
                                    continue;
                               default:  result.append("\n"); // {{...}...
                              }
                       ungetToken();
                       continue;
                      }
                    indent ++; // first brace. Rest is indented 1 space.
                    result.append(token);
                    if (isShortBlock())
                       {
                        doShortBlock(result);
                        return maxNameLength;
                       }
                    foundBrace = true;
                    token2 = getTokenOrDie();
                    switch (token2)
                           {
                            case "{":  // could be name={{
                                 ungetToken();
                                 result.append("\n").append(Utilities.spaces(indent));
                                 parseBlock(indent,result);
                                 result.append("\n");
                                 break;
                            case "}": // {} should be impossible. isShortBlock would have caught it.
                                 Utilities.error("StructuredMEssageFormatter2.parseBlock impossible case{}");
                                 result.append(token2).append("\n");
                                 return maxNameLength;
                            case "=": throw new MyParseException("StructuredMessageFormatter2.parseBlock illegal equals{"
                                                                +"Offset="+Parms.getCurrentTokenOffsetInLine()+"}");
                            case ":": throw new MyParseException("StructuredMessageFormatter2.parseBlock illegal colon{"
                                                                +"Offset="+Parms.getCurrentTokenOffsetInLine()+"}");
                            default:
                                 token3 = getTokenOrDie();
                                 switch (token3)
                                        {
                                         case ":": // {name:
                                              if (Debug)Utilities.debug("StructuredMessageFormatter2.parseBlock found {name:");
                                              result.append(token2).append(token3).append("\n");
                                              continue;
                                         default:
                                              // it's a list or name thing
                                              ungetToken();  // get rid of token3
                                              ungetToken();  // get rid of token2
                                              result.append("\n");
                                              break;
                                        }
                           }
                     break;

               case "}":
//                    if (!foundBrace)throw new MyParseException("StructuredMessageFormatter2.parseBlock found spurious close brace{"
//                                                              +"offset="+Parms.getCurrentTokenOffsetInLine()+"}");
                    indent --;
                    result.append(Utilities.spaces(indent)).append(token);
                    return maxNameLength;

               case "=":  //e.g. {...} = {...}
                    result.append(token);
                    break;

               default: // name ...
                   if (Debug)Utilities.debug("StructuredMessageFormatter2.parseBlock found name{"
                                            +"indent="+indent+"\t"
                                            +"name="  +token +"}");
                   result.append(Utilities.spaces(indent)).append(token);
                   token2 = getToken();
                   if (token2 == null)
                       return maxNameLength;
                   switch (token2)
                          {
                           case "=":                                                                           // name=
                                if (maxNameLength < token.length())maxNameLength = token.length();
                                result.append(Utilities.spaces(maxNameLength-token.length())).append(token2);
                                token3 = getTokenOrDie();
                                switch (token3)
                                       {
                                        case "{":                                                              // name={
                                             ungetToken();
                                             parseBlock(indent+maxNameLength+1,result);
                                             result.append("\n");
                                             break; 
                                        case "}":                                                              // name=}
                                             result.append(token3);
                                             return maxNameLength;
                                        default:                                                               // name=value 
                                             result.append(token3).append("\n");
                                             while (true)
                                                   {                                                           // name=value     token=token3
                                                    token3 = getToken();                                       //      value           token3
                                                    if (token3 == null)return maxNameLength;                   //      ...             ...
                                                    switch (token3)
                                                           {
                                                            case "{": ungetToken(); continue mainParseLoop;  // would be an error
                                                            case "}": ungetToken(); continue mainParseLoop;
                                                            default:
                                                                 token4 = getToken();
                                                                 if (token4 == null)
                                                                    {
                                                                     result.append(Utilities.spaces(indent + maxNameLength + 1)).append(token3).append("\n");
                                                                     return maxNameLength;
                                                                    }
                                                                 switch (token4)
                                                                        {
                                                                         case "=":
                                                                              ungetToken(); // token 4  =
                                                                              ungetToken(); // token 3  value
                                                                              continue mainParseLoop;
                                                                         case "}":
                                                                              result.append(Utilities.spaces(indent + maxNameLength + 1)).append(token3).append("\n");
                                                                              ungetToken(); // token 4
                                                                              continue mainParseLoop;
                                                                         default:                               // name 
                                                                              result.append(Utilities.spaces(indent + maxNameLength + 1)).append(token3).append("\n");
                                                                              ungetToken(); // token 4  Will pick it up as token 3 next time around.
                                                                        }
                                                           }
                                                   }
                                       }
                                break;
                           default: // name name ...
                                ungetToken(); // get rid of second name
                                result.append("\n");
                          }
                   break;
              }
      }
  if (foundBrace)throw new MyParseException("StructuredMessageFormatter.parseBlock Unexpected EOF{"
                                                  +"result="+result.toString()+"}");
  return maxNameLength;
 }

 public StructuredMessageFormatter2(){}

 private void ungetTokens(int n)
 throws MyIOException,MyParseException
 {
  for (int i=0;i<n;i++)ungetToken();
 }

 private void ungetToken()
 throws MyIOException,MyParseException
 {
  Parms.ungetToken();
  String token = Parms.getToken();
  if (Debug)Utilities.debug("StructuredMessageFormatter2.ungetToken note token{"
                           +"token="+token           +"\t"
                           +"nest=" +Utilities.nest()+"}");
  Parms.ungetToken();
  if (!Parms.isSeparator(token))return; // It wasn't a tab.
  // It was a tab. Go back one more. it is either a value or tab. Doesn't matter. We're ready to get it again.
  Parms.ungetToken();
  if (Debug)Utilities.debug("StructuredMessageFormatter2.ungetToken note 2nd ungetToken");
 }
}
