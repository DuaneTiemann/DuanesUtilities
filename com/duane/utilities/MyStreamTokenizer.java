package com.duane.utilities;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import com.duane.utilities.MyParseException;
import com.duane.utilities.MyPair;
import com.duane.utilities.Utilities;

// Handle /* */, // comments.  Caller will not see them.
// double quoted strings are transparent
// It is expected that strings will be joined by \, but that is not peformed in this class.  
// \ is just a token.  It is never a transparency character unless it is in a double quoted string.
// EOL may be \r, \n, \r\n, \n\r or EOF
// EOL is whitespace except for // comments
// /* ... */ comments end with first */ regardless of quotes.  i.e. any mess can be between /* and */

// StreamTokenizer would have worked just fine, but I didn't use it becasue I wanted to be able to provide token offset in line.  
// StreamTokenizer only provides line number.

// Issues
// 1. What should MyParseException do with chained exceptions?  A: Utility method to print exceptions.
// 2. Should internal # be accepted as a comment character?  A: NO
// 3. Should separators be handled here or by caller? A: By caller because meaning is different for lists and name=value

// Subtleties
// Buffer management. 
// I don't intend for this to be used to process huge amounts of data so I haven't worried too much about speed.
// However, I do want to be able to process huge files without crashing, just in case.
// There are 2 situations that could mess us up. 
// 1. Input files could be so large that they can not be stored in the JVM.  I've dealt with this by trimming processed data from the buffer.
//    I also keep a limited cache of tokens to facilitate ungetting rather than an entire history.
// 2. Huge input files could be mostly commented out.  I've dealt with this by deleting full contiguous commented lines.
// Item 2 seems to be isolated to just the comment code.  Item 1 is not isolated.  Buffer offsets are used in many places and can easily become stale due to trimming for item 1. 
// I've tried to organize this, but it is still awkward.  I've introduced the concept of local and global buffer offsets.  The local offset is the current offset in the current buffer.
// The global offset is the offset that would exist without item 1 trimming.  Note that the deleting of comments for item 2 do not have to be considered for this.  i.e. both offsets 
// treat the deleted comments as if they never existed.  Thus global offset will not be the same as offset in the file if it has multi-line comments.
// getBufferGlobalOffset converts local to global. getBufferLocalOffset does the opposite. They're hokey, but do serve to keep the accommodations more isolated.
//
// Token history contains global offsets so it does not need to be updated as part of trimming.
//
// Note that huge single line files will mess this up.


public class MyStreamTokenizer
{
 public static class Parms
 {
  public Reader Reader  ;
  public String FileName;
  public Parms(Reader reader,String fileName){Reader=reader;FileName=fileName;}
 }

 private class TokenInfo
 {
  private long    Id;
  private long    LineNo;
  private long    LineStartGlobalOffset;
  private boolean Locked=false;
  private boolean IsSymbol; // In order to distinguish { from "{", etc.
  private String  Token;
  private int     TokenOffsetInLine;
  public long     getId                   (         ){return Id                   ;}
  public long     getLineNo               (         ){return LineNo               ;}
  public long     getLineStartGlobalOffset(         ){return LineStartGlobalOffset;}
  public String   getToken                (         ){return Token                ;}
  public int      getTokenOffsetInLine    (         ){return TokenOffsetInLine    ;}
  public boolean  isLocked                (         ){return Locked               ;}
  public boolean  isSymbol                (         ){return IsSymbol             ;}
  public void     lock                    (         ){Locked=true                 ;}
//public void     setLineStartOffset      (int value){LineStartOffset=value       ;}
  public void     unlock                  (         ){Locked=false                ;}

  public TokenInfo(long lineNo, long lineStartGlobalOffset, boolean isSymbol, String token, int tokenOffsetInLine, long id)
  {
   Id                    = id;
   IsSymbol              = isSymbol;
   LineNo                = lineNo;
   LineStartGlobalOffset = lineStartGlobalOffset;
   Token                 = token;
   TokenOffsetInLine     = tokenOffsetInLine;
  }

  public String toString()
  {
   return "{TokenInfo:"
         +"IsSymbol="             +IsSymbol             +"\t"
         +"LineNo="               +LineNo               +"\t"
         +"LineStartGlobalOffset="+LineStartGlobalOffset+"\t"
         +"Token="                +Token                +"\t"
         +"TokenOffsetInLine="    +TokenOffsetInLine    +"}";
  }
 }

 protected StringBuffer            Buffer                              = new StringBuffer();
 protected long                    CurrentLineNo                       = 1;
 protected int                     CurrentLineStartLocalOffsetInBuffer = 0;
 protected int                     CurrentLocalOffsetInBuffer          = 0;
 protected TokenInfo               CurrentTokenInfo                    = null;
 protected boolean                 Debug                               = false;
 private   char[]                  EOLChars                            = {'\r', '\n'};
 private   TokenInfo               FirstTokenInfo                      = null;
 private   int                     MaxHistoryDepth                     = 5;
 private   long                    NextTokenId                         = 0;
 private   int                     NTokenInfos                         = 0;
 private   int                     ReadBufferSize                      = 8192;
 private   char[]                  SeparatorChars                      = {',', ';'};
 private   Parms[]                 Source                              = new Parms[0];
 private   int                     SourceNo                            = 0;
 private   HashMap<String,String>  Substitutions                       = new HashMap<String,String>();
 private   char[]                  SymbolChars                         = {'\\', '{', '}', '[', ']', '(', ')', '=', ',', ';'};
 private   LinkedList  <TokenInfo> TokenHistory                        = new LinkedList<TokenInfo>();
 private   ListIterator<TokenInfo> TokenHistoryIterator                = null;
 private   long                    TotalAmountTrimmedFromBuffer        = 0;
 // The ListIterator cursor is always between elements.  previous and next are with respect to the inbetween position.
 // Our cursor is on the current element.  get and unget are with respect to that element.  We will maintain the iterator cursor after the current element.
 // Thus next gets the next element and previous previous gets the previous element.
 private   char[]                  WhitespaceChars                     = {' ', '\t'};

 private void advancePastComment()
 throws MyIOException, MyParseException
 {
  if (!isComment())throw new MyParseException("advancePastComment Internal error. Not positioned at comment.{"
                                             +"where="+where()+"}");
  char c = getCharOrDie();
  switch (c)
         {
          case '/':
               if (!moreData())throw new MyParseException("advancePastComment Internal error. Unexpected EOF{"
                                         +"where="+where()+"}");
               char c2 = peekChar();
               switch (c2)
                      {
                       case '/': advancePastEOL()      ;return;
                       case '*': advancePastStarSlash();return;
                       default: throw new MyParseException("advance Past Comment Internal error. Not / or * after /.{"
                                                          +"where="+where()+"}");
                      }
          default:
               throw new MyParseException("advancePastComment Internal error. Not positioned at comment2.{"
                                                          +"where="+where()+"}");
         }
 }

 private void advancePastComments()
 throws MyIOException, MyParseException
 {
  while (true)
        {
         advancePastWhitespace();
         if (isComment())advancePastComment(); //Right now, this is only called from getNontransparentString which is only called from getToken_ which calls advancePastComment first.
                                               //so it is currently impossible.
         else            break;
        }
 }

 private void advancePastEOL()
 throws MyIOException, MyParseException
 {  // EOL is \r, \n, \r\n, \n\r or EOF
  advanceToEOL();
  if (!moreData())return; // EOF
  char eolChar                        = getCharOrDie();
  if (!moreData())return; // EOF
  char nextChar                       = peekChar();  // can not be EOF
  if (isEOLChar(nextChar) && (nextChar != eolChar))getCharOrDie();
  CurrentLineNo                      ++;
  CurrentLineStartLocalOffsetInBuffer = CurrentLocalOffsetInBuffer;
 }

 private void advancePastNontransparentData()
 throws MyIOException, MyParseException
 { // stops before whitespace or EOL.
  while(moreData())
       {
        char c = peekChar();
        if (isWhitespaceChar(c)    )return;
        if (isEOLChar       (c)    )return;
        if (isSymbolChar    (c)    )return;
        if (c == '/' && isComment())return;
        getCharOrDie();
       }
 }

 private void advancePastStarSlash()
 throws MyIOException, MyParseException
 {
  long   saveCurrentLineNo         = CurrentLineNo;
  int    saveCurrentOffsetInLine   = CurrentLocalOffsetInBuffer - CurrentLineStartLocalOffsetInBuffer -1;
  String saveCurrentLine           = getCurrentLine();
                                   
  long   numberOfLines             = 0;
  long   lineStartGlobalOffset     = getBufferGlobalOffset(CurrentLineStartLocalOffsetInBuffer);
  long   lastLineStartGlobalOffset = lineStartGlobalOffset;
  while (moreData())
        {
         char c = getCharOrDie();
         if (isEOLChar(c))
            {
             advancePastEOL();
             lastLineStartGlobalOffset = lineStartGlobalOffset;
             lineStartGlobalOffset     = getBufferGlobalOffset(CurrentLocalOffsetInBuffer);
             numberOfLines++;
             if (numberOfLines >= 2)
                {
                 Buffer.delete(getBufferLocalOffset(lastLineStartGlobalOffset),
                               getBufferLocalOffset(lineStartGlobalOffset    )); // delete prior line.  It was entirely a comment.  The concern is that a huge file could be mostly commentted out.
        
                 CurrentLocalOffsetInBuffer -= (int)(lineStartGlobalOffset - lastLineStartGlobalOffset);
                 lineStartGlobalOffset       = getBufferGlobalOffset(CurrentLocalOffsetInBuffer);
                }
             continue;
            }
         if (c == '*')
            {
             if (!moreData())
                 throw new MyParseException("Matching */ not found {"
                                           +"where="+where(saveCurrentLineNo,saveCurrentLine,saveCurrentOffsetInLine)+"}");
             if (peekChar()=='/'){getCharOrDie();return;}
            }
        }
  throw new MyParseException("Matching */ not found{"
                            +"where="+where(saveCurrentLineNo,saveCurrentLine,saveCurrentOffsetInLine)+"}");
 }

 protected void advancePastWhitespace()
 throws MyIOException, MyParseException
 {
  while (moreData())
        {
         char c = peekChar();
         if (isEOLChar        (c)){advancePastEOL();continue;}
         if (!isWhitespaceChar(c))return;
         getCharOrDie();
        }
 }

 private void advanceToEOL()
 throws MyIOException, MyParseException
 {
  CurrentLocalOffsetInBuffer = getEOLLocalOffset(CurrentLocalOffsetInBuffer);
 }

 private void checkEOD()
 throws MyIOException, MyParseException
 {
  checkEOD("Unexpected end of data");
 }

 private void checkEOD(String message)
 throws MyIOException, MyParseException
 {
  checkEOD(message,CurrentLocalOffsetInBuffer);
 }

 private void checkEOD(String message,int startPos)
 throws MyIOException, MyParseException
 {
  if (moreData())return;
  throw new MyParseException(message+"{"
                          +"where="+where()+"}");
 }

 private void dumpData(String title)
 {
  if (!(Debug&&Utilities.debug()))return;
  dumpData_(title);
 }

 private void dumpData_(String title)
 {
  String msg = title + "{"
              +"Buffer="                             +Buffer.toString()                  +"\t"
              +"CurrentLocalOffsetInBuffer="         +CurrentLocalOffsetInBuffer         +"\t"
              +"CurrentLineStartLocalOffsetInBuffer="+CurrentLineStartLocalOffsetInBuffer+"\t"
              +"TokenHistory="                       +Utilities.toString(TokenHistory)   +"}";
  System.err.println(msg);
 }

 public void eraseCurrentToken()
 throws MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("MyStreamTokenizer.eraseCurrentToken internal error. CurrentTokenInfo is null");

  TokenHistoryIterator = null;

  for (;;)
      {
       TokenInfo tokenInfo = TokenHistory.removeLast();
       if (tokenInfo == null)
          {
           System.err.println("MyStreamTokenizer.eraseCurrentToken CurrentTokenInfo not found in TokenHistory");
           throw new MyParseException("MyStreamTokenizer.eraseCurrentToken CurrentTokenInfo not found in TokenHistory");
          }

       if (tokenInfo == CurrentTokenInfo)break;
      }

  CurrentLineNo                       = CurrentTokenInfo.getLineNo();
  CurrentLineStartLocalOffsetInBuffer = getBufferLocalOffset(CurrentTokenInfo.getLineStartGlobalOffset());
  CurrentLocalOffsetInBuffer          = CurrentLineStartLocalOffsetInBuffer+CurrentTokenInfo.getTokenOffsetInLine();
  CurrentTokenInfo                    = TokenHistory.size()==0?null:TokenHistory.getLast();
 }

 protected long getBufferGlobalOffset(int  value)
 throws MyParseException
 {
  if (value < 0)throw new MyParseException("getBufferGlobalOffset internal error value < 0{"
                                          +"value="+value+"}");
  return value + TotalAmountTrimmedFromBuffer;
 }

 protected int getBufferLocalOffset(long value)
 throws MyParseException
 {
  if (value < TotalAmountTrimmedFromBuffer)throw new MyParseException("getBufferLocalOffset internal error. value < TotalAmountTrimmedFromBuffer{"
                                                                     +"value="+value+"}");
  int result = (int)(value - TotalAmountTrimmedFromBuffer);

  if (result > Buffer.length()            )throw new MyParseException("getBufferLocalOffset internal error. result > Buffer length{"
                                                                     +"Buffer.length="+Buffer.length()+"\t"
                                                                     +"result="       +result         +"\t"
                                                                     +"value="        +value          +"}");
  return result;
 }

 private char getCharOrDie()
 throws MyIOException, MyParseException
 {
  if (!moreData())throw new MyParseException("getCharOrDie Internal error. Unexpected EOF"
                                            +"where="+where()+"}");
  return Buffer.charAt(CurrentLocalOffsetInBuffer++);
 }

 public String getCurrentLine()
 throws MyIOException, MyParseException
 {
  return getLine(CurrentLineStartLocalOffsetInBuffer);
 }

 private long getCurrentLocalOffsetInBuffer(){return CurrentLocalOffsetInBuffer;}

 public long getCurrentTokenId()
 throws MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("MyStreamTokenizer.getCurrentTokenID internal error. CurrentTokenInfo is null");
  return CurrentTokenInfo.getId();
 }

 public long getCurrentTokenLineNo()
 throws MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("MyStreamTokenizer.getCurrentTokenLineNo internal error. CurrentTokenInfo is null");
  return CurrentTokenInfo.getLineNo();
 }

 public int getCurrentTokenOffsetInLine()
 throws MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("MyStreamTokenizer.getCurrentTokenOffsetInLine internal error. CurrentTokenInfo is null");
  return CurrentTokenInfo.getTokenOffsetInLine();
 }

 private int getEOLLocalOffset(int startLocalOffsetInBuffer)
 throws MyIOException, MyParseException
 {
//  if (Debug&&Utilities.debug())System.err.println("getEOLLocalOffset{"
//                                          +"Buffer.length="           +Buffer.length()         +"\t"
//                                          +"startLocalOffsetInBuffer="+startLocalOffsetInBuffer+"\t"
//                                          +"nest="                    +Utilities.nest()        +"}");
  long globalPos = 0;
  int  localPos  = 0;
  for (localPos=startLocalOffsetInBuffer, globalPos=getBufferGlobalOffset(localPos);
        localPos<Buffer.length() || moreData(localPos);
         globalPos++,localPos=getBufferLocalOffset(globalPos))
      {
       localPos = getBufferLocalOffset(globalPos);  // moreData may have been the last thing to run before entering this block. localPos could be stale.
       if (isEOLChar(Buffer.charAt(localPos)))return localPos;
      }
  return Buffer.length();
 }

 private String getLine(int lineStartLocalOffset)
 throws MyIOException, MyParseException
 {
  long lineStartGlobalOffset = getBufferGlobalOffset(lineStartLocalOffset);
  int  eolLocalOffset        = getEOLLocalOffset    (lineStartLocalOffset);
  return Buffer.substring(getBufferLocalOffset(lineStartGlobalOffset),eolLocalOffset);
 }

 private int getLineStartLocalOffset(int localOffset)
 throws MyParseException
 {
  if (localOffset > Buffer.length())throw new MyParseException("getLineStartLocalOffset internal error localOffset > buffer length{"
                                                               +"localOffset="+localOffset+"}");
  for (int pos=localOffset-1; pos>=0; pos--)
       if (isEOLChar(Buffer.charAt(pos)))return pos+1;
  return 0;
 }

 private long getLineNo(int localOffset)
 {
  return 0;
 }

 private String getNontransparentString()
 throws MyIOException, MyParseException
 {
  if (Debug&&Utilities.debug())System.err.println("getNontransparentString entry note CurrentLocalOffsetInBuffer{"
                                          +"CurrentLocalOffsetInBuffer="+CurrentLocalOffsetInBuffer+"}");
  advancePastWhitespace();
  advancePastComments  ();
  advancePastWhitespace();
  if (!moreData())throw new MyParseException("getNontransparentString Internal error. unexpected EOF{" // This shouldn't be possible because getToken did a peekChar and got something
                                            +"where="+where()+"}");                                    // that wasn't a symbol.
  long startGlobalOffset = getBufferGlobalOffset(CurrentLocalOffsetInBuffer);
  advancePastNontransparentData();
  if (Debug&&Utilities.debug())System.err.println("getNontransparentString exit note CurrentLocalOffsetInBuffer{"
                                          +"CurrentLocalOffsetInBuffer="+CurrentLocalOffsetInBuffer+"}");
  return Buffer.substring(getBufferLocalOffset(startGlobalOffset),CurrentLocalOffsetInBuffer);
 }

 /*  no longer used
 private String getString(int size)
 throws MyIOException, MyParseException
 {                       
  long startGlobalOffset = getBufferGlobalOffset(CurrentLocalOffsetInBuffer);
  int  endLocalOffset    = CurrentLocalOffsetInBuffer+size;

  for (long endGlobalOffset = getBufferGlobalOffset(endLocalOffset);
        endLocalOffset > Buffer.length() && moreData(endLocalOffset);
         endLocalOffset = getBufferLocalOffset(endGlobalOffset));

  if (endLocalOffset > Buffer.length())throw new MyParseException("getString unexpected EOF{"
                                                                 +"where="+where()+"}");
  CurrentLocalOffsetInBuffer = endLocalOffset;
  return Buffer.substring(getBufferLocalOffset(startGlobalOffset),endLocalOffset);
 }

 private String getStringToEOL()
 throws MyIOException, MyParseException
 {
  StringBuffer result                    = new StringBuffer();
  advancePastWhitespace();
  int          startLocalOffsetInBuffer = CurrentLocalOffsetInBuffer;
  advancePastEOL();
  int          endLocalOffsetInBuffer   = CurrentLocalOffsetInBuffer;
  return Buffer.substring(startLocalOffsetInBuffer,endLocalOffsetInBuffer).trim();
 }
*/

 public HashMap<String,String> getSubstitutions()
 {
  return Substitutions;
 }

 public String getToken()
 throws MyIOException, MyParseException
 {
  String token = getToken_();
  String substitution = Substitutions.get(token);
  if (Debug&&Utilities.debug())
     {
      System.err.println("getToken note token{"
                        +"FileName="    +Source[SourceNo].FileName+"\t"
                        +"substitution="+substitution             +"\t"
                        +"token="       +token                    +"\t"
                        +"nest="        +Utilities.nest()         +"}");
      System.err.println("getToken note TokenHistory{"
                        +"TokenHistory="+Utilities.toString(TokenHistory)+"}");
      if (TokenHistoryIterator != null)
          System.err.println("getToken note TokenHistoryIterator{"
                            +"TokenHistoryIterator.nextIndex="+TokenHistoryIterator.nextIndex()+"}");
     }

  if (substitution != null)token = substitution;
  return token;
 }

 public String getToken_()
 throws MyIOException, MyParseException
 {
  String token = getTokenFromHistory();
  if (token != null)return token;

  // this method is entered at the beginning of a token only.  Finding the end is subordinate to this method.
  while (true)
  {
   advancePastWhitespace();
//   if (Utilities.debug())System.err.println("getToken_ note CurrentOffsetInBuffer after advancePastWhiteSpace{"
//                                           +"CurrentOffsetInBuffer="+CurrentOffsetInBuffer+"}");
   if (!moreData())return null;

   if (isComment()){advancePastComment();continue;}

   long    currentLineNo                        = CurrentLineNo                                             ;
   long    currentLineStartGlobalOffsetInBuffer = getBufferGlobalOffset(CurrentLineStartLocalOffsetInBuffer);
   long    currentGlobalOffsetInBuffer          = getBufferGlobalOffset(CurrentLocalOffsetInBuffer         );
   boolean isSymbol                             = false                                                     ;
   char    c                                    = peekChar()                                                ;
   if (isSymbolChar(c)){getCharOrDie();token = new String(new char[]{c});isSymbol=true;}else
   if (isEOLChar   (c)){advancePastEOL();continue;                                     }else  // This isn't possible since advamceWhiteSpace should have covered it.
   switch(c)
         {
          case '\"': token = getTransparentString   ();break;
          default:   token = getNontransparentString();break;
         }
   registerToken(currentLineNo                                                          ,
                 currentLineStartGlobalOffsetInBuffer                                   ,
                 isSymbol                                                               ,
                 token                                                                  ,
                 (int)(currentGlobalOffsetInBuffer-currentLineStartGlobalOffsetInBuffer));
   return token;
  }
 }

 protected String getTokenFromHistory()
 {
  if (TokenHistoryIterator == null)return null;
  if (TokenHistoryIterator.hasNext())
     {
      CurrentTokenInfo = TokenHistoryIterator.next();
      if (Debug&&Utilities.debug())System.err.println("MyPtxStreamTokenizer.getToken_ get token from history{"
                                              +"token="+CurrentTokenInfo.getToken()+"}");
      return CurrentTokenInfo.getToken();
     }
  TokenHistoryIterator = null;
  return null;
 }

 private TokenInfo getTokenInfo(long id)
 throws MyParseException
 {
  if (Debug&&Utilities.debug())System.err.println("getTokenInfo{"
                                                 +"nest="+Utilities.nest()+"}");
  ListIterator<TokenInfo> tokenHistoryIterator = TokenHistory.listIterator();
  while (tokenHistoryIterator.hasNext())
        {
         TokenInfo tokenInfo = tokenHistoryIterator.next();
         if (tokenInfo.getId()==id)return tokenInfo;
        }
  throw new MyParseException("MyStreamTokenizer.getTokenInfo internal error. id not found{"
                            +"id="+id+"}");
 }

 public String getTokenOrDie()
 throws MyIOException, MyParseException
 {
  String result = getToken();
  if (result == null)throw new MyParseException("MyStreamTokenizer.getTokenOrDie unexpected EOF{"
                                               +"where="+where()+"}");
  return result;
 }

//  private static char[] TransparentChars={'\b', '\f', '\r', '\n', '\t', '\"', '\\'};
//  public static String localQuote(String source)
//  throws MyParseException
//  {
// //  if (debug())System.err.println("Utilities.quote note source{"
// //                                +"source="+source+"}");
//   if (source == null          )return null;
//   StringBuffer result = new StringBuffer("\"");
//   int pos = 0;
//   for (int newPos=indexOf(source,pos,TransparentChars);newPos!=-1;pos=newPos+1,newPos=indexOf(source,pos,TransparentChars))
//       {
// //       if (debug())System.err.println("Utilities.quote{"
// //                                     +"newPos="+newPos+"\t"
// //                                     +"pos="   +pos   +"}");
//        result.append(source.substring(pos, newPos));
//        switch (source.charAt(newPos))
//               {
//                case '\b' : result.append("\\b" );continue;
//                case '\f' : result.append("\\f" );continue;
//                case '\r' : result.append("\\r" );continue;
//                case '\n' : result.append("\\n" );continue;
//                case '\t' : result.append("\\t" );continue;
//                case '\"' : result.append("\\\"");continue;
//                case '\\' : result.append("\\\\");continue;
//                default   : throw new MyParseException("Utilities.quote internal error. unexpected char{"
//                                                      +"char="  +source.charAt(newPos)+"\t"
//                                                      +"newPos="+newPos               +"\t"
//                                                      +"pos="   +pos                  +"\t"
//                                                      +"source="+source               +"}");
//               }
//       }
//   result.append(source.substring(pos,source.length())).append("\"");
//   return result.toString();
//  }


 protected String getTransparentString()
 throws MyIOException, MyParseException
 {
  StringBuffer result                    = new StringBuffer();
  long         startGlobalOffsetInBuffer = getBufferGlobalOffset(CurrentLocalOffsetInBuffer);
  if (getCharOrDie() != '\"')throw new MyParseException("Internal error. getTransparentString not at double quote{"
                                                  +"where="+where()+"}");
  if (Debug)System.err.println("getTransparentString note Buffer{"
                              +"Buffer="+Buffer.toString()+"}");
  while (moreData())
        {
         char c = getCharOrDie();
//         if (Debug)System.err.println("getTransparentString{"
//                                     +"c="+c+"}");
         switch (c)
                {
                 case '\\': 
                      if (!moreData())throw new MyParseException("getTransparentString matching double quote not found{"
                                                                +"where="+where(getBufferLocalOffset(startGlobalOffsetInBuffer))+"}");
                      char c2 = getCharOrDie();
                      result.append(c );
                      result.append(c2);
                      continue;
                 case '"' : return "\""+result.toString()+"\"";
                 case '\n':
                 case '\r': throw new MyParseException("Double quoted string not terminated before EOL{"
                                                      +"where="+where(getBufferLocalOffset(startGlobalOffsetInBuffer))+"}");
                 default  :
                      result.append(c);
                }
         if (result.length() > 8192)
            {
             System.err.println("MyStreamTokenizer.getTransparentString result>8192{"
                               +"result="+result.toString()+"}");
             throw new MyParseException("MyStreamTokenizer.getTransparentString result>8192");
            }
        }
  throw new MyParseException("Double quoted string not terminated before EOF{"
                            +"where="+where(getBufferLocalOffset(startGlobalOffsetInBuffer))+"}");
 }

 private boolean isComment()
 throws MyIOException, MyParseException
 {
  if (!moreData())return false;
  char c1 = peekChar();
  if (c1 != '/')return false;
  getCharOrDie();
  if (!moreData()){ungetChar();return false;}
  char c2 = peekChar();
  ungetChar();
  if (c2 == '*')return true;
  if (c2 == '/')return true;
  return false;
 }

 protected boolean isEOLChar(char c)
 {
  for (char eolChar : EOLChars)if (c == eolChar)return true;
  return false;
 }

 public boolean isSeparator(String s)
 {
  if (s.length() != 1)return false;
  char sChar = s.charAt(0);
  for (char c: SeparatorChars)
       if (sChar == c)return true;
  return false;
 }

 public boolean isSymbol()
 throws MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("MyStreamTokenizer.isSymbol Internal error. CurrentTokenInfo is null");

  return CurrentTokenInfo.isSymbol();
 }

 public boolean isSymbol(String s)
 {
  if (s          == null)return false;
  if (s.length() != 1   )return false;
  return isSymbolChar(s.charAt(0));
 }

 private boolean isSymbolChar(char c)
 {
  for (char symbolChar : SymbolChars)if (c == symbolChar)return true;
  return false;
 }

 protected boolean isWhitespaceChar(char c)
 {
  for (char whitespaceChar : WhitespaceChars)if (c == whitespaceChar)return true;
  return false;
 }

 public void lockCurrentToken()
 throws MyIOException, MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("MyStreamTokenizer.lockToken CurrentTokenInfo is null{"
                                                         +"where="+where()+"}");
  CurrentTokenInfo.lock();
 }

 public static void main(String[] args)
 { // test MyStreamTokenizer
  String test1 = "Array1=abc\r\n"
                +"Name1 =name";
  MyStreamTokenizer mst = new MyStreamTokenizer(new StringReader(test1),null);
  try {
       for (String token = mst.getToken(); token!=null; token=mst.getToken())
           {
            System.out.println("note token{"
                              +"token="+token+"}");
           }
      }
  catch (MyException e){System.out.println(e.getMessage());}
 }

 public boolean moreData()
 throws MyIOException, MyParseException
 {
  return moreData(CurrentLocalOffsetInBuffer);
 }

 public boolean moreData(int pos)
 throws MyIOException, MyParseException
 {
  if (pos < Buffer.length())return true;

  char[] buffer = new char[ReadBufferSize];
  try {
       while (true)
             {
              if (SourceNo >= Source.length)return false;
              int size = Source[SourceNo].Reader.read(buffer);
               if (size == 0)
                   throw new MyIOException("No bytes read{"
                                          +"where="+where()+"}");
               if (size == -1){SourceNo++;continue;}
               Buffer.append(buffer,0,size);
               break;
             }  
      }
  catch (IOException e){throw new MyIOException("I/O error{"
                                               +"ExceptionMessage="+e.getMessage()+"\t"
                                               +"where="           +where()       +"}");}
  dumpData("before trimBuffer");
  trimBuffer();
  dumpData("after trimBuffer");
  return true;
 }

 public MyStreamTokenizer(Reader reader,String fileName)
 {
  Source           = new Parms[]{new Parms(reader,fileName)};
  CurrentTokenInfo = new TokenInfo(1,0,false,null,0,NextTokenId++);  // create initail dummy TokenInfo in case caller wants to lock at beginning of 'file'.
  TokenHistory.add(CurrentTokenInfo);
 }

 public MyStreamTokenizer(Parms[] parms)
 {
  if (parms        == null)Utilities.fatalError("MyStreamTokenizer parms is null");
  if (parms.length == 0   )Utilities.fatalError("MyStreamTokenizer parms.length is 0");
  Source = parms;
  
  CurrentTokenInfo = new TokenInfo(1,0,false,null,0,NextTokenId++);  // create initail dummy TokenInfo in case caller wants to lock at beginning of 'file'.
  TokenHistory.add(CurrentTokenInfo);
 }

 public void noteSubstitution(String name, String substitution)
 {
  Substitutions.put(name,substitution);
 }

 public void noteSubstitutions(HashMap<String,String> substitutions)
 {
  Substitutions = substitutions;
 }

 public char peekChar()
 throws MyIOException, MyParseException
 {
  if (!moreData())throw new MyParseException("peekChar Internal error. Unexpected EOF"
                                            +"where="+where()+"}");
  return Buffer.charAt(CurrentLocalOffsetInBuffer);
 }

 public void registerToken(long    currentLineNo                       ,
                           long    currentLineStartGlobalOffsetInBuffer,
                           boolean isSymbol                            ,
                           String  token                               ,
                           int     offsetInLine)
 {
  CurrentTokenInfo = new TokenInfo(currentLineNo                       ,
                                   currentLineStartGlobalOffsetInBuffer,
                                   isSymbol                            ,
                                   token                               ,
                                   offsetInLine                        ,
                                   NextTokenId++                       );
  TokenHistory.add(CurrentTokenInfo);
  while (TokenHistory.size() > MaxHistoryDepth)
        {
         TokenInfo firstToken = TokenHistory.getFirst();
         if (firstToken == null   )break;
         if (firstToken.isLocked())break;
         TokenHistory.removeFirst();
        }
 }

 // This method is the same as getTokenInfo except it uses the class variable TokenHistorIterator.  TokenHistoryIterator must be set correctly on exit.
 public void setCurrentToken(long id)
 throws MyParseException
 {
  if (Debug&&Utilities.debug())System.err.println("setCurrentToken{"
                                                 +"nest="+Utilities.nest()+"}");
  TokenHistoryIterator = TokenHistory.listIterator();
  while (TokenHistoryIterator.hasNext())
        {
         TokenInfo tokenInfo = TokenHistoryIterator.next();
         if (tokenInfo.getId()==id)return;
        }
  throw new MyParseException("MyStreamTokenizer.setCurrentToken internal error. id not found{"
                            +"id="+id+"}");
 }

 public void setDebug          (boolean value){Debug          =value;}
 public void setEOLChars       (char[]  value){EOLChars       =value;}
 public void setReadBufferSize (int     value){ReadBufferSize =value;}
 public void setSeparatorChars (char[]  value){SeparatorChars =value;}
 public void setSymbolChars    (char[]  value){SymbolChars    =value;}
 public void setWhitespaceChars(char[]  value){WhitespaceChars=value;}

 private void trimBuffer()
 throws MyParseException
 {
  if (TokenHistory.size()==0)return;
  int amount = getBufferLocalOffset(TokenHistory.getFirst().getLineStartGlobalOffset());
  Buffer.delete(0,amount);
  TotalAmountTrimmedFromBuffer        += amount;
  CurrentLocalOffsetInBuffer          -= amount;
  CurrentLineStartLocalOffsetInBuffer -= amount;
 }

 protected void ungetChar()
 {
  CurrentLocalOffsetInBuffer--;
 }

 public void ungetToken()
 throws MyIOException, MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("ungetToken. Internal error. No token to unget.{"
                                                         +"where="+where()+"}");
  if (Debug&&Utilities.debug())System.err.println("ungetToken note token{"
                                          +"FileName="+Source[SourceNo].FileName  +"\t"
                                          +"nest="    +Utilities.nest()           +"\t"
                                          +"token="   +CurrentTokenInfo.getToken()+"}");
  if (TokenHistory.size() < 1)throw new MyParseException("ungetToken. Internal error. No token to unget2.{"
                                                        +"where="+where()         +"}");
  if (TokenHistoryIterator == null)TokenHistoryIterator = TokenHistory.listIterator(TokenHistory.size()); // because iterator is positioned after current.

  if (!TokenHistoryIterator.hasPrevious())throw new MyParseException("ungetToken. Internal error. No token to unget3.{"
                                                                    +"where="+where()+"}");
  TokenHistoryIterator.previous();  // This would retrieve the current element.  But we want the one before it to now be current.

  if (TokenHistoryIterator.hasPrevious())
     {
      CurrentTokenInfo = TokenHistoryIterator.previous();
      TokenHistoryIterator.next();  // Reposition cursor to after the current element.
     }
  else 
      CurrentTokenInfo = null;   // We are at the beginning.  That is OK.  We just can't unget again.  This feature doesn't seem to be used right now.
 }

 public void unlockCurrentToken()
 throws MyIOException, MyParseException
 {
  if (CurrentTokenInfo == null)throw new MyParseException("MyStreamTokenizer.unlockCurrentToken CurrentTokenInfo is null{"
                                                         +"where="+where()+"}");
  CurrentTokenInfo.unlock();
 }

 public String where()
 throws MyIOException, MyParseException
 {
  int sourceNo = SourceNo>=Source.length?Source.length-1:SourceNo;
  return where(Source[sourceNo].FileName,CurrentLineNo,getLine(CurrentLineStartLocalOffsetInBuffer),CurrentLocalOffsetInBuffer-CurrentLineStartLocalOffsetInBuffer);
 }

 public String where(int localOffsetInBuffer)
 throws MyIOException, MyParseException
 {
  int offsetInLine = localOffsetInBuffer - CurrentLineStartLocalOffsetInBuffer;
  int sourceNo = SourceNo>=Source.length?Source.length-1:SourceNo;
  return where(Source[sourceNo].FileName,CurrentLineNo,getLine(CurrentLineStartLocalOffsetInBuffer),offsetInLine);
 }

 private String where(long lineNo, int lineStartLocalOffsetInBuffer, int offsetInLine)
 throws MyIOException, MyParseException
 {
  return where(lineNo,getLine(lineStartLocalOffsetInBuffer),offsetInLine);
 }

 public String where(long lineNo, String line, int offsetInLine)
 throws MyParseException
 {
  int sourceNo = SourceNo>=Source.length?Source.length-1:SourceNo;
  return where(Source[sourceNo].FileName,lineNo,line,offsetInLine);
 }

 public String where(String fileName, long lineNo, int lineStartLocalOffsetInBuffer, int offsetInLine)
 throws MyIOException, MyParseException
 {
  return where(fileName,lineNo,getLine(lineStartLocalOffsetInBuffer),offsetInLine);
 }

 public String where(String fileName, long lineNo, String line, int offsetInLine)
 throws MyParseException
 {
  String quotedLine = Utilities.quote(line);
  return "{"
        +"Buffer="                             +Utilities.quote(Buffer.toString())                                    +"\t"
        +"CurrentLineStartLocalOffsetInBuffer="+CurrentLineStartLocalOffsetInBuffer                                   +"\t"
        +"CurrentLocalOffsetInBuffer="         +CurrentLocalOffsetInBuffer                                            +"\t"
        +"file="                               +fileName                                                              +"\t"
        +"line="                               +quotedLine                                                            +"\t"
        +"lineMark="                           +Utilities.spaces(Utilities.quotedOffset(quotedLine,offsetInLine))+"^" +"\t"
        +"lineNumber="                         +lineNo                                                                +"\t"
        +"offsetInLine="                       +offsetInLine                                                          +"}";
 }

 private String where(TokenInfo tokenInfo)
 throws MyIOException, MyParseException
 {
  int sourceNo = SourceNo>=Source.length?Source.length-1:SourceNo;
  return where(Source[sourceNo].FileName                                          ,
               tokenInfo.getLineNo()                                              ,
               getLine(getBufferLocalOffset(tokenInfo.getLineStartGlobalOffset())),
               tokenInfo.getTokenOffsetInLine()                                   );
 }

 public String whereById(long id)
 throws MyIOException, MyParseException
 {
  return where(getTokenInfo(id));
 }

 public String whereCurrentToken()
 throws MyIOException, MyParseException
 {
  if (CurrentTokenInfo==null)return where();
//  if (CurrentTokenInfo==null)throw new MyParseException("MyStreamTokenizer.whereCurrentToken internal error. CurrentTokenInfo is null");
  return where(CurrentTokenInfo);
 }
}


