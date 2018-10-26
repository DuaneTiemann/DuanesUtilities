package com.duane.utilities;
import java.lang.Exception;
import com.duane.utilities.StructuredMessageFormatter;
import com.duane.utilities.Utilities;

/**
 * MyException is used to distinguish exceptions whose mesages 
 * can be formatted with my standard formatter.  
 */
public class MyException extends Exception
{
 private static final long serialVersionUID = 1;
 public MyException(String msg){super(msg);}
// public String toString(){try{return StructuredMessageFormatter.format(getMessage()                )
//                                    +StructuredMessageFormatter.format("Nest"+Utilities.nest(this));}
//                          catch(Exception e){try {return e.getMessage()+"\n"
//                                                        +Utilities.nest(e);}
//                                             catch (MyParseException e2){return null;}
//                                            }
//                         }
}
