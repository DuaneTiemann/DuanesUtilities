package com.duane.utilities;
import com.duane.utilities.MyException;
/**
 * 
 * 
 * @author Duane Tiemann (4/18/2015)
 * @param  message
 */

// I created this class because ParseException requires an offset, but has no line number parameter.  
// I want to provide both with nice formatting so I just take a message.  I expect it will follow nested braces conventions.

public class MyParseException extends MyException
{
 private static final long serialVersionUID = 1;
 public MyParseException(String message)
 {
  super(message);
 }
}
