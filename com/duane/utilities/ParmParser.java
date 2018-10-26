package com.duane.utilities;
import  java.io.IOException;
import  java.io.RandomAccessFile;
import  java.lang.IllegalAccessException;
import  java.util.ArrayList;
import  java.util.HashMap;

import  com.duane.utilities.MyParseException;
import  com.duane.utilities.ParmParserImpl;
import  com.duane.utilities.Utilities;

/*
TO DO: 
 1. Test current code.
    Think up test cases.
    Review code coverage.
    Use it.
 2. Do annotations.
 3. Write formatter.
 4. null for arrays and collections
 
  
The ParmParser class maps a json-like parameter specification to java objects.
The intent is that parameters may be provided on the command line and/or in config files.

Braces can not be used to both bound arrays and constructor parms because of examples like the following:
An object has an element which is an array of objects of class c which take 1 and 2 string constructors.
element={a,b} could be taken to mean a 2 element array of c objects using its single string constructor
or a 1 element array of class c using its 2 string constructor.
So let's use parens to bound constructor arguments.

Could braces still be used to bound arrays and to house object elements?
No.
Suppose class c constructors take an array or string in single argument constructors.  Then for an element which is an array of c's,
element={a,b} could be taken to mean 1 instance of the class or 2 instances which take a single argument constructor.

So let's use brackets to bound arrays as json does.

Single element arrays may omit the brackets as we already know they are arrays from the class definition.
Single argument construction parm lists can omit the parens.
Braces will always be needed to bound named element lists.  Thus a=b=c is not legal.  a={b=c} is needed.

We will not worry about distinguishing subclasses.  If an object contains an element which could be one of several subclasses, we'll construct the 
superclass specified.  We'll handle typing the subclasses in json later, if needed.  This seems like a feature which is more likely needed for
serialization/deserialization than for config files.

For now, we're starting with objects which are already instantiated.  How do we decide when to append to arrays and when to replace them, in case
they were there by default? 
 
Strings can not span files. 
  
@author Duane Tiemann

*/

// issues:
// 1. What if initial object has a single string constructor?  How is that distinguished from a file name?
//    A. For now it will have to be inside parens to be considered a constructor argument.
// 2. How will null be specified?
//    A. {} makes commas always optional and ignored. It also means we can't have {} mean default components.  
//        () would have to be the only way to use all defaults.
//        json-io uses null unquoted for null.  I could do that too.  If someone really wants to specify a string of "null", it would have to be quoted.
//        That allows me to be compatible with json.  For now, that seems best.
//  3. How do we know when to append to arrays and when to replace them?  
//    A. Auxiliary structure to track arrays. 
// 4. Allowing target to be Class or Object is inband signalling.  That's a no no.  That means Class objects can not be created. 
//    I could always create the target object.  main could use ParmParser to create the parent class. 
// 5. What about comments?   #, /* */, //
//    A. All handled by MyStreamTokenizer
// 6. Should I throw IOException and ParseException or just my own ParseException.
//    A. My own ParseException.  That allows more consistent formatting.  Offset does not have to be separate parameter which was awkward.
//       IOException would have been awkward to match to where().
// 7. What is the strategy for log and debug files?
// 8. The parse routine should be static so no one has to create the ParmParser object just to call it.
// 9. Should I narrow the visibility of ParmParser object internals by using an implementation class?
// 10. Can StreamTokenizer do the job? 
// 10.a What about String continuation on multiple lines with \?
// 10.b what about ',,' vs '  '?
// 12. What do I do if there is no constructor?
// 13. Can I always construct an object right away in parse() if target is a Class?  i.e. Can the first parm be constructor arguments?
//     A. Probably not.  Certainly not, if I use parse to create objects after =.
// 14. I've provided 2 methods for parsing in order to avoid depending on whether or not the target is a class.  This allows creation of Class objects.
// 15. Arrays are currently always replaced.  That is a problem if we're getting elements from 2 files.  Each one will be parsed separately. 
//     The elements from the 2nd file will replace those from the first.  One way around it is to just use a flag to indicate whether or not arrays 
//     should be appended.  Another way is to handle it with annotations.  I think the flag is better as that will handle arrays in foreign objects.
//     Oops.  I need to track replace/append per individual array, not globally.
// 16. If target is an array, we need to be able to create it.
//     And the HashMap approach will not work because there won't be a variable name.  Maybe I could use null for that case.
// 17. Need to handle ArrayLists, HashMaps, etc. explicitly.
//     Watch out for feature creep here. The intent is to generally provide parameter files rather than a serialized transport mechanism.  
//     We could create an ArrayList by just creating the underlying support structure automatically, but that would be painful for folks to use.
//     What does json-io do for this?
//     A.  json-io does arrays, Collections, and Maps generically. I'll do that too.  But I'm going to rely on the target for the element class.  
//         If it hasn't been specified and String doesn't work or the input is in object format, I'll throw an error.  Maybe, later, if needed, I'll add 
//         support for specifying the element class in the input. For now, I regard that as feature creep. 
// 
// 18. If I manage array appending properly, do I need postProcessArrays?
//     A.  No.  But it would be more efficient than always 'appending' to an array.  I expect that arrays would usually be small, but n^2 seems tacky and
//         it would be embarrassing if it became a problem.
// 19. How far should I go to support arrays, ArrayLists, HashMaps, HashTables, HashSets, and arbitrary similar classes?
// 20. Should I have a HashSet that indicates which variables have been initialized, allowing subsequent additions to append rather than replace?
// 21. Can appending to these "collections" be done in a uniform manner? It seems we have 2 cases: 1) key,value and 2) value.  What does json-io do here?    
//     A. Sortof.  I can do Arrays, Collections, and Maps. I can look at generic type variables to determine the target classes, 
//        but if the target doesn't have a generic declaration, it fails. I would have to have descriptive info in the parms file and I don't want to do that.
// 22. If initial target is an array, how do we incorporate file names?
//     We could require brackets for the elements in that case. 
//     But that is awkward. Maybe we should just require that the initial target not be an array or collection.  The name=value idea doesn't really 
//     work arrays and collections.
// 
//     A map would still be OK.
// 
// slass b
// {
//  String X=null;
//  public b(String s){X=s;}
//  public b(){}
// }
// 
// class c
// {
//  public b[][] Bs;
// }
// 
// 
//   Bs={a}
//   Bs={b}
//   Bs={{a,b,c},{d,e,f}}
//   Bs={
//       {i,{X=d},{X=g},h}
//      }
//   Bs={{X=d},{X=g},h}
//
//   element ::= {Name=value,Name=value,...}
//   element ::= value   
//   ARRAY ::= element
//.  ARRAY ::= {element,element,...}
// Note that arrays could be interspersed among collections.  So we can not rely on component type on returned values.  Returned values may have an empty array or collection.
//
//  ArrayList<ArrayList<c>[]>[] target=null;
//
// 23. Should we allow omitting braces for single element groups at all levels or just the top?
// 
// 
// 
// 
// TODO 
// 1. Allow annotation of fields to limit parms. 
// 2. Allow more general construction parameters. 
// 3. Format messages
// 4. Figure out logging interface.
// 

public class ParmParser
{
 public static Object createObject(Class<?> target, String[] parms)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Utilities.debug())System.err.println("createObject enter{"
                                          +"parms="+Utilities.toString(parms)+"}");
  Object result = ParmParserImpl.parse(target,parms,true);
  if (Utilities.debug())System.err.println("createObject return");
  return result;
 }

 private static class TestClass1
 {
  private ArrayList<Integer>       ArrayList1   =null;
  private String[]                 Array1       =new String[]{"original"};
  private HashMap<String,String>[] Array2       =null;
  private boolean                  Boolean1     =false;
  private byte                     Byte1        ='a' ;
  private char                     Char1        ='a' ;
  private double                   Double1      =0   ;
  private float                    Float1       =0   ;
  private HashMap<String,String>   HashMap1     =null;
  private int                      Int1         =0   ;
  private long                     Long1        =0   ;
  private String                   Name1        =null;
  private short                    Short1       =0   ;
  private boolean               [] BooleanArray=null;
  private byte                  [] ByteArray   =null;
  private char                  [] CharArray   =null;
  private double                [] DoubleArray =null;
  private float                 [] FloatArray  =null;
  private int                   [] IntArray    =null;
  private long                  [] LongArray   =null;
  private short                 [] ShortArray  =null;
  private ArrayList<boolean[]>     BooleanArrayList=null;
  private ArrayList<byte   []>     ByteArrayList   =null;
  private ArrayList<char   []>     CharArrayList   =null;
  private ArrayList<double []>     DoubleArrayList =null;
  private ArrayList<float  []>     FloatArrayList  =null;
  private ArrayList<int    []>     IntArrayList    =null;
  private ArrayList<long   []>     LongArrayList   =null;
  private ArrayList<short  []>     ShortArrayList  =null;
//  public TestClass1(){}
  public String toString()
  {
   return "{TestClass1:"
         +"Array1="           +Utilities.toString(Array1    )      +"\t"
         +"Array2="           +Utilities.toString(Array2    )      +"\t"
         +"ArrayList1="       +Utilities.toString(ArrayList1)      +"\t"
         +"Boolean1="         +Boolean1                            +"\t"
         +"Byte1="            +Byte1                               +"\t"
         +"Char1="            +Char1                               +"\t"
         +"Double1="          +Double1                             +"\t"
         +"Float1="           +Float1                              +"\t"
         +"HashMap1="         +Utilities.toString(HashMap1)        +"\t"
         +"Int1="             +Int1                                +"\t"
         +"Long1="            +Long1                               +"\t"
         +"Name1="            +Name1                               +"\t"
         +"Short1="           +Short1                              +"\t"
         +"BooleanArray="     +Utilities.toString(BooleanArray    )+"\t"
         +"ByteArray="        +Utilities.toString(ByteArray       )+"\t"
         +"CharArray="        +Utilities.toString(CharArray       )+"\t"
         +"DoubleArray="      +Utilities.toString(DoubleArray     )+"\t"
         +"FloatArray="       +Utilities.toString(FloatArray      )+"\t"
         +"IntArray="         +Utilities.toString(IntArray        )+"\t"
         +"LongArray="        +Utilities.toString(LongArray       )+"\t"
         +"ShortArray="       +Utilities.toString(ShortArray      )+"\t"
         +"BooleanArrayList=" +Utilities.toString(BooleanArrayList)+"\t"
         +"ByteArrayList="    +Utilities.toString(ByteArrayList   )+"\t"
         +"CharArrayList="    +Utilities.toString(CharArrayList   )+"\t"
         +"DoubleArrayList="  +Utilities.toString(DoubleArrayList )+"\t"
         +"FloatArrayList="   +Utilities.toString(FloatArrayList  )+"\t"
         +"IntArrayList="     +Utilities.toString(IntArrayList    )+"\t"
         +"LongArrayList="    +Utilities.toString(LongArrayList   )+"\t"
         +"ShortArrayList="   +Utilities.toString(ShortArrayList  )+"}";
  }
 }

 private static class TestClass2
 {
  public int Value=0;
  public TestClass2(int value){Value=value;}
 }

 private static class TestClass3
 {
  public TestClass2 TC2=null;
 }

 private static class TestClass4
 {
  public String Value=null;
  public TestClass4(String value){Value=value;}
 }

 private static class TestClass5
 {
  public TestClass4 TC4=null;
 }

 private static class TestClass6
 {
  public String Value=null;
  public TestClass6(int    value){}
  public TestClass6(String value){Value=value;}
 }

 private static class TestClass7
 {
  public TestClass6 TC6=null;
 }

 private static class TestClass8
 {
  public HashMap<ArrayList<String>,HashMap<String,String>>      HashMap =null;
  public HashMap<HashMap<String,String>,HashMap<String,String>> HashMap2=null;
  public String                                                 Value  = "default Value";
 }

 private static class TestClass9
 {
  public ArrayList<ArrayList<ArrayList<HashMap<HashMap<String,String>,ArrayList<String>>[][]>>> collection = null;
 }

 // private static class TestClass9
 // {
 //  public ArrayList<
 //                   ArrayList<
 //                             ArrayList<
 //                                       HashMap<
 //                                               HashMap<String,String>,
 //                                               ArrayList<String>
 //                                              >[][]
 //                                      >
 //                            >
 //                  > collection = null;
 // 
 //              { ArrayList        1
 //               { ArrayList       2
 //                { ArrayList      3
 //                 { []            4
 //                  { []           5
 //                   { HashMap     6
 //                    { HashMap    7
 //                                 8
 //                    }
 //                    { ArrayList
 //                    }
 //                   }
 //                  }
 //                 }
 //                }
 //               }
 //              }
 // }


 private static void createTestFile(String fileName, String contents)
 throws IOException
 {
  RandomAccessFile raf = new RandomAccessFile(fileName,"rw");
  raf.writeBytes(contents);
  raf.close();
 }

 public static void main(String[] args)
 {
  Utilities.debug(true);
  main_(args);
  Utilities.debug(false);
  main_(args);
 }

 @SuppressWarnings("unchecked")
 public static void main_(String[] args)
 { // test ParmParser
  try {
       setReadBufferSize(20);
       createTestFile("TestFile1.inp","Array1  =abc\r\n"
                                     +"Array2  ={{a=b c=d d=e},"
                                     +          "{e=f f=g g=h}}"
                                     +"HashMap1={a=b;c=d}// comment \r\n"
                                     +"//HashMap1={a=b;c=d}\r\n"
                                     +"/*HashMap1={a=b;c=d}\r\n"
                                     +"/*HashMap1={a=b;c=d}\r\n"
                                     +"/*HashMap1={a=b;c=d}\r\n"
                                     +"/*HashMap1={a=b;c=d} commented out */ \r\n"
                                     +"Name1   =name,  # comment \r\n"
                                     +"Name1   =name,  # Check out max history depth \r\n"
                                     +"Name1   =name,  # Check out max history depth \r\n"
                                     +"Name1   =name,  # Check out max history depth \r\n"
                                     +"Name1   =name,  # Check out max history depth \r\n"
                                     +"Name1   =name,  # Check out max history depth \r\n"
                                     +"Name1   =name,  # Check out max history depth \r\n"
                                     +"Name1   =name,  # Check out max history depth \r\n"
                                     +"Name1   = /* comment*/\r\n"
                                     +"          a\r\n"
                                     +"Name1   =\r\n"
                                     +"         (name)\r\n"
                                     +"BooleanArray=true\r\n"
                                     +"ByteArray   =1   \r\n"
                                     +"CharArray   =1   \r\n"
                                     +"DoubleArray =1   \r\n"
                                     +"FloatArray  =1   \r\n"
                                     +"IntArray    =1   \r\n"
                                     +"LongArray   =1   \r\n"
                                     +"ShortArray  =1   \r\n"
                                     +"BooleanArrayList=true\r\n"
                                     +"ByteArrayList   =1   \r\n"
                                     +"CharArrayList   =1   \r\n"
                                     +"DoubleArrayList =1   \r\n"
                                     +"FloatArrayList  =1   \r\n"
                                     +"IntArrayList    =1   \r\n"
                                     +"LongArrayList   =1   \r\n"
                                     +"ShortArrayList  =1   \r\n");
       
       TestClass1 tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"TestFile1.inp"});

       System.out.println("Test   1 "+(tc1.Array1.length==1                 ?"good":"bad"));
       System.out.println("Test   2 "+(tc1.Array1[0]         .equals("abc" )?"good":"bad"));
       System.out.println("Test   3 "+(tc1.Array2.length==2                 ?"good":"bad"));
       System.out.println("Test   4 "+(tc1.Array2[0].size()==3              ?"good":"bad"));
       System.out.println("Test   5 "+(tc1.Array2[0].get("a").equals("b"   )?"good":"bad"));
       System.out.println("Test   6 "+(tc1.Array2[0].get("c").equals("d"   )?"good":"bad"));
       System.out.println("Test   7 "+(tc1.Array2[0].get("d").equals("e"   )?"good":"bad"));
       System.out.println("Test   8 "+(tc1.Array2[1].size()==3              ?"good":"bad"));
       System.out.println("Test   9 "+(tc1.Array2[1].get("e").equals("f"   )?"good":"bad"));
       System.out.println("Test  10 "+(tc1.Array2[1].get("f").equals("g"   )?"good":"bad"));
       System.out.println("Test  11 "+(tc1.Array2[1].get("g").equals("h"   )?"good":"bad"));
       System.out.println("Test  12 "+(tc1.HashMap1 .get("a").equals("b"   )?"good":"bad"));
       System.out.println("Test  13 "+(tc1.HashMap1 .get("c").equals("d"   )?"good":"bad"));
       System.out.println("Test  14 "+(tc1.Name1             .equals("name")?"good":"bad"));

       int[]     intArray = (int[])createObject(int[].class,null);
       System.out.println("Test  15 "+(intArray.length==0                   ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,null);
       System.out.println("Test  16 "+(tc1 != null                          ?"good":"bad"));
       tc1            = (TestClass1)updateObject(tc1             ,null);
       System.out.println("Test  17 "+(tc1 != null                          ?"good":"bad"));
       try {createObject(TestClass1.class,new String[]{"Name1=(name}"});
            System.out.println("Test  18 bad no catch");}
       catch (MyParseException e){System.out.println("Test  18 "+(e.getMessage().contains("parseObject expected close paren{"                       )?"good":"bad"));}
       try {createObject(TestClass1.class,new String[]{"Name1=(name name2)"});
            System.out.println("Test  19 bad no catch");}
       catch (MyParseException e){System.out.println("Test  19 "+(e.getMessage().contains("parseObject expected close paren{"                       )?"good":"bad"));}
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"{Name1=name}"});
       System.out.println("Test  20 "+(tc1.Name1             .equals("name")?"good":"bad"));
       try {createObject(TestClass1.class,new String[]{"=}"});
            System.out.println("Test  21 bad no catch");}
       catch (MyParseException e){System.out.println("Test  21 "+(e.getMessage().contains("\"ParmParserImpl.parse unexpected = as first parm\"{"     )?"good":"bad"));}
//       try {createObject(TestClass1.class,new String[]{"[}"});
//            System.out.println("Test  22 bad no catch");}
//       catch (MyParseException e){System.out.println("Test  22 "+(e.getMessage().contains("ParmParserImpl.parse target is not array and token is [{")?"good":"bad"));
//                                  System.out.println("Test  22 "+e.getMessage());
//                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=\"name\" \\ \"1\""});
       System.out.println("Test  23 "+(tc1.Name1             .equals("name1")?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=name\\1"});
       System.out.println("Test  24 "+(tc1.Name1             .equals("name1")?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1={}"});
       System.out.println("Test  25 "+(tc1.Name1.length() == 0               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=null"});
       System.out.println("Test  26 "+(tc1.Name1 == null                     ?"good":"bad"));
       try {createObject(TestClass1.class,new String[]{"}Name1==7"});
            System.out.println("Test  27 bad no catch");}
       catch (MyParseException e){System.out.println("Test  27 "+(e.getMessage().contains("ParmParserImpl.parseObject_ unexpected initial symbol{"  )?"good":"bad"));}
       try {createObject(TestClass1.class,new String[]{"Name1=name}"});
            System.out.println("Test  18 bad no catch");}
       catch (MyParseException e){System.out.println("Test  28 "+(e.getMessage().contains("parseObject unexpected close brace{"                     )?"good":"bad"));}
       try {createObject(TestClass1.class,new String[]{"Name1=7,}Name1=8"});
            System.out.println("Test  29 bad no catch");}
       catch (MyParseException e){System.out.println("Test  29 "+(e.getMessage().contains("parseObject unexpected close brace{"                     )?"good":"bad"));
                                  System.out.println("Test  29 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"{Array1=abc Name1=name}"});
       System.out.println("Test  30 "+(tc1.Name1.equals("name")              ?"good":"bad"));
       try {updateObject(TestClass1.class,new String[]{"{Array1=abc Name1=name}"});
            System.out.println("Test  31 bad no catch");}
       catch (MyParseException e){System.out.println("Test  31 "+(e.getMessage().contains("ParmParserImpl.doAssignment No such field{"              )?"good":"bad"));}
       tc1            = (TestClass1)updateObject(tc1             ,new String[]{"{Array1=abc Name1=name}"});
       System.out.println("Test  32 "+(tc1.Name1.equals("name")              ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"TestFile1.inp TestFile1.inp"});
       System.out.println("Test  33 "+(tc1.Array1.length == 2                ?"good":"bad"));
       try {createObject(TestClass1.class,new String[]{"{Array1=abc Name1=name"});
            System.out.println("Test  34 bad no catch");}
       catch (MyParseException e){System.out.println("Test  34 "+(e.getMessage().contains("ParmParserImpl.parse unexpected end of file"             )?"good":"bad"));
                                  System.out.println("Test  34 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"{HashMap1="});
            System.out.println("Test  35 bad no catch");}
       catch (MyParseException e){System.out.println("Test  35 "+(e.getMessage().contains("parseMap unexpected EOF"                                 )?"good":"bad"));}
       HashMap<String,String> hashMap = null;
       try {hashMap = (HashMap<String,String>)createObject(HashMap.class,new String[0]);
            System.out.println("Test  36 bad no catch");}
       catch (MyParseException e){System.out.println("Test  36 "+(e.getMessage().contains("parse Top level map"                                     )?"good":"bad"));}
       try {hashMap = (HashMap<String,String>)updateObject(new HashMap<String,String>(),new String[0]);
       System.out.println("Test  37 bad no catch");}
       catch (MyParseException e){System.out.println("Test  37 "+(e.getMessage().contains("parse Top level map"                                     )?"good":"bad"));}
       try {createObject(TestClass1.class,new String[]{"{HashMap1=s"});
            System.out.println("Test  38 bad no catch");}
       catch (MyParseException e){System.out.println("Test  38 "+(e.getMessage().contains("ParmParserImpl.parseMap Map must start with open brace"  )?"good":"bad"));}
       try {createObject(TestClass1.class,new String[]{"{HashMap1=("});
            System.out.println("Test  39 bad no catch");}
       catch (MyParseException e){System.out.println("Test  39 "+(e.getMessage().contains("ParmParserImpl.parseMap Map must start with open brace"  )?"good":"bad"));}
       try {hashMap = (HashMap<String,String>)updateObject(new HashMap<String,String>(),new String[]{"{a=b}"});
            System.out.println("Test  40 bad no catch");}
       catch (MyParseException e){System.out.println("Test  40 "+(e.getMessage().contains("parse Top level map"                                     )?"good":"bad"));}
       try {createObject(TestClass1.class,new String[]{"{HashMap1={a(b"});
            System.out.println("Test  41 bad no catch");}
       catch (MyParseException e){System.out.println("Test  41 "+(e.getMessage().contains("ParmParser.parseMap expecting ="                         )?"good":"bad"));
                                  System.out.println("Test  41 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"{HashMap1={a b"});
            System.out.println("Test  42 bad no catch");}
       catch (MyParseException e){System.out.println("Test  42 "+(e.getMessage().contains("ParmParser.parseMap expecting ="                         )?"good":"bad"));
                                  System.out.println("Test  42 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"{HashMap1={a=b("});
            System.out.println("Test  43 bad no catch");}
       catch (MyParseException e){System.out.println("Test  43 "+(e.getMessage().contains("ParmParserImpl.getTokenOrDie unexpected EOF"             )?"good":"bad"));
                                  System.out.println("Test  43 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Boolean1=true Char1=C Byte1=2 Int1=5 Long1=6 Short1=3 Float1=1.5 Double1=2.5"});
       System.out.println("Test  44 "+(tc1.Boolean1 == true                ?"good":"bad"));
       System.out.println("Test  45 "+(tc1.Byte1    == 2                   ?"good":"bad"));
       System.out.println("Test  46 "+(tc1.Char1    == 'C'                 ?"good":"bad"));
       System.out.println("Test  47 "+(tc1.Double1  == 2.5                 ?"good":"bad"));
       System.out.println("Test  48 "+(tc1.Float1   == 1.5                 ?"good":"bad"));
       System.out.println("Test  49 "+(tc1.Int1     == 5                   ?"good":"bad"));
       System.out.println("Test  50 "+(tc1.Long1    == 6                   ?"good":"bad"));
       System.out.println("Test  51 "+(tc1.Short1   == 3                   ?"good":"bad"));
       try {createObject(TestClass1.class,new String[]{"Boolean1="});
            System.out.println("Test  52 bad no catch");}
       catch (MyParseException e){System.out.println("Test  52 "+(e.getMessage().contains("parsePrimitive unexpected EOF"                           )?"good":"bad"));
                                  System.out.println("Test  53 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"Boolean1="});
            System.out.println("Test  53 bad no catch");}
       catch (MyParseException e){System.out.println("Test  53 "+(e.getMessage().contains("parsePrimitive unexpected EOF"                           )?"good":"bad"));
                                  System.out.println("Test  53 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       int int1 = (int)createObject(int.class,new String[0]);
       System.out.println("Test  54 "+(int1 == 0                           ?"good":"bad"));
       try {createObject(TestClass1.class,new String[]{"Boolean1=["});
            System.out.println("Test  55 bad no catch");}
       catch (MyParseException e){System.out.println("Test  55 "+(e.getMessage().contains("ParmParserImpl.parsePrimitive exception target field is primitive and source is object{")?"good":"bad"));
                                  System.out.println("Test  55 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"Boolean1=("});
       System.out.println("Test  56 bad no catch");}
       catch (MyParseException e){System.out.println("Test  56 "+(e.getMessage().contains("ParmParserImpl.parsePrimitive exception target field is primitive and source is object{")?"good":"bad"));
                                  System.out.println("Test  56 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"Boolean1={"});
            System.out.println("Test  57 bad no catch");}
       catch (MyParseException e){System.out.println("Test  57 "+(e.getMessage().contains("ParmParserImpl.parsePrimitive exception target field is primitive and source is object{")?"good":"bad"));
                                  System.out.println("Test  57 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"Array1="});
            System.out.println("Test  58 bad no catch");}
       catch (MyParseException e){System.out.println("Test  58 "+(e.getMessage().contains("ParmParserImpl.parseArray unexpected EOF"                )?"good":"bad"));
                                  System.out.println("Test  58 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Array1={}"});
       System.out.println("Test  59 "+(tc1.Array1!=null                    ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Array1={a b,c,,e}"});
       System.out.println("Test  60 "+(tc1.Array1[0].equals("a")           ?"good":"bad"));
       System.out.println("Test  61 "+(tc1.Array1[1].equals("b")           ?"good":"bad"));
       System.out.println("Test  62 "+(tc1.Array1[2].equals("c")           ?"good":"bad"));
       System.out.println("Test  63 "+(tc1.Array1[3] == null               ?"good":"bad"));
       System.out.println("Test  64 "+(tc1.Array1[4].equals("e")           ?"good":"bad"));
       try {createObject(TestClass1.class,new String[]{"Array1=5]"});
            System.out.println("Test  65 bad no catch");}
       catch (MyParseException e){System.out.println("Test  65 "+(e.getMessage().contains("ParseObject unexpected symbol"                           )?"good":"bad"));
                                  System.out.println("Test  65 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"Array1={5 6"});
            System.out.println("Test  66 bad no catch");}
       catch (MyParseException e){System.out.println("Test  66 "+(e.getMessage().contains("findClosingBrace unexpected EOF"                             )?"good":"bad"));
                                  System.out.println("Test  66 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"ArrayList1={1 5}"});
       System.out.println("Test  67 "+(tc1.ArrayList1.get(0) == 1          ?"good":"bad"));
       System.out.println("Test  68 "+(tc1.ArrayList1.get(1) == 5          ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=\"\\\\\\b\\f\\r\\n\\t\\u0020\\\"\\{\""});
       System.out.println("Test  69 "+(tc1.Name1.equals("\\\b\f\r\n\t \"{") ?"good":"bad "+tc1.Name1));
       try{createObject(TestClass1.class,new String[]{"Name1=\"\\\\\\b\\f\\r\\n\\t\\u0020\\"});
           System.out.println("Test  70 bad no catch");}
       catch (MyParseException e){System.out.println("Test  70 "+(e.getMessage().contains("getTransparentString matching double quote not found"    )?"good":"bad"));
                                  System.out.println("Test  70 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Name1=\"\\\\\\b\\f\\r\\n\\t\\u002g\\"});
           System.out.println("Test  71 bad no catch");}
       catch (MyParseException e){System.out.println("Test  71 "+(e.getMessage().contains("getTransparentString matching double quote not found{"   )?"good":"bad "+e.getMessage()));
                                  System.out.println("Test  71 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Name1=\"\\\\\\b\\f\\r\\n\\t\\u002"});
           System.out.println("Test  72 bad no catch");}
       catch (MyParseException e){System.out.println("Test  72 "+(e.getMessage().contains("Double quoted string not terminated before EOF{"         )?"good":"bad "+e.getMessage()));
                                  System.out.println("Test  72 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"/* commented out *"});
           System.out.println("Test  73 bad no catch");}
       catch (MyParseException e){System.out.println("Test  73 "+(e.getMessage().contains("Matching */ not found {"                                 )?"good":"bad"));
                                  System.out.println("Test  73 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"/* commented out "});
           System.out.println("Test  74 bad no catch");}
       catch (MyParseException e){System.out.println("Test  74 "+(e.getMessage().contains("Matching */ not found{"                                  )?"good":"bad"));
                                  System.out.println("Test  74 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a/b"});
       System.out.println("Test  75 "+(tc1.Name1.equals("a/b")             ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a/*b*/"});
       System.out.println("Test  76 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       try{createObject(TestClass1.class,new String[]{"Name1=\"b\r\n"});
           System.out.println("Test  77 bad no catch");}
       catch (MyParseException e){System.out.println("Test  77 "+(e.getMessage().contains("Double quoted string not terminated before EOL{"         )?"good":"bad"));
                                  System.out.println("Test  77 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Name1=\"b"});
           System.out.println("Test  78 bad no catch");}
       catch (MyParseException e){System.out.println("Test  78 "+(e.getMessage().contains("Double quoted string not terminated before EOF{"         )?"good":"bad"));
                                  System.out.println("Test  78 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"{Name1=a}}"});
           System.out.println("Test  79 bad no catch");}
       catch (MyParseException e){System.out.println("Test  79 "+(e.getMessage().contains("ParmParserImpl.parse trailing data not parsed{"          )?"good":"bad"));
                                  System.out.println("Test  79 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}

       createTestFile("TestFile2.inp","{Name1=name}}");
       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"TestFile1.inp"});
       try{createObject(TestClass1.class,new String[]{"TestFile2.inp"});
           System.out.println("Test  80 bad no catch");}
       catch (MyParseException e){System.out.println("Test  80 "+(e.getMessage().contains("parseFile trailing data not parsed{"                     )?"good":"bad"));
                                  System.out.println("Test  80 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"HashMap1={"});
           System.out.println("Test  81 bad no catch");}
       catch (MyParseException e){System.out.println("Test  81 "+(e.getMessage().contains("MyStreamTokenizer.getTokenOrDie unexpected EOF{"         )?"good":"bad"));
                                  System.out.println("Test  81 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Name1= /*comment 1*/ /*comment2 */ /* commment3 */ "});
           System.out.println("Test  82 bad no catch");}
       catch (MyParseException e){System.out.println("Test  82 "+(e.getMessage().contains("parseObject_ unexpected EOF, expecting value{"           )?"good":"bad"));
                                  System.out.println("Test  82 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{" HashMap1={}"});
       System.out.println("Test  83 "+(tc1.HashMap1!=null && tc1.HashMap1.size()==0?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a#"});
       System.out.println("Test  84 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a#\r"});
       System.out.println("Test  85 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a#\r\n"});
       System.out.println("Test  86 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a\r"});
       System.out.println("Test  87 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a\r\n"});
       System.out.println("Test  88 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a/*comment*/"});
       System.out.println("Test  89 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a #"});
       System.out.println("Test  90 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a #\r"});
       System.out.println("Test  91 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a #\r\n"});
       System.out.println("Test  92 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=a #\r\r"});
       System.out.println("Test  93 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       //                                                                                111111 1 1112
       //                                                                       123456789012345 6 7890
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=         \"\\u0061\""});
       System.out.println("Test  94 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       //                                                                                1 1 111111 1 1 2
       //                                                                       1234567890 1 234567 8 9 0
       tc1            = (TestClass1)createObject(TestClass1.class,new String[]{"Name1=    \"\\u0062\"\r\n"   
                                                                              +"Name1=    \"\\u0062\"\r\n"   
                                                                              +"Name1=    \"\\u0062\"\r\n"   
                                                                              +"Name1=    \"\\u0062\"\r\n"   
                                                                              +"Name1=    \"\\u0062\"\r\n"   
                                                                              +"Name1=    \"\\u0062\"\r\n"   
                                                                              +"Name1=    \"\\u0062\"\r\n"   
       //                                                                                111111 1 1112
       //                                                                       123456789012345 6 7890
                                                                              +"Name1=         \"\\u0061\""});
       System.out.println("Test  95 "+(tc1.Name1.equals("a")               ?"good":"bad"));
       try{
           TestClass3 tc3 = (TestClass3)createObject(TestClass3.class,new String[]{"TC2=string"});
           System.out.println("Test  96 bad no catch{tc3="          +tc3          +"}");
           System.out.println("Test  96 bad no catch{tc3.TC2="      +tc3.TC2      +"}");
           System.out.println("Test  96 bad no catch{tc3.TC2.Value="+tc3.TC2.Value+"}");
          }
       catch (MyParseException e){System.out.println("Test  96 "+(e.getMessage().contains("constructObject failed{"           )?"good":"bad"));
                                  System.out.println("Test  96 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       TestClass5 tc5 = (TestClass5)createObject(TestClass5.class,new String[]{"TC4=string"});
       System.out.println("Test  97 "+(tc5.TC4.Value.equals("string")         ?"good":"bad"));
       TestClass7 tc7 = (TestClass7)createObject(TestClass7.class,new String[]{"TC6=string"});
       System.out.println("Test  98 "+(tc7.TC6.Value.equals("string")         ?"good":"bad"));

       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"Array1=newOne","TestFile1.inp","TestFile1.inp"});
       System.out.println("Test  99 "+(tc1.Array1.length==3                   ?"good":"bad"));
       System.out.println("Test 100 "+(tc1.Array1[0]         .equals("newOne")?"good":"bad "+tc1.Array1[0]));
       System.out.println("Test 101 "+(tc1.Array1[1]         .equals("abc"   )?"good":"bad"));
       System.out.println("Test 102 "+(tc1.Array1[2]         .equals("abc"   )?"good":"bad "+tc1.Array1[2]));
       System.out.println("Test 103 "+(tc1.Array2.length==4                   ?"good":"bad"));
       System.out.println("Test 104 "+(tc1.Array2[0].size()==3                ?"good":"bad"));
       System.out.println("Test 105 "+(tc1.Array2[0].get("a").equals("b"     )?"good":"bad"));
       System.out.println("Test 106 "+(tc1.Array2[0].get("c").equals("d"     )?"good":"bad"));
       System.out.println("Test 107 "+(tc1.Array2[0].get("d").equals("e"     )?"good":"bad"));
       System.out.println("Test 108 "+(tc1.Array2[1].size()==3                ?"good":"bad"));
       System.out.println("Test 109 "+(tc1.Array2[1].get("e").equals("f"     )?"good":"bad"));
       System.out.println("Test 110 "+(tc1.Array2[1].get("f").equals("g"     )?"good":"bad"));
       System.out.println("Test 111 "+(tc1.Array2[1].get("g").equals("h"     )?"good":"bad"));
       System.out.println("Test 112 "+(tc1.HashMap1 .get("a").equals("b"     )?"good":"bad"));
       System.out.println("Test 113 "+(tc1.HashMap1 .get("c").equals("d"     )?"good":"bad"));
       System.out.println("Test 114 "+(tc1.Name1             .equals("name"  )?"good":"bad"));

       TestClass8 tc8 = (TestClass8)createObject(TestClass8.class,new String[]{"HashMap={}","Value=null"});
       System.out.println("Test 115 "+(tc8.HashMap != null                    ?"good":"bad"));
       System.out.println("Test 116 "+(tc8.Value   == null                    ?"good":"bad"));
       tc8 = (TestClass8)createObject(TestClass8.class,new String[]{"Value=\"null\""});
       System.out.println("Test 117 "+((tc8.Value!=null&&tc8.Value.equals("null"))?"good":"bad "+tc8.Value));
       tc8 = (TestClass8)createObject(TestClass8.class,new String[]{"HashMap={"                  +"\r\n"
                                                                            +"{a,b}={a=b,c=d}"   +"\r\n"
                                                                            +"{c,d}={b=e,c=f}"   +"\r\n"
                                                                            +"}"                 +"\r\n"});
       ArrayList<String> ab = new ArrayList<String>();ab.add("a");ab.add("b");
       HashMap<String,String> hmab = tc8.HashMap.get(ab);
       System.out.println("Test 118 "+(hmab.get("a").equals("b")              ?"good":"bad"));
       try{createObject(TestClass1.class,new String[]{"Name1= \\"});
       System.out.println("Test 119 bad no catch");}
       catch (MyParseException e){System.out.println("Test 119 "+(e.getMessage().contains("ParmParserImpl.getToken unexpected \\"           )?"good":"bad"));
                                  System.out.println("Test 119 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Name1= \"a\" \\"});
       System.out.println("Test 120 bad no catch");}
       catch (MyParseException e){System.out.println("Test 120 "+(e.getMessage().contains("ParmParserImpl.getToken unexpected EOF{"         )?"good":"bad"));
                                  System.out.println("Test 120 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Name1= \"a\" \\ {"});
       System.out.println("Test 121 bad no catch");}
       catch (MyParseException e){System.out.println("Test 121 "+(e.getMessage().contains("ParmParserImpl.getToken unexpected symbol{"      )?"good":"bad"));
                                  System.out.println("Test 121 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Name2= a"});
       System.out.println("Test 122 bad no catch");}
       catch (MyParseException e){System.out.println("Test 122 "+(e.getMessage().contains("ParmParserImpl.doAssignment No such field{"      )?"good":"bad"));
                                  System.out.println("Test 122 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc8 = (TestClass8)createObject(TestClass8.class,new String[]{"HashMap2={{a=b c=d}={e=f}}"});
       HashMap<String,String> hmabcd = new HashMap<String,String>();
       hmabcd.put("a","b");
       hmabcd.put("c","d");
       HashMap<String,String> hashMap2 = tc8.HashMap2.get(hmabcd);
       System.out.println("Test 123 "+(hashMap2.get("e").equals("f"          )?"good":"bad"));
       int[] array1={1,2,3,4,5};
       updateObject(array1,new String[]{"{2,4,6,8,10}"});
       System.out.println("Test 124 "+(array1[0]==2                           ?"good":"bad"));
       System.out.println("Test 125 "+(array1[1]==4                           ?"good":"bad"));
       System.out.println("Test 126 "+(array1[2]==6                           ?"good":"bad"));
       System.out.println("Test 127 "+(array1[3]==8                           ?"good":"bad"));
       System.out.println("Test 128 "+(array1[4]==10                          ?"good":"bad"));
       try{updateObject(array1,new String[]{"{1,2,3,4,5,6}"});
       System.out.println("Test 129 bad no catch");}
       catch (MyParseException e){System.out.println("Test 129 "+(e.getMessage().contains("copyArrayListToArray Too many elements for array{")?"good":"bad"));
                                  System.out.println("Test 129 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"bogusFileName"});
       System.out.println("Test 130 bad no catch");}
       catch (MyIOException    e){System.out.println("Test 130 "+(e.getMessage().contains("vetInputFile File doesn't exist{"                 )?"good":"bad"));
                                  System.out.println("Test 130 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"Array1=def Testfile1.inp"});
       System.out.println("Test 131 "+(tc1.Array1[0].equals("def"             )?"good":"bad"));
       System.out.println("Test 132 "+(tc1.Array1[1].equals("abc"             )?"good":"bad"));
       updateObject(tc1,new String[]{"Array1=def Testfile1.inp"});
       System.out.println("Test 133 "+(tc1.Array1[0].equals("def"             )?"good":"bad"));
       System.out.println("Test 134 "+(tc1.Array1[1].equals("abc"             )?"good":"bad"));
       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"{Array1=def Testfile1.inp}"});
       System.out.println("Test 135 "+(tc1.Array1[0].equals("def"             )?"good":"bad"));
       System.out.println("Test 136 "+(tc1.Array1[1].equals("abc"             )?"good":"bad"));
       updateObject(tc1,new String[]{"{Array1=def Testfile1.inp}"});
       System.out.println("Test 137 "+(tc1.Array1[0].equals("def"             )?"good":"bad"));
       System.out.println("Test 138 "+(tc1.Array1[1].equals("abc"             )?"good":"bad"));
       try{createObject(TestClass1.class,new String[]{"Array1 [0]"});
       System.out.println("Test 139 bad no catch");}
       catch (MyParseException e){System.out.println("Test 139 "+(e.getMessage().contains("ParseObject unexpected symbol{"                   )?"good":"bad"));
                                  System.out.println("Test 139 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{createObject(TestClass1.class,new String[]{"Array1 \"}\""});
       System.out.println("Test 140 bad no catch");}
       catch (MyIOException    e){System.out.println("Test 140 "+(e.getMessage().contains("vetInputFile File doesn't exist{"                 )?"good":"bad"));
                                  System.out.println("Test 140 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       ArrayList<String> arrayList = new ArrayList<String>();
       arrayList.add("a");
       try {updateObject(arrayList,new String[]{"[c d]"});
            System.out.println("Test 141 bad no catch");}
       catch (MyParseException e){System.out.println("Test 141 "+(e.getMessage().contains("parse Top level collection"                       )?"good":"bad"));}
       createTestFile("file456","{4 5 6}");
       createTestFile("file789","{7 8 9}");
       int[] array = (int[])createObject(int[].class,new String[]{"{0 1 2 3} file456 file789 {10 11 12}"});
       System.out.println("Test 143 "+(array[ 0] ==  0                          ?"good":"bad"));
       System.out.println("Test 144 "+(array[ 1] ==  1                          ?"good":"bad"));
       System.out.println("Test 145 "+(array[ 2] ==  2                          ?"good":"bad"));
       System.out.println("Test 146 "+(array[ 3] ==  3                          ?"good":"bad"));
       System.out.println("Test 147 "+(array[ 4] ==  4                          ?"good":"bad"));
       System.out.println("Test 148 "+(array[ 5] ==  5                          ?"good":"bad"));
       System.out.println("Test 149 "+(array[ 6] ==  6                          ?"good":"bad"));
       System.out.println("Test 150 "+(array[ 7] ==  7                          ?"good":"bad"));
       System.out.println("Test 151 "+(array[ 8] ==  8                          ?"good":"bad"));
       System.out.println("Test 152 "+(array[ 9] ==  9                          ?"good":"bad"));
       System.out.println("Test 153 "+(array[10] == 10                          ?"good":"bad"));
       System.out.println("Test 154 "+(array[11] == 11                          ?"good":"bad"));
       System.out.println("Test 155 "+(array[12] == 12                          ?"good":"bad"));
       updateObject(array,new String[]{"{20 21 22 23} file456 file789 {20 21 22}"});
       System.out.println("Test 156 "+(array[ 0] == 20                          ?"good":"bad"));
       System.out.println("Test 157 "+(array[ 1] == 21                          ?"good":"bad"));
       System.out.println("Test 158 "+(array[ 2] == 22                          ?"good":"bad"));
       System.out.println("Test 159 "+(array[ 3] == 23                          ?"good":"bad"));
       System.out.println("Test 160 "+(array[ 4] ==  4                          ?"good":"bad"));
       System.out.println("Test 161 "+(array[ 5] ==  5                          ?"good":"bad"));
       System.out.println("Test 162 "+(array[ 6] ==  6                          ?"good":"bad"));
       System.out.println("Test 163 "+(array[ 7] ==  7                          ?"good":"bad"));
       System.out.println("Test 164 "+(array[ 8] ==  8                          ?"good":"bad"));
       System.out.println("Test 165 "+(array[ 9] ==  9                          ?"good":"bad"));
       System.out.println("Test 166 "+(array[10] == 20                          ?"good":"bad"));
       System.out.println("Test 167 "+(array[11] == 21                          ?"good":"bad"));
       System.out.println("Test 168 "+(array[12] == 22                          ?"good":"bad"));
       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"ArrayList1 = 1 ArrayList1 = 2"});
       System.out.println("Test 169 "+(tc1.ArrayList1.get(0).equals(1)          ?"good":"bad"));
       System.out.println("Test 170 "+(tc1.ArrayList1.get(1).equals(2)          ?"good":"bad"));
       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"Array1=a Array1=b"});
       System.out.println("Test 171 "+(tc1.Array1[0].equals("a")                ?"good":"bad"));
       System.out.println("Test 172 "+(tc1.Array1[1].equals("b")                ?"good":"bad"));
       try {createObject(int[].class,new String[]{"{0 1 2 3} file456 file789 [10 11 12]"});
       System.out.println("Test 173 bad no catch");}
       catch (MyParseException e){System.out.println("Test 173 "+(e.getMessage().contains("parseArray_ unexpected symbol{")?"good":"bad "+e.getMessage()));
                                  System.out.println("Test 173 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"{ fileName ]"});
       System.out.println("Test 174 bad no catch");}
       catch (MyParseException e){System.out.println("Test 174 "+(e.getMessage().contains("ParseObject unexpected symbol{")?"good":"bad "+e.getMessage()));
                                  System.out.println("Test 174 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try {createObject(TestClass1.class,new String[]{"{ fileName }"});
       System.out.println("Test 175 bad no catch");}
       catch (MyIOException e){System.out.println("Test 175 "+(e.getMessage().contains("vetInputFile File doesn't exist{")?"good":"bad "+e.getMessage()));
                                  System.out.println("Test 175 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       String result = StructuredMessageFormatter.format(null);
       System.out.println("Test 176 "+(result == null                           ?"good":"bad"));
       try{result=StructuredMessageFormatter.format("{");
           System.out.println("Test 177 bad no catch");}
       catch (MyParseException e){System.out.println("Test 177 "+(e.getMessage().contains("StructuredMessageFormatter.format invalid prefix{")?"good":"bad "+e.getMessage()));
                                  System.out.println("Test 177 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}
       try{result=StructuredMessageFormatter.format("prefix\t");
           System.out.println("Test 178 bad no catch result="+result);}
       catch (MyParseException e){System.out.println("Test 178 "+(e.getMessage().contains("StructuredMessageFormatter.format expecting open brace{")?"good":"bad "+e.getMessage()));
                                  System.out.println("Test 178 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}

       result = StructuredMessageFormatter.format("prefix{}");
       System.out.println("Test 179 "+(result.equals("prefix\n{}\n")              ?"good":"bad\n"+result));

       result = StructuredMessageFormatter.format("prefix{value}");
       System.out.println("Test 180 "+(result.equals("prefix\n"
                                                    +"{value}\n")                 ?"good":"bad\n"+result));

       result = StructuredMessageFormatter.format("prefix{value\tvalue2\t\tvalue3}");
       System.out.println("Test 181 "+(result.equals("prefix\n"+
                                                    "{\n"
                                                    +" value\n"
                                                    +" value2\n"
                                                    +"\n"
                                                    +" value3\n"
                                                    +"}\n")                       ?"good":"bad\n"+result));

       result = StructuredMessageFormatter.format("prefix{value\tvalue2\t\tvalue3}");
       System.out.println("Test 182 "+(result.equals("prefix\n"+
                                                    "{\n"
                                                    +" value\n"
                                                    +" value2\n"
                                                    +"\n"
                                                    +" value3\n"
                                                    +"}\n")                       ?"good":"bad\n"+result));

       result = StructuredMessageFormatter.format("prefix{value\tvalue2\t\t\tvalue3}");
       System.out.println("Test 183 "+(result.equals("prefix\n"+
                                                    "{\n"
                                                    +" value\n"
                                                    +" value2\n"
                                                    +"\n"
                                                    +"\n"
                                                    +" value3\n"
                                                    +"}\n")                       ?"good":"bad\n"+result));

       result = StructuredMessageFormatter.format("prefix{type:value\tvalue2\t\t\tvalue3}");
       System.out.println("Test 184 "+(result.equals("prefix\n"+
                                                    "{type:\n"
                                                    +" value\n"
                                                    +" value2\n"
                                                    +"\n"
                                                    +"\n"
                                                    +" value3\n"
                                                    +"}\n")                       ?"good":"bad\n"+result));

       try{result=StructuredMessageFormatter.format("prefix{value\t");
           System.out.println("Test 185 bad no catch result="+result);}
       catch (MyParseException e){System.out.println("Test 185 "+(e.getMessage().contains("StructuredMessageFormatter.parseBlock unexpected EOF")?"good":"bad "+e.getMessage()));
                                  System.out.println("Test 185 "+e.getMessage());
                                  try{System.out.print(StructuredMessageFormatter.format(e.getMessage()));}catch (MyParseException e2){System.out.println(e2.toString());}}

//collection={ ArrayList
//            { ArrayList
//             { []
//              { []
//               { HashMap
//                {a=b}=a
//               }
//              }
//             }
//            }
//           }"
       TestClass9 tc9 = (TestClass9)createObject(TestClass9.class,new String[]{"collection={{{{{{a=b}=c}}}}}"});
       System.out.println("tc9.collection="+Utilities.toString(tc9.collection));
       System.out.println("tc9.collection="+tc9.collection.getClass());
       try {System.out.println("tc9="+Utilities.toString(tc9));}catch(Exception e){}
       System.out.println("Test 186 "+(tc9.collection.get(0).get(0).get(0)[0][0].size()==1             ?"good":"bad"));
       boolean[] boolean_ = (boolean[])createObject(boolean[].class,new String[]{"{true}"});
       System.out.println("Test 187 "+(boolean_[0]                                                     ?"good":"bad"));
       byte   [] byte_    = (byte[]   )createObject(byte   [].class,new String[]{"{1}"});
       System.out.println("Test 188 "+(byte_   [0] == 1                                                ?"good":"bad"));
       char   [] char_    = (char[]   )createObject(char   [].class,new String[]{"{1}"});
       System.out.println("Test 189 "+(char_   [0] == '1'                                              ?"good":"bad"));
       double [] double_  = (double[] )createObject(double [].class,new String[]{"{1}"});
       System.out.println("Test 190 "+(double_ [0] == 1                                                ?"good":"bad"));
       float  [] float_   = (float  [])createObject(float  [].class,new String[]{"{1}"});
       System.out.println("Test 191 "+(float_  [0] == 1                                                ?"good":"bad"));
       int    [] int_     = (int    [])createObject(int    [].class,new String[]{"{1}"});
       System.out.println("Test 192 "+(int_    [0] == 1                                                ?"good":"bad"));
       long   [] long_    = (long   [])createObject(long   [].class,new String[]{"{1}"});
       System.out.println("Test 193 "+(long_   [0] == 1                                                ?"good":"bad"));
       short  [] short_   = (short  [])createObject(short  [].class,new String[]{"{1}"});
       System.out.println("Test 194 "+(short_  [0] == 1                                                ?"good":"bad"));
       tc1 = (TestClass1)createObject(TestClass1.class,new String[]{"HashMap1={a=b} HashMap1={c=d}"});
       System.out.println("Test 195 "+(tc1.HashMap1.get("a").equals("b") &&
                                       tc1.HashMap1.get("c").equals("d")                               ?"good":"bad"));
                         updateObject(tc1             ,new String[]{"HashMap1={e=f}"});
       System.out.println("Test 196 "+(tc1.HashMap1.get("e").equals("f") &&
                                       tc1.HashMap1.size() == 1                                        ?"good":"bad"));
                         updateObject(tc1             ,new String[]{"HashMap1={}"});
       System.out.println("Test 197 "+(tc1.HashMap1.size() == 0                                        ?"good":"bad"));
                         updateObject(tc1             ,new String[]{"HashMap1=null"});
       System.out.println("Test 198 "+(tc1.HashMap1 == null                                            ?"good":"bad"));
      }
  catch (Exception e){try {String msg="main Exception{"     
                                     +"exception="+e.toString()     +"\t"
                                     +"nest="     +Utilities.nest(e)+"}";
                           System.err.println(msg);
                           System.err.print(StructuredMessageFormatter.format(msg));}
                      catch (MyIOException    e2){System.out.println(e2.toString());}
                      catch (MyParseException e2){System.out.println(e2.toString());}}
}

  /*
  {{ object data }}
  */
  /* 
    Think up odd cases
    Run code coverage tool
    Use it
   
    
    test cases: 
      1. file name
      2. name=value
      3. arrayname={value,value,value...}
      4. arrayname=value arrayname=value
      5. construct from string.
      6. mismatched braces
      7. extra data
      8. branch test.
      9. invalid name.
     10. Create Class object.
     11. Array of arrays.
     12. HashMap with object as key.
     13. Array with ,,
     14. "{="
     15. Internal classes.
     16. Nested file names
     17. Array split across files.
     18. Set private elements.
     19. Trailing data in file.
     20. createObject
         array target
         colFile1 [a b c]
         colFile2 colFile3 [d e f]
               [g]
               colFile4 [h i j]
     21. updateObject
         array target
         colFile1 [a b c]
         colFile2 colFile3 [d e f]
               [g]
               colFile4 [h i j]
     22. createObject
         collection target
         colFile1 [a b c]
         colFile2 colFile3 [d e f]
               [g]
               colFile4 [h i j]
     23. updateObject
         collection target
         colFile1 [a b c]
         colFile2 colFile3 [d e f]
               [g]
               colFile4 [h i j]
     24. createObject
         map target
         mapFile1 {a=a b=b c=c}
         mapFile2 mapFile3 {d=d e=e f=f}
               {g=g}
               mapFile4 {h=h i=i j=j}
     25. updateObject
         map target
         mapFile1 {a=a b=b c=c}
         mapFile2 mapFile3 {d=d e=e f=f}
               {g=g}
               mapFile4 {h=h i=i j=j}
     26. createObject
         object target
         mapFile1 {a=a b=b c=c}
         mapFile2 mapFile3 {d=d e=e f=f}
               {g=g}
               mapFile4 {h=h i=i j=j}
     27. updateObject
         object target
         mapFile1 {a=a b=b c=c}
         mapFile2 mapFile3 {d=d e=e f=f}
               {g=g}
               mapFile4 {h=h i=i j=j}
   
     28. Can I really allow array=value rigorously?  It can occur at (at least) two levels, top and bottom.
         Suppose HashMap<String[],String> hMap
   
         Do "hMap={{a}=b}" and "hMap={a=b}" 
   
       
  */

 public static void setReadBufferSize(int readBufferSize){ParmParserImpl.setReadBufferSize(readBufferSize);}

 public static Object updateObject(Object target, String[] parms)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Utilities.debug())System.err.println("updateObject enter{"
                                          +"parms="+Utilities.toString(parms)+"}");
  Object result = ParmParserImpl.parse(target,parms,false);
  if (Utilities.debug())System.err.println("updateObject return");
  return result;
 }

 public static String usage(Class<?> class_)
 {
  return ParmParserImpl.usage(class_);
 }
}