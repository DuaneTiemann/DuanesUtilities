package com.duane.utilities;
import  java.io.File;
import  java.io.FileNotFoundException;
import  java.io.FileReader;
import  java.io.InputStreamReader;
import  java.io.IOException;
import  java.io.StringReader;
import  java.lang.Class;
import  java.lang.ClassNotFoundException;
import  java.lang.IllegalAccessException;
import  java.lang.InstantiationException;
import  java.lang.Package;
import  java.lang.String;
import  java.lang.StringBuffer;
import  java.lang.annotation.Annotation;
import  java.lang.reflect.Array;
import  java.lang.reflect.Constructor;
import  java.lang.reflect.Field;
import  java.lang.reflect.InvocationTargetException;
import  java.lang.reflect.Type;
import  java.lang.reflect.TypeVariable;
import  java.util.AbstractCollection;
import  java.util.AbstractMap;
import  java.util.ArrayList;
import  java.util.Arrays;
import  java.util.Collection;
import  java.util.Collections;
import  java.util.HashMap;
import  java.util.HashSet;
import  java.util.Map;
import  java.util.Map.Entry;
import java.util.Set;

import  com.duane.utilities.CompileDate;
import  com.duane.utilities.MyParseException;
import  com.duane.utilities.MyStreamTokenizer;
import  com.duane.utilities.MyStreamTokenizer.Parms;
import  com.duane.utilities.ParmParserParm;
import  com.duane.utilities.StructuredMessageFormatter;
import  com.duane.utilities.StructuredMessageFormatter2;
import  com.duane.utilities.Utilities;

/**
The ParmParserImpl class maps a json-like parameter specification to java objects.
The intent is that parameters may be provided on the command line and/or in config files.

Braces can not be used to both bound arrays and constructor parms because of examples like the following:
An object has an element which is an array of objects of class c which take 1 and 2 string constructors.
element={a,b} could be taken to mean a 2 element array of c objects using its single string constructor or a 1 element array 
of class c using its 2 string constructor. So let's use parens to bound constructor arguments. 

Could braces still be used to bound arrays and to house object elements?
Suppose class c constructors take a string in a single argument constructor.  Then for an element which is an array of c's,
element={a,b} could be taken to mean 1 instance of the class or 2 instances which take a single argument constructor.
Ouch.  The current implementation uses brackets for arrays.  But that is unnatural.  Once we decide to have constructor arguments in parens, we're freed
to reconsider braces for both objects and arrays.  Right now, I don't see a counter example.
One case to consider is how to distinguish the single element form versus the array form.  The key seems to be whether or not the elements, if any, are name=value or just values.
Note we can still have {{name=value}={name=value}}, so that decision needs to be careful.

Single element arrays may omit the brackets (braces) as we already know they are arrays from the class definition.
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
//        But right now, MyStreamTokenizer does not distinguish null from "null".  It returns null as EOF.
//  3. How do we know when to append to arrays and when to replace them?  
//    A. Auxiliary structure to track arrays. 
// 4. Allowing target to be Class or Object is inband signalling.  That's a no no.  That means Class objects can not be created. 
//    I could always create the target object.  main could use ParmParserImpl to create the parent class. 
// 5. What about comments?   #, /* */, //
//    A. All handled by MyStreamTokenizer
// 6. Should I throw IOException and ParseException or just my own ParseException.
//    A. My own ParseException.  That allows more consistent formatting.  Offset does not have to be separate parameter which was awkward.
//       IOException would have been awkward to match to where().
// 7. What is the strategy for log and debug files?
// 8. The parse routine should be static so no one has to create the ParmParserImpl object just to call it.
// 9. Should I narrow the visibility of ParmParserImpl object internals by using an implementation class?
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
//        I need to treat maps similar to collections and arrays.
//        Note that Maps can have an object as a key.
// 22. If target is a Map to be updated, how to ensure that it is initialized?
// 23. Should Maps allow file names?
// 24. How do I handle quoted symbols?
// 25. How do I tell depth of nested collections?
// 36. Do I really need to check the depth of multi-dimension arrays?  Why not just say only single dimension can have shortcut.
// 37. Do I really need copyCollection and copyMap?  Why not just create in doAssignment and then update?
// 38. Should missing classes cause us to abort?
// 39. The createFlag and created parms are messy.  Especially since I now need to pass created as in/out no less.  The issue is that parseFile may
//     create the object and a subsequent one may not know about it.  This seems to work now, but it seems sloppy.
// 40. Does the depth counting really work?  
//     Suppose an arraylist of objects with string constructors.
//     ArrayList<Object> field Depth     =1
//     field={a,b,{name=c}}    InputDepth=1
//     field={{name=a},b,c}    InputDepth=2
//     field={name=a}          InputDepth=1
//     field=a                 InputDepth=0
// 41. Should "field={name=a,b}" be legal?
//     So what does "field={name=a}" mean?  Are the braces for the object or for the array?  Is there a contradiction in this?  It seems ambiguous.
// 42. Suppose we say that natural bracing is in effect, but if no brace is encountered when expected, one can be added with a lazy match.
//     Thus "field={name=a,b}" becomes "field={{name=a},b}".
//     Thus, an input brace is always given its highest possible meaning.
// 43. Should we really support top level ArrayLists and HashMaps? The problem is that they have no generic information.  We only have generic info for fields.
// 44. File name should not be scanned in low level object
// 45. Do classes need to be static?
// 46. Why do I need default constructor?
// 47. Should field names be case sensitive?  Maybe set a flag.
//     
// TODO 
//  1. Allow annotation of fields to limit parms. 
//  2. Allow more general construction parameters. 
//  3. Format messages
//  4. Figure out logging interface.
//  5. Allow class designations in input.
//  6. Support file names for arrays, collections and maps as highest level targets.
//  7. Make threadsafe
//  8. Use braces instead of brackets.  This forces us to do file names at the top level only.  
    
//  9. Suppose we have array = {{}}
//                     array = {{}}
//     That should mean the high level array has 2 elements which are 2 empty arrays.  Same as {{},{}}
//     Watch out for arrays with a specified number of elements.
// 10. handle empty braces
// 11. optimize scan for = to not do it for mundane classes.
// 12. allow null values. 
// 13. Defend recursion in usage.
// 14. ArrayLists and maps in usage.
// 15. Generic doc in usage.
// 16. Inherent Parms.
 

class ParmParserImpl
{
 private static class BooleanWrapper 
 {
  public boolean value=false;
  public String toString(){return ""+value;}
 }

 private static enum ClassType
 {
  ARRAY      ,
  COLLECTION ,
  MAP        ,
  OBJECT     ,
  PRIMITIVE  
 }

 private static class Pair implements Comparable<Pair>
 {
  private String Key   = null;
  private String Value = null;
  public String getKey  (){return Key  ;}
  public String getValue(){return Value;}
  public Pair(String key,String value){Key=key;Value=value;}
  @Override
  public int compareTo(Pair pair)
  {
   if (Key.length() < pair.Key.length())return  1;
   if (Key.length() > pair.Key.length())return -1; 
   return Key.compareTo(pair.Key);
  }
 }

 private static HashMap<Class<?>,String> UsageClassCache = new HashMap<Class<?>,String>();
 private static boolean                  Debug           = false;
 private static int                      ReadBufferSize  = 8192;
 private static HashMap<String,String>   Substitutions   = null;
 private static ArrayList<Pair>          SubstitutionsAR = null;

 private static Object constructObject(Class<?> class_,String parm, MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  if (Debug)debug("constructObject{"
                                          +"class_="+class_+"\t"
                                          +"parm="  +parm  +"}");
  try {
       return tryConstructor(parm==null?class_.getConstructor(new Class<?>[]{            })
                                       :class_.getConstructor(new Class<?>[]{String.class}),
                             parm,parms);
      }
  catch (Exception e)
        {
         if (Debug)debug("constructObject explicit constructor failed{"
                                    +"class_="           +class_           +"\t"
                                    +"exception.class="  +e.getClass()     +"\t"
                                    +"exception.message="+e.getMessage()   +"\t"
                                    +"nest="             +Utilities.nest(e)+"\t"
                                    +"parm="             +parm             +"}");
        }

  if (Debug)debug("constructObject 2{"
                                          +"class_="+class_+"\t"
                                          +"parm="  +parm  +"}");

  Constructor<?>[] constructors = class_.getDeclaredConstructors();
  if (constructors == null || constructors.length == 0)throw new MyParseException("constructObject No constructors{"
                                                                                 +"class_="+class_                   +"\t"
                                                                                 +"nest="  +Utilities.nest()         +"\t"
                                                                                 +"parm="  +parm                     +"\t"
                                                                                 +"where=" +parms.whereCurrentToken()+"}");
  for (Constructor<?> constructor : constructors)
      {
       try {
            Object result = tryConstructor(constructor,parm,parms);
            if (result == null)continue;
            if (Debug)debug("constructObject implicit constructor worked{"
                                       +"class_="     +class_     +"\t"
                                       +"constructor="+constructor+"\t"
                                       +"parm="       +parm       +"\t"
                                       +"result="     +result     +"}");
            return result;
           }
       catch (Exception e){throw new MyParseException("tryConstructor exception{"
                                                     +"class_="   +class_                   +"\t"
                                                     +"exception="+Utilities.toString(e)    +"\t"
                                                     +"parm="     +parm                     +"\t"
                                                     +"where="    +parms.whereCurrentToken()+"}");}
      }
  throw new MyParseException("constructObject failed{"
                            +"class_="+class_                   +"\t"
                            +"parm="  +parm                     +"\t"
                            +"where=" +parms.whereCurrentToken()+"}");
 }

 @SuppressWarnings("unchecked")
 private static void addAll(ArrayList<Object> target, Object source)
 {
  if (source instanceof AbstractCollection<?>)target.addAll((AbstractCollection<Object>)source);
  else                                        copyArrayToArrayList(source,target);
 }

 private static boolean appendParmParserParms(StringBuffer result, ParmParserParm[] parmParserParms, String name, boolean anyFieldFound)
 {
  boolean thisFieldFound = false;
  for (ParmParserParm parmParserParm : parmParserParms)
      {
       if (Debug)Utilities.debug("ParmParserImpl.appendParmParserParms{"
                                +"parmParserParm="+parmParserParm.toString()+"}");
       String value = parmParserParm.value();

       if (value != null & value.length()>0)
          {
           if (anyFieldFound)result.append("\t");
           result.append(name).append("=");
           thisFieldFound = splitAppendString(result,value,anyFieldFound);
          }
      }
  return thisFieldFound;
 }

 private static Object copyArrayListToArray(ArrayList<Object> arrayList,Object array, MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  if (arrayList == null)return null;      // impossible
  if (array     == null)return null;      // impossible
  if (arrayList.size() > Array.getLength(array))throw new MyParseException("copyArrayListToArray Too many elements for array{"
                                                                          +"sourceSize="+arrayList.size()         +"\t"
                                                                          +"targetSize="+Array.getLength(array)   +"\t"
                                                                          +"where="     +parms.whereCurrentToken()+"}");
  for (int i=0;i<arrayList.size();i++)
       Array.set(array,i,arrayList.get(i));
  return array;
 }

 private static void copyArrayToArrayList(Object array,ArrayList<Object> arrayList)
 {
  if (arrayList == null)return;        // impossible
  if (array     == null)return;        // impossible
  for (int i=0;i<Array.getLength(array);i++)
       arrayList.add(Array.get(array,i));
 }

 private static void copyArrayToArrayListStart(Object array,ArrayList<Object> arrayList)
 {
  if (arrayList == null)return;        // impossible
  if (array     == null)return;        // impossible
  for (int i=0;i<Array.getLength(array);i++)
       arrayList.add(i,Array.get(array,i));
 }

 @SuppressWarnings("unchecked")
 private static void copyCollection(Object value, Object target, Field field)
 throws MyIOException, MyParseException
 {
  if (Debug)debug("copyCollection enter{"
                             +"target="+Utilities.toString(target)+"\t"
                             +"value=" +Utilities.toString(value )+"}");
  if (value == null)return;
  try {
       Collection<Object> targetCollection = (Collection<Object>)field.get(target);
       targetCollection.addAll((Collection<Object>)value);
      }
  catch(IllegalAccessException e){throw new MyParseException("ParmParserImpl.copyCollection exception{"
                                                            +"e="+e.toString()+"}");}
  if (Debug)debug("copyCollection return{"
                             +"target="+Utilities.toString(target)+"}");
 }

 @SuppressWarnings("unchecked")
 private static void copyMap(Object value, Object target, Field field)
 throws MyParseException
 {
  if (value == null)return;
  try {
       Map<Object,Object> targetMap = (Map<Object,Object>)field.get(target);
       targetMap.putAll((Map<Object,Object>)value);
      }
  catch(IllegalAccessException e){throw new MyParseException("ParmParserImpl.copyMap exception{"
                                                            +"e="+e.toString()+"}");}
 }

 private static int countArrayDimensions(String genericType)
 {
  if (genericType.length() <= 2)return 0;
  int nDimensions = 0;
  for (int offset=genericType.length()-2;offset>0;offset-=2)
       if (genericType.charAt(offset)=='[' && genericType.charAt(offset+1)==']')nDimensions++;
       else break;
  return nDimensions;
 }

// private static int countOpenBrackets(MyStreamTokenizer parms)
// throws MyIOException, MyParseException
// {
//  parms.lockPosition();     // We could have lots of open brackets. It doesn't seem likely, but it is possible.
//                            // We have to ensure that our earliest one is not trimmed.
//  int nOpenBrackets = 0;
//  for (;;nOpenBrackets++)
//      {
//       String token = parms.getToken();
//       if (token == null)throw new MyParseException("ParmParserImpl.countOpenBrackets unexpected EOF{"
//                                                   +"where="+parms.where()+"}");
//       if (!token.equals("["))break;
//      }
//  for (int i=0;i<nOpenBrackets+1;i++)parms.ungetToken();
//  parms.unlockPosition();
//  return nOpenBrackets;
// }

// private static Field createField(Object target, String name, MyStreamTokenizer parms)
// throws MyIOException, MyParseException
// {
//  try {return target.getClass().getField(name);}
//  catch (NoSuchFieldException e){throw new MyParseException(e.toString()+parms.where());}
// }


 private static String createArrayName(Class<?> class_, int dimensions)
 throws MyParseException
 {
  if (dimensions == 0)return class_.getName();
  String prefix = getClassNamePrefix(class_);
  return Utilities.repeat("[",dimensions)+prefix+(prefix.equals("L")?Utilities.endWith(class_.getName(),";"):"");
 }

 // Not in use right now.
 private static Class<?> createClassArray(Class<?> class_, int dimensions)
 throws MyParseException
 {
  try{return Class.forName(createArrayName(class_,dimensions));}
  catch (ClassNotFoundException e){throw new MyParseException("createClassArray internal error. class not found"
                                                             +"class_="    +class_.getName()+"\t"
                                                             +"dimensions="+dimensions      +"}");}
 }

 public static Object createPrimitive(Class<?>targetClass,String token,MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  if (targetClass.equals(boolean.class))return Boolean.valueOf(token).booleanValue();
  if (targetClass.equals(byte.class   ))return Byte   .valueOf(token).byteValue   ();
  if (targetClass.equals(char.class   ))return token.charAt(0)                      ;
  if (targetClass.equals(double.class ))return Double .valueOf(token).doubleValue ();
  if (targetClass.equals(float.class  ))return Float  .valueOf(token).floatValue  ();
  if (targetClass.equals(int.class    ))return Integer.valueOf(token).intValue    ();
  if (targetClass.equals(long.class   ))return Long   .valueOf(token).longValue   ();
  if (targetClass.equals(short.class  ))return Short  .valueOf(token).shortValue  ();
  throw new MyParseException("ParmParserImpl.createPrimitive unknown fieldClass{"  // This should be impossible.
                            +"targetClass="+targetClass.toString()   +"\t"
                            +"where="      +parms.whereCurrentToken()+"}");
 }

 @SuppressWarnings("unchecked")
 private static void doAssignment(Object target,String fieldName,HashSet<String>initializedGroups,HashMap<String,ArrayList<Object>> arrays,
                                  MyStreamTokenizer parms,long tokenId)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debugEnter("doAssignment{"
                                  +"fieldName="+fieldName                 +"\t"
                                  +"target="   +Utilities.toString(target)+"}");
  Class<?> targetClass       = target.getClass();
  Field     field            = getField(targetClass,fieldName,parms);
  boolean accessible = field.isAccessible();
  field.setAccessible(true);
  Class<?>  fieldClass       = field.getType();
  if (Debug)debug("doAssignment note fieldClass{"
                             +"fieldClass="+fieldClass+"}");

  Type      fieldGenericType = field.getGenericType();
  if (Debug)debug("doAssignment note fieldGenericType{"
                             +"fieldGenericType="+fieldGenericType+"}");
  Object    value            = parse(fieldClass,fieldGenericType.getTypeName(),parms,null,true,true);
  if (Debug)debug("doAssignment note value{"
                             +"value="+Utilities.toString(value)+"}");
  ClassType classType        = getClassType(fieldClass);
  switch (classType)
         {
          case COLLECTION:
          case MAP       :
               if (!initializedGroups.contains(fieldName))
                  {
                   initializedGroups.add(fieldName);

                   if (Debug)debugEnter("doAssignment note target before constructObject{"
                                                   +"target="+Utilities.toString(target)+"}");
                   try {field.set(target,constructObject(fieldClass,null,parms));}  // Ensure that new values replace default values.
                   catch(IllegalAccessException e){throw new MyParseException("ParmParserImpl.doAssignment exception{"
                                                                             +"e="    +e.toString()            +"\t"
                                                                             +"where="+parms.whereById(tokenId)+"}");}
                  }
               break;
          case ARRAY     :
          default        :
               break;
         }
  if (Debug)debug("doAssignment note target after constructObject{"
                 +"target="+Utilities.toString(target)+"}");
  switch (classType)
         {
          case ARRAY:
               if (Debug)debug("doAssignment Array");
               ArrayList<Object> arrayList = arrays.get(fieldName);
               if (arrayList == null)
                  {
                   arrayList = new ArrayList<Object>(); 
                   arrays.put(fieldName,arrayList);
                  }
               addAll(arrayList,value);
               break;

          case COLLECTION:copyCollection(value,target,field);break;
          case MAP       :copyMap       (value,target,field);break;
          default        :field.set     (target,value)      ;
                          if (Debug)debug("doAssignment after field.set{"
                                         +"fieldName="+fieldName                 +"\t"
                                         +"hashCode=" +target.hashCode()         +"\t"
                                         +"target="   +Utilities.toString(target)+"\t"
                                         +"value="    +value                     +"}");
                          break;
         }

  field.setAccessible(accessible);
  if (Debug)debugReturn("doAssignment return{"
                                   +"fieldName="+fieldName                 +"\t"
                                   +"target="   +Utilities.toString(target)+"}");
 }

 private static char[] angleChars = new char[]{'<','>'};
 
 private static int findClosingAngle(String type)
 {
  int depth=0;
  int pos  =0;
  for (pos = Utilities.indexOf(type,0,angleChars);pos!=-1;pos=Utilities.indexOf(type,pos+1,angleChars))
      {
       if (type.charAt(pos)=='<')depth++;
       else                      depth--;
       if (depth == 0)return pos;
      }
  return pos;
 }

 private static void findClosingBrace(MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  int depth=1;
  for (String token=parms.getToken();token!=null;token=parms.getToken())
      {
       if (parms.isSymbol())
          {
           if (token.equals("{"))depth++;
           if (token.equals("}"))depth--;
           if (depth == 0       )return;
          }
      }
  throw new MyParseException("findClosingBrace unexpected EOF{"
                            +"where="+parms.whereCurrentToken()+"}");
 }

 /**
  * returns nested depth to highest level object.
  * 
  * @author Duane (8/14/2015)
  * 
  * @param parms 
  * @param genericType 
  * 
  * @return int 
  */
 private static int findGroupDepth(MyStreamTokenizer parms, String genericType)
 throws MyIOException, MyParseException
 {
  if (Debug)debugEnter("findGroupDepth enter{"
                                  +"genericType="+genericType              +"\t"
                                  +"where="      +parms.whereCurrentToken()+"}",true);
  int result = findGroupDepth_(parms,genericType);
  if (Debug)debugReturn("findGroupDepth return{"
                                   +"genericType="+genericType              +"\t"
                                   +"result="     +result                   +"\t"
                                   +"where="      +parms.whereCurrentToken()+"}",true);
  return result;
 }

 private static int findGroupDepth_(MyStreamTokenizer parms, String genericType)
 throws MyIOException, MyParseException
 {
  int dimensions = 0;
  while (genericType.endsWith("[]"))
        {
         dimensions += 1;
         genericType = genericType.substring(0,genericType.length()-2);
        }
  String    className = getClassName   (genericType);
  Class<?>  class_    = getClassForName(parms,className);
  if (getClassType(class_)==ClassType.COLLECTION)
     {
      String componentClassName = getFirstComponentClassName(genericType);
      return dimensions+1+findGroupDepth(parms,componentClassName);
     }
  return dimensions;
 }  

 private static int findInputGroupDepth(MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  if (Debug)debugEnter("findInputGroupDepth enter{"
                                  +"where="+parms.whereCurrentToken()+"}",true);
  if (parms.getToken()==null)return -1;
  parms.ungetToken();
  parms.lockCurrentToken();
  long currentToken = parms.getCurrentTokenId();
  int  result       = findInputGroupDepth_(parms);
  parms.setCurrentToken(currentToken);
  parms.unlockCurrentToken();
  if (Debug)debugReturn("findInputGroupDepth return{"
                                   +"result="+result       +"\t"
                                   +"where=" +parms.whereCurrentToken()+"}",true);
  return result;
 }

 private static int findInputGroupDepth_(MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  // a         0
  // {a}       1
  // {a a}     1   COLLECTION
  // {a a} = b 0   MAP    
  // {a=b}     0   MAP or OBJECT
  // {{a=b}}   1
  // {a=b}=c   0
  // {}        ?   1
  // {{}}      ?   2
  // {{} {}}   ?   2
  // (({}=a}}  1

  // {{{}}=c}  0

  // {} is ambiguous.  It could mean an object with default members or it could mean an empty array.
  // if componentType is PRIMITIVE, it must mean empty array.
  // if it is MAP or OBJECT, it could be an empty MAP or OBJECT or an empty array.
  // We will treat it as an empty array whenever we can.  More braces must be provided to force it to an empty object.


  int depth = 0;
  while(true)
       {
        String token=parms.getTokenOrDie();
        if (!parms.isSymbol())
           {
            token = parms.getToken();
            if (token == null)return depth;
            if (parms.isSymbol() && token.equals("="))return depth - 1;  // because it is an object or a map.
            else                                      return depth;
           }
        if (token.equals("}"))return depth==0?-1:depth;  // {} is a special case. We return -1 to signal empty braces.

        if (!token.equals("{"))
           {
            parms.ungetToken();
            throw new MyParseException("findInputGroupDepth unexpected symbol{"
                                      +"token="+token        +"\t"
                                      +"where="+parms.where()+"}");
           }
        parms.lockCurrentToken();
        long currentToken = parms.getCurrentTokenId();
        findClosingBrace(parms);
        token = parms.getToken();
        if (token !=null)
           {
            boolean isSymbol = parms.isSymbol();
            parms.ungetToken();

            if (isSymbol && token.equals("="))return depth-1;
           }
        parms.setCurrentToken(currentToken);
        parms.unlockCurrentToken();
        depth++;
       }
 }    

 private static int findUnembeddedComma(String s,int pos)
 {
  int depth = 0;
  char[] scanChars = {'<','>',','};
  for (int pos2=Utilities.indexOf(s,pos,scanChars);pos2!=-1;pos2=Utilities.indexOf(s,pos2+1,scanChars))
      {
       switch (s.charAt(pos2))
              {
               case '<':
                    depth ++;
                    break;
               case '>':
                    depth --; 
                    if (depth <= 0)return -1;
                    break;
               case ',': 
                    if (depth == 1)return pos2;
              }
      }
  return -1; // impossible because the generic string is created by the compiler.
 }

 private static HashMap<String,Class<?>> Classes = new HashMap<String,Class<?>>();

 // If createFlag==true then the target supplied by caller of create/updateObject must be a class.
 // But it may have been overwritten with an object of that class, so it could legally not be a class.
 // This does not violate the ability to create Class objects because the class of a Class object is a Class object.

 private static Class<?> getClass(Object target, boolean createFlag)
 throws MyParseException
 {
  if (!createFlag               )return target.getClass();
  if (target instanceof Class<?>)return (Class<?>)target;
  throw new MyParseException("getClass Internal error target is to be created and is not a class{"
                             +"target class="+target.getClass()+"}");
 }

 static char[] AngleAndBracket = new char[]{'<','['};

 private static Class<?> getClassForName(MyStreamTokenizer parms,String name)
 throws MyIOException, MyParseException
 {
  Class<?> result = getClassForName_(parms,name);
  if (Debug)debug("getClassForName{"
                             +"name="  +name  +"\t"
                             +"result="+result+"}");
  return result;
 }

 private static Class<?> getClassForName_(MyStreamTokenizer parms,String name)
 throws MyIOException, MyParseException
 {
  Class<?> class_ = Classes.get(name);
  if (class_ != null)return class_;

  int pos = Utilities.indexOf(name,AngleAndBracket);
  String trimmedName = pos == -1?name:name.substring(0,pos);
  int    dimensions  = countArrayDimensions(name);
  class_             = getClassForName__(parms,trimmedName);
  if (dimensions > 0)class_ = getClassForName__(parms,createArrayName(class_,dimensions));

  Classes.put(name, class_);
  return class_;
 }

 private static Class<?> getClassForName__(MyStreamTokenizer parms, String name)
 throws MyIOException, MyParseException
 {
  Class<?> class_ = null;
  int      count  = 0;
  try {class_ = Class.forName(name);}
  catch (ClassNotFoundException e)
        {
         if (name.equals("boolean"))return boolean.class;
         if (name.equals("byte"   ))return byte.class   ;
         if (name.equals("char"   ))return char.class   ;
         if (name.equals("double" ))return double.class ;
         if (name.equals("float"  ))return float.class  ;
         if (name.equals("int"    ))return int.class    ;
         if (name.equals("long"   ))return long.class   ;
         if (name.equals("short"  ))return short.class  ;
         Package[] packages = Package.getPackages();
         for (Package package_ : packages) 
             {
              try {class_ = Class.forName(package_.getName()+"."+name);}
              catch (ClassNotFoundException f){continue;}
              count++;
             }
        }
  if (class_ == null)throw new MyParseException("getClassForName__ internal error. class not found{"
                                               +"name="       +name                     +"\t"
                                               +"where="      +parms.whereCurrentToken()+"}");
  if (count > 1)throw new MyParseException("getClassForName__ class name occurs in multiple packages{"
                                          +"count="      +count                    +"\t"
                                          +"name="       +name                     +"\t"
                                          +"where="      +parms.whereCurrentToken()+"}");
  return class_;
 }

 private static String getClassName(String genericType)
 {
  if (genericType == null)return null;
  int pos = genericType.indexOf('<');
  if (pos == -1)return genericType;
  return genericType.substring(0,pos);
 }

 private static String getClassNamePrefix(Class<?> class_)
 {
  if (class_.equals(boolean.class))return "Z";
  if (class_.equals(byte.class   ))return "B";
  if (class_.equals(char.class   ))return "C";
  if (class_.equals(double.class ))return "D";
  if (class_.equals(float.class  ))return "F";
  if (class_.equals(int.class    ))return "I";
  if (class_.equals(long.class   ))return "J";
  if (class_.equals(short.class  ))return "S";
                                   return "L";
 }

 private static ClassType getClassType(Class<?> class_)
 {
  if (class_.isArray    ())return ClassType.ARRAY;
  if (class_.isPrimitive())return ClassType.PRIMITIVE;
  for (Class<?> superclass = class_.getSuperclass();superclass!=null;superclass=superclass.getSuperclass())
      {
       if (superclass.equals(AbstractCollection.class))return ClassType.COLLECTION;
       if (superclass.equals(AbstractMap       .class))return ClassType.MAP;
      }
  return ClassType.OBJECT;
 }

 private static Class<?> getCollectionComponentClass(MyStreamTokenizer parms,String fieldGenericType)
 throws MyIOException, MyParseException
 {
  String componentClassName = getFirstComponentClassName(fieldGenericType);
  if (componentClassName == null)return null;
  int pos = componentClassName.indexOf("<");
  if (pos != -1)
      {
       int pos2 = findClosingAngle(componentClassName);
       if (pos2 == -1)throw new MyParseException("getCollectionComponentClass internal error. no closing angle{"
                                                +"componentClassName="+componentClassName+"\t"
                                                +"fieldGenericType="  +fieldGenericType  +"}");
       componentClassName = componentClassName.substring(0,pos)+componentClassName.substring(pos2+1);  // remove generic info, but keep array dimensions.
      }

  return getClassForName(parms,componentClassName);
 }

 private static String getDequotedToken(MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  return Utilities.dequote(getToken(parms));
 }

 private static String getDequotedTokenOrDie(MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  return Utilities.dequote(getTokenOrDie(parms));
 }

// private static Class<?> getGroupElementClass(Type genericType, Class<?> groupClass, ClassType classType, MyStreamTokenizer parms)
// throws MyIOException, MyParseException
// {
//  String genericTypeName = genericType.getTypeName();
//  String groupClassName  = groupClass.getCanonicalName();
//  if (Debug)debug("getGroupElementClass{"
//                                          +"genericTypeName="+genericTypeName+"\t"
//                                          +"groupClassName=" +groupClassName +"}");
//  if (groupClassName == null)throw new MyParseException("ParmParserImpl.getGroupElementClass groupClassName is null "+parms.where());
//  switch (classType)
//         {
//          case ARRAY:
//               if (!groupClassName.endsWith("[]"))throw new MyParseException("ParmParserImpl.getGroupElementClass groupClassName is not array{"
//                                                                            +"groupClassName="+groupClassName+"\t"
//                                                                            +"location={"     +parms.where() +"}");
//               String elementClassName = groupClassName.substring(0,groupClassName.length()-2);
//               try {return Class.forName(elementClassName);}
//               catch (ClassNotFoundException e){throw new MyParseException(e.toString()+parms.where());} 
//
//          case COLLECTION:
//          case MAP:
//               TypeVariable<?>[] typeVariables = groupClass.getTypeParameters();
//               if (typeVariables == null)throw new MyParseException("ParmParserImpl.getGroupElementClass groupClass has no type parameters{"
//                                                                   +"groupClass="+groupClass   +"\t"
//                                                                   +"where="     +parms.where()+"}");
//               if (Debug)
//                  {
//                   debug("ParmParserImpl.getGroupElementClass note class{"
//                                     +"groupClass="   +groupClass                       +"\t"
//                                     +"typeVariables="+Utilities.toString(typeVariables)+"}");
//                   for (TypeVariable<?> typeVariable : typeVariables)
//                       {
//                        Type[] types =  typeVariable.getBounds();
//                        for (Type type : types)
//                             debug("ParmParserImpl.getGroupElementClass note type{"
//                                               +"type="+type+"}");
//                       }
//                   Type genericSuperclass = groupClass.getGenericSuperclass();
//                   debug("ParmParserImpl.getGroupElementClass note GenericSuperclass{"
//                                     +"genericSuperclass="+genericSuperclass+"}");
//                  }
//
//               return null;
//
//          default: throw new MyParseException("ParmParserImpl.getGroupElementClass internal error bad classType{"
//                                             +"classType="+classType+"}");
//         }
//
// }

 private static String getElementGenericType(MyStreamTokenizer parms, String genericType)
 throws MyIOException, MyParseException
 {
  if (genericType.endsWith("[]"))return genericType.substring(0,genericType.length()-2);
  int pos = genericType.indexOf('<');
  if ((pos == -1))return "";
  if (!genericType.endsWith(">"))throw new MyParseException("getElementGenericType internal error. genericType does not end with > or []{"
                                                           +"genericType="+genericType  +"\t"
                                                           +"where="      +parms.where()+"}");
  return genericType.substring(pos+1,genericType.length()-1);
 }

 private static Field getField(Class<?> targetClass, String fieldName, MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  Field field = null;
  for (Class<?> class_=targetClass;class_!=null;class_=class_.getSuperclass())
      {
       try {field = class_.getDeclaredField(fieldName);break;}
       catch(NoSuchFieldException e){if (Debug)Utilities.debug("ParmParserImpl.getField No such field{"
                                                                             +"class_="     +class_       +"\t"
                                                                             +"field="      +fieldName    +"\t"
                                                                             +"targetClass="+targetClass  +"\t"
                                                              +"where="      +parms.where()+"}");} 
       catch(Exception            e){Utilities.debug("ParmParserImpl.getField exception getting field{"
                                                       +"exception={"+e.toString()  +"\t"
                                                       +"class_="     +class_       +"\t"
                                                       +"field="      +fieldName    +"\t"
                                                       +"targetClass="+targetClass  +"\t"
                                                    +"where="      +parms.where()+"}");}
      }
  if (field == null)throw new MyParseException("ParmParserImpl.getField field not found{" // impossible  due to NoSuchField exception above.
                                              +"field="      +fieldName    +"\t"
                                              +"targetClass="+targetClass  +"\t"
                                              +"where="      +parms.where()+"}");

  ParmParserParm[] parmParserParms = getParmParserParms(field);
  if (parmParserParms        == null ||
      parmParserParms.length == 0)throw new MyParseException("ParmParserImpl.doAssignment field is not marked as a parameter{" 
                                                            +"field="+fieldName         +"\t"
                                                            +"targetClass="+targetClass +"\t"
                                                            +"where="+parms.where()     +"}");
  return field;
 }

 private static String getFirstComponentClassName(String genericType)
 throws MyIOException,MyParseException
 {
  String result = getFirstComponentClassName_(genericType);
  if (Debug)debug("getFirstComponentClassName{"
                             +"genericType="+genericType+"\t"
                             +"result="     +result     +"}",true);
  return result;
 }

 private static String getFirstComponentClassName_(String genericType)
 throws MyIOException,MyParseException
 {
  int pos = genericType.indexOf("<");
  if (pos == -1)return null;
  if (!genericType.endsWith(">"))throw new MyParseException("getComponentClassName internal error. genericType is missing trailing >{"
                                                           +"genericType="+genericType+"}");
  return getGenericClassName(genericType.substring(pos+1,genericType.length()-1));
 }

 private static String getGenericClassName(String genericType)
 throws MyIOException,MyParseException
 {
  if (Debug)debug("getGenericClassName{"
                                          +"genericType="+genericType+"}");
  int pos = findClosingAngle(genericType);

  if (pos == -1)
     {
      pos = genericType.indexOf(',');
      if (pos == -1)return genericType;
      return genericType.substring(0, pos);
     }
  pos ++;
  for (;pos <= genericType.length()-2;pos+=2)
      {
       if (genericType.charAt(pos  )!= '[')break;
       if (genericType.charAt(pos+1)!= ']')break;
      }
  return genericType.substring(0, pos);
 }

 private static String getKeyClassName(String genericType)
 throws MyParseException
 {
  int pos = genericType.indexOf("<");
  if (pos == -1)return "java.lang.String";

  int pos2 = findUnembeddedComma(genericType,pos);
  if (pos2 == -1)throw new MyParseException("ParmParserImpl.getKeyClassName no comma found in genericType{" // impossible becasuse generic type is compiler generated.
                                           +"genericType="+genericType+"}");
  return genericType.substring(pos+1,pos2).trim();
 }

 private static ParmParserParm[] getParmParserParms(Class<?> class_)
 {
  ArrayList<ParmParserParm> result = new ArrayList<ParmParserParm>();
  for (Class<?> class__=class_;class__!=null;class__=class__.getSuperclass())
       result.addAll(Arrays.asList(getParmParserParms(class__.getDeclaredAnnotations())));
  return result.toArray(new ParmParserParm[0]);
 }

 private static ParmParserParm[] getParmParserParms(Field field)
 {
  return getParmParserParms(field.getDeclaredAnnotations());
 }

 private static ParmParserParm[] getParmParserParms(Annotation[] annotations)
 {
  ArrayList<ParmParserParm> result = new ArrayList<ParmParserParm>();
  for (Annotation annotation : annotations)
      {
       if (Debug)Utilities.debug("ParmParserImpl.getParmParserParms note annotation{"
                                +"annotation="     +annotation.toString()                 +"\t"
                                +"annotationclass="+annotation.annotationType().toString()+"}");
       if (annotation instanceof ParmParserParm)
           result.add((ParmParserParm)annotation);
      }
  return result.toArray(new ParmParserParm[0]);
 }

 private static String getSubstitution(String value)
 {
  if (value == null       )return value;
  if (!value.contains("@"))return value;

  for (Pair pair : SubstitutionsAR) // use sorted ArrayList so longer keys are substituted first.
       value = value.replace(pair.getKey(),pair.getValue());

  return value;
 }

 // get next token which may be continued string
 private static String getToken(MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  String result = parms.getToken();
  if (result == null)return null;
  if (result.equals("\\"))throw new MyParseException("ParmParserImpl.getToken unexpected \\{"
                                                    +"where="+parms.whereCurrentToken()+"}");
  if (parms.isSymbol())return result;

  boolean isQuoted = false;
  if (result.startsWith("\""))
     {
      isQuoted = true;
      if (result.length() < 2   )throw new MyParseException("ParmParserImpl.getToken internal error. invalid transparent string.  Too short{"
                                                           +"result="+result                   +"\t"
                                                           +"where=" +parms.whereCurrentToken()+"}");
      if (!result.endsWith("\""))throw new MyParseException("ParmParserImpl.getToken internal error. invalid transparent string. Doesn't end with duoble quote{"
                                                           +"result="+result                   +"\t"
                                                           +"where=" +parms.whereCurrentToken()+"}");
      result = result.substring(1,result.length()-1);
     }

  for (String token=parms.getToken();token!=null;token=parms.getToken())
      {
       if (!(parms.isSymbol() && token.equals("\\")))
          {
           parms.ungetToken();
           break;
          }
       String nextToken=parms.getToken();
       if (nextToken == null)throw new MyParseException("ParmParserImpl.getToken unexpected EOF{"
                                                       +"where="+parms.where()+"}");
       if (parms.isSymbol() )throw new MyParseException("ParmParserImpl.getToken unexpected symbol{"
                                                       +"nextToken="+Utilities.quote(nextToken)+"\t"
                                                       +"where="    +parms.whereCurrentToken() +"}");
       if (nextToken.startsWith("\""))
          {
           isQuoted = true;
           if (nextToken.length() < 2   )throw new MyParseException("ParmParserImpl.getToken internal error. invalid transparent string.  Too short2{"
                                                                +"nextToken="+nextToken                +"\t"
                                                                +"where="    +parms.whereCurrentToken()+"}");
           if (!nextToken.endsWith("\""))throw new MyParseException("ParmParserImpl.getToken internal error. invalid transparent string. Doesn't end with duoble quote2{"
                                                                +"nextToken="+nextToken                +"\t"
                                                                +"where="    +parms.whereCurrentToken()+"}");
           result += nextToken.substring(1,nextToken.length()-1);
          }
       else
           result += nextToken;
      }
  return isQuoted?"\""+result+"\"":result;
 }

 private static String getTokenOrDie(MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  String result = getToken(parms);
  if (result == null)throw new MyParseException("ParmParserImpl.getTokenOrDie unexpected EOF{"
                                               +"where="+parms.where()+"}");
  return result;
 }

 private static String getValueClassName(String genericType)
 throws MyParseException
 {
  int pos = genericType.indexOf("<");
  if (pos == -1)return "String";

  int pos2 = findUnembeddedComma(genericType,pos);
  if (pos2 == -1)throw new MyParseException("ParmParserImpl.getValueClassName no comma found in genericType{" // impossible because generic type is compiler generated.
                                           +"genericType="+genericType+"}");
  return genericType.substring(pos2+1,genericType.length()-1).trim();
 }

 private static boolean implements_(Class<?> class_, Class<?> interface_) 
 {
  if (class_ == null) return false;
  Class<?> [] interfaces = class_.getInterfaces();
  if (interfaces == null) return false;
  for (Class<?> interface__ : interfaces)if (interface__.equals(interface_)) return true;
  return implements_(class_.getSuperclass(),interface_);
 }

 private static boolean isCollection(Object target)
 {
  if (target instanceof Class<?>)return isCollection((Class<?>)target);
  else                           return isCollection(target.getClass());
 }

 private static boolean isCollection(Class<?> target)
 {
  for (Class<?> superClass=target;superClass!=null;superClass=superClass.getSuperclass())if (superClass.equals(AbstractCollection.class))return true;
  return false;
 }

 private static boolean isMap(Object target)
 {
  if (target instanceof Class<?>)return isMap((Class<?>)target);
  else                           return isMap(target.getClass());
 }

 private static boolean isMap(Class<?> target)
 {
  for (Class<?> superClass=target;superClass!=null;superClass=superClass.getSuperclass())if (superClass.equals(AbstractMap.class))return true;
  return false;
 }

 private static void noteSubstitution(String key, String value)
 {
  Substitutions.put(key,value);
  SubstitutionsAR = new ArrayList<Pair>();
  for (Map.Entry<String,String> entry : Substitutions.entrySet())
       SubstitutionsAR.add(new Pair(entry.getKey(), entry.getValue()));
  Collections.sort(SubstitutionsAR);
 }

//private static boolean isElement(String genericType, MyStreamTokenizer parms)
//throws MyIOException,MyParseException
//{
// String token = parms.getToken();
// if (!(parms.isSymbol()) && token.equals("{"))
//    {
//     parms.ungetToken();
//     return true;
//    }
// parms.ungetToken();
// String class_ = getClassName
//}

private static String objectToJsonString(Object object)
{
 return Utilities.toString(object);
}

/**
This method parses an array of strings into the target object.
@param target The object or class into which the parameter fields should be parsed.
@param parms  An array of strings to be parsed.  These are typically the command line arguments.
*/
 public static Object parse(Object target, String[] parms, boolean createFlag)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (isCollection(target))throw new MyParseException("parse Top level collection classes are not supported because there is no generic info");
  if (isMap       (target))throw new MyParseException("parse Top level map classes are not supported because there is no generic info");
  DebugLevel = 0;
  String parm = "";
  if (parms != null)
     {
      boolean dash = true;
      for (String p : parms)
          {
           if (!p.startsWith("-"))dash = false;  // ignore initial parms with leading -;
           if (!dash             )parm+=p+" ";
          }
     }
  //                   Note trailing space on parms will separate them from any stdin
  int inBytes = 0;
  for (long ms = 0;ms<500;ms+=50)
      {
       try {inBytes = System.in.available();}
       catch (IOException e){Utilities.fatalError("MyStreamTokenizer.parse IOException{"
                                                 +"e="+Utilities.toString(e)+"}");}
       if (inBytes > 0)break;
      }
  MyStreamTokenizer mst = inBytes==0?new MyStreamTokenizer(                            new StringReader     (parm     ),null     )
                                    :new MyStreamTokenizer(new MyStreamTokenizer.Parms[]
                                                          {new MyStreamTokenizer.Parms(new StringReader     (parm     ),null    ),
                                                           new MyStreamTokenizer.Parms(new InputStreamReader(System.in),"stdin")});
  mst.setDebug(false);
  mst.setReadBufferSize(ReadBufferSize);
  Substitutions   = new HashMap<String,String>();
  SubstitutionsAR = new ArrayList<Pair>();
  target = parse(target,mst,new HashSet<String>(),createFlag,false);
  if (mst.getToken()!=null)
      throw new MyParseException("ParmParserImpl.parse trailing data not parsed{"
                                +"where="+mst.whereCurrentToken()+"}");
  return target;
 }

/**
@param target The object into which the parameter fields should be parsed.
@param parms  Contains the json-like string to be parsed.  This 
              string typically contains multiple fields.         
*/

// constructedElements mentions variables which have been updated.  I use it to distinguish arrays, collections, and maps which should be created vs
// updated when they are encountered.  e.g. An array may have elements mentioned in several parm files.  The new elements should replace the ones created
// by default, but not each other.   I currently have no way to extend a default list.  It can only be left intact or replaced.  Later, if neeeded, I may
// support an annotation to force appending.  That still wouldn't support optional appending.  I suppose I could use () to indicate the orignal content.  
// Later...

// private static Object parse(Object target, MyStreamTokenizer parms)
// throws IllegalAccessException, MyIOException, MyParseException
// {
//  return parse(target,parms,null);
// }

// private static Object parse(Object target, MyStreamTokenizer parms, HashSet<String> constructedGroups, boolean createFlag)
// throws IllegalAccessException, MyIOException, MyParseException
// {
//  return parse(target,parms,constructedGroups,createFlag,false);
// }

 private static Object parse(Object target, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  return parse(target,parms,null,createFlag,isValue);
 }

 private static Object parse(Object target, String targetGenericType, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  return parse(target,targetGenericType,parms,null,createFlag,isValue);
 }

 private static Object parse(Object target, MyStreamTokenizer parms, HashSet<String> constructedGroups, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  Class<?> targetClass = getClass(target,createFlag);
  return parse(target,targetClass.getTypeName(),parms,constructedGroups,createFlag,isValue);
 }

 private static String makeParmsString(ParmParserParm[] parms)
 {
  StringBuffer result=new StringBuffer();
  for (ParmParserParm parm : parms)
      {
       if (parm          ==null)continue;
       String value = parm.value();
       if (value         ==null)continue;
       if (value.length()==0   )continue;
       String[] strings = value.split("\\t",-1);
       for (String string : strings)
           {
            if (!string.startsWith("\""))string = "\""+string+"\"";
            if (result.length()>0)result.append("\t");
            result.append(string);
           }
      }
  return result.toString();
 }

 private static Object parse(Object target, String targetGenericType, MyStreamTokenizer parms, HashSet<String> initializedGroups, boolean createFlag,
                             boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  return parse(target,targetGenericType,parms,initializedGroups,createFlag,isValue,null);
 }

// parse is called with an object to create or update
// It may be called from doAssignment to process the right hand side.
 private static Object parse(Object target, String targetGenericType, MyStreamTokenizer parms, HashSet<String> initializedGroups, boolean createFlag,
                             boolean isValue,BooleanWrapper created)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debugEnter("parse{"
                                  +"createFlag="       +createFlag               +"\t"
                                  +"isValue="          +isValue                  +"\t"
                                  +"targetGenericType="+targetGenericType        +"\t"
                                  +"whereCurrentToken="+parms.whereCurrentToken()+"}",true);
  Object result=parse_(target,targetGenericType,parms,initializedGroups,createFlag,isValue,created);
  if (Debug)debugReturn("parse return");
  return result;
 }

 private static Object parse_(Object target, String targetGenericType, MyStreamTokenizer parms, HashSet<String> initializedGroups, boolean createFlag, 
                              boolean isValue, BooleanWrapper created)
 throws IllegalAccessException, MyIOException, MyParseException
 {
// target could be class.  An array for example.
// target could be existing object.
// if target is an existing array to be updated, the number of elements must match.

  if (createFlag && !(target instanceof Class<?>))throw new MyParseException("ParmParserImpl.parse Internal error. createFlag is true and target is not a class{"
                                                                          +"where="+parms.whereCurrentToken()+"}");

  // initializedGroups is passed to parseObject because it may call parseFile
  if (initializedGroups == null)initializedGroups = new HashSet<String>();

  Class<?>  targetClass = getClass(target,createFlag);
  ClassType classType   = getClassType(targetClass);
  switch(classType)
        {
         case ARRAY      :return parseArray     (target,targetGenericType,parms,                  createFlag,isValue);
         case COLLECTION :return parseCollection(target,targetGenericType,parms,                  createFlag,isValue); 
         case MAP        :return parseMap       (target,targetGenericType,parms,                  createFlag,isValue); 
         case OBJECT     :return parseObject    (target,targetGenericType,parms,initializedGroups,createFlag,isValue,created);
         case PRIMITIVE  :return parsePrimitive (target,targetGenericType,parms,                  createFlag,isValue);
         default         :throw new MyParseException("ParmParserImpl.parse unknown classType{"
                                                    +"classType="  +classType                +"\t"
                                                    +"targetClass="+targetClass              +"\t"
                                                    +"where="      +parms.whereCurrentToken()+"}");
        }
 }
 
 @SuppressWarnings("unchecked")
 private static Object parseArray(Object target, String fieldGenericType, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  Class<?> targetClass  = getClass(target,createFlag);
  Class<?> elementClass = targetClass.getComponentType();
  if (Debug)debugEnter("parseArray enter{"
                                  +"createFlag="      +createFlag               +"\t"
                                  +"elementClass="    +elementClass             +"\t"
                                  +"fieldGenericType="+fieldGenericType         +"\t"
                                  +"isValue="         +isValue                  +"\t"
                                  +"targetClass="     +targetClass              +"\t"
                                  +"where="           +parms.whereCurrentToken()+"}",true);
  Object   result       = parseArray_(target,elementClass,fieldGenericType,parms,createFlag,isValue);
  if (createFlag)
      result = toArray(elementClass,(ArrayList<Object>)result,parms);
  else      
      copyArrayListToArray((ArrayList<Object>)result,target,parms);
  if (Debug)debugReturn("parseArray return");
  return result;
 }

// Target is an array or collection.
// Input may be either the group or an element of it.

 @SuppressWarnings("unchecked")
 private static ArrayList<?> parseArray_(Object target, Class<?> elementClass, String fieldGenericType, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  // sample targetGenericClass cases
  // int[20]
  // MyClass<Integer>[][][]  

  // sample parms
  // 13
  // [13]
  // [12 13 14]
  // [[13] [12 13]]

  // If array has multiple dimensions, the appropriate nesting in the input must match. 
  // The arrayName=element construction is only for 1 dimension arrays.
  // One can imagine arrays of ArrayLists of arrays of ...

  if (Debug)debugEnter("parseArray_ enter{"
                                  +"fieldGenericType="+fieldGenericType         +"\t"
                                  +"where="           +parms.whereCurrentToken()+"}",true);
  ArrayList<Object> arrayList = new ArrayList<Object>();

  ClassType classType          = getClassType         (elementClass);
  int       classDepth         = findGroupDepth       (parms,fieldGenericType);
  int       inputDepth         = findInputGroupDepth  (parms);
  String    elementGenericType = getElementGenericType(parms,fieldGenericType);
  if (Debug)debug("parseArray_ note variables{"
                                          +"classDepth="        +classDepth               +"\t"
                                          +"classType="         +classType                +"\t"
                                          +"elementClass="      +elementClass             +"\t"
                                          +"elementGenericType="+elementGenericType       +"\t"
                                          +"inputDepth="        +inputDepth               +"\t"
                                          +"isValue="           +isValue                  +"\t"
                                          +"where="             +parms.whereCurrentToken()+"}",true);
  String    token              = parms.getToken();
  if (token == null)
     {
      if (isValue)throw new MyParseException("ParmParserImpl.parseArray unexpected EOF");
      if (Debug)debugReturn("parseArray_ return1{"
                                       +"arrayList="+Utilities.toString(arrayList)+"}");
      return arrayList;
     }

  parms.ungetToken();

  if (isValue)
     {
      // Input is allowed to omit high level braces. We take braces to represent the lowest level possible
      // unless there is no low level data.  If there is no low level data, the braces applied from the top.
      // Thus, an array that defaults to some data at a low level can be made an empty array by array={}.
      // It should work nicely if no one thinks about it...

      if (classDepth > inputDepth && inputDepth != -1) // inputDepth = -1 means there is no data. Just a pair of braces.
         { // array = value rather than array = {value...}
          arrayList.add(parse(elementClass,elementGenericType,parms,true,true));
          if (Debug)debugReturn("parseArray_ return2{"
                               +"arrayList="+Utilities.toString(arrayList)+"}");
          return arrayList;
         }

      token = parms.getToken();
      if (!(parms.isSymbol() && token.equals("{")))throw new MyParseException("parseArray_ expecting open brace{"
                                                                             +"token="+token                    +"\t"
                                                                             +"where="+parms.whereCurrentToken()+"}");
      if (Debug)debug("parseArray_ before parseArray__{"
                                 +"where="           +parms.whereCurrentToken()+"}",true);
      arrayList = parseArray__(parms, elementClass, elementGenericType);
      if (Debug)debugReturn("parseArray_ return3{"
                                       +"arrayList="+Utilities.toString(arrayList)+"}");
      return arrayList;
     }

  // target is left side of = or  highest level item.
  for (token=parms.getToken();token!=null;token=parms.getToken())
      {
       if (parms.isSymbol())
          {
           if (token.equals("{"))  // Must be array or collection since this is highest level.  Elements are not allowed.
              {
               arrayList.addAll(parseArray__(parms,elementClass,elementGenericType));
               continue;
              }
            throw new MyParseException("parseArray_ unexpected symbol{"
                                      +"token="+Utilities.quote(token)   +"\t"
                                      +"where="+parms.whereCurrentToken()+"}");
          }
       String fileName = token; //  e.g. {a,b,c} filename {a,b,c}
       // parseFile will return an AbstractCollection or an array because calls parse which calls parseCollection or parseArray depending...
       addAll(arrayList,parseFile(target,fieldGenericType,fileName,null,createFlag,null,null,parms));
      }
  if (Debug)debugReturn("parseArray_ return4{"
                                          +"arrayList="+Utilities.toString(arrayList)+"}");
  return arrayList;
 }

 /**
  * Parses through array elements.
  * 
  * @author Duane (8/17/2015)
  * 
  * @param parms 
  * @param elementClass 
  * @param genericType 
  * 
  * @return ArrayList<Object> 
  */
 private static ArrayList<Object> parseArray__(MyStreamTokenizer parms, Class<?> elementClass, String genericType)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debugEnter("parseArray__ enter{"
                                  +"genericType="+genericType              +"\t"
                                  +"where="      +parms.whereCurrentToken()+"}",true);
  ArrayList<Object> arrayList = new ArrayList<Object>();
  int separatorCount = 0;

  for (String token=parms.getToken();token!=null;token=parms.getToken()) // use parms.getToken because parse will catenate strings as needed.
      {
       if (parms.isSymbol() && parms.isSeparator(token))separatorCount ++;
       else                                             separatorCount  = 0;
       if (separatorCount == 1)continue;
       if (separatorCount == 2)
          {
           separatorCount = 1;
           arrayList.add(null);
           continue;
          }
       if (parms.isSymbol() && token.equals("}"))
          {
           if (Debug)debugReturn("parseArray__ return{"
                                                   +"arrayList="+Utilities.toString(arrayList)+"}");
           return arrayList;
          }
       parms.ungetToken();
       if (Debug)debug("parseArray__ where before parse{"
                                  +"where="+parms.whereCurrentToken()+"}",true);
       arrayList.add(parse(elementClass,genericType,parms,true,true));
      }
//  This doesn't happen right now because of look ahead on findInputGroupDepth. It will show up again if we optimize that to relax when we don't have to worry
//  about arrays, etc. as map key.
  String msg = "parseArray__ unexpected EOF{"         
               +"arrayList="     +Utilities.toString(arrayList)+"\t"
               +"separatorCount="+separatorCount               +"}";
  if (Debug)debug(msg);
  throw new MyParseException(msg);
 }

@SuppressWarnings("unchecked")
 private static AbstractCollection<?> parseCollection(Object target, String fieldGenericType, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debugEnter("parseCollection{"
                                          +"createFlag="      +createFlag                +"\t"
                                          +"fieldGenericType="+fieldGenericType          +"\t"
                                          +"isValue="         +isValue                   +"\t"
                                          +"target="          +Utilities.toString(target)+"}");
  ArrayList<?> arrayList = parseArray_(target,getCollectionComponentClass(parms,fieldGenericType),fieldGenericType,parms,createFlag,isValue);

  AbstractCollection<Object> result = createFlag?(AbstractCollection<Object>)constructObject((Class<?>)target,null,parms):(AbstractCollection<Object>)target;
  result.clear();
  result.addAll(arrayList);
  if (Debug) debugReturn("parseCollection return{"
                                           +"arrayList="+Utilities.toString(arrayList)+"}");
  return result;
 }

 private static Object parseFile(Object target, String targetGenericType, String fileName, HashSet<String> initializedGroups, boolean createFlag,
                                 BooleanWrapper created, HashMap<String,ArrayList<Object>> arrays, MyStreamTokenizer parms)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debugEnter("parseFile{"
                                          +"fileName="         +fileName                    +"\t"
                                          +"target.class="     +target.getClass().toString()+"\t"
                                          +"targetClass="      +getClass(target,createFlag) +"\t"
                                          +"targetGenericType="+targetGenericType           +"}");
  Utilities.vetInputFile(fileName);
  try{
      MyStreamTokenizer mst    = new MyStreamTokenizer(new FileReader(fileName),fileName);
      mst.noteSubstitutions(parms.getSubstitutions());
      mst.setDebug(false);
      mst.setReadBufferSize(ReadBufferSize);                                                     
      Object            result = parse(target,targetGenericType,mst,initializedGroups,createFlag,false,created);
      if (mst.getToken()!=null)throw new MyParseException("parseFile trailing data not parsed{"
                                                         +"fileName="+fileName               +"\t"
                                                         +"where="   +mst.whereCurrentToken()+"}");
      return result;
     }
  catch(FileNotFoundException e){throw new MyIOException("parseFile file not found{"                   //impossible.  vetInputFile would have picked it up.
                                                        +"exceptionMessage="+e.getMessage()+"\t"
                                                        +"fileName="        +fileName      +"\t"
                                                        +"fileName="+fileName+"}");}
  finally{if (Debug)debugReturn("parseFile return{"
                                                  +"fileName="+fileName+"}");}
 }

 private static Object parseMap(Object target, String targetGenericType, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debugEnter("parseMap Enter{"
                                  +"createFlag="       +createFlag       +"\t"
                                  +"isValue="          +isValue          +"\t"
                                  +"nest="             +Utilities.nest() +"\t"
                                  +"targetGenericType="+targetGenericType+"}",true);
  Object result = parseMap_(target,targetGenericType,parms,createFlag,isValue);
  if (Debug)debugReturn("parseMap Return");
  return result;
 }

 @SuppressWarnings("unchecked")
 private static Object parseMap_(Object target, String targetGenericType, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  String   token      =getDequotedToken(parms);
  Class<?> targetClass=getClass(target,createFlag);
  if (token == null)
     { // nothing to be provided. Instantiate target if needed and return target.
      if (isValue)throw new MyParseException("parseMap unexpected EOF{"
                                            +"where="+parms.where()+"}");
      if (createFlag)target =constructObject(targetClass,null,parms); // should be impossible since we disallow maps as top level target.
      return target;
     }

//  if (parms.isSymbol() && token.equals("("))  Lets not support map constructor parms for now.
//     {
//      String parm      = parms.getTokenOrDie();
//      Object result    = constructObject(targetClass,parm,parms);
//      String nextToken = parms.getTokenOrDie();
//      if (!(parms.isSymbol()&&nextToken.equals(")")))throw new MyParseException("parseMap expected close paren{"
//                                                                               +"where="+parms.where()+"}");
//      return result;
//     }

  if (token.equals("null"))return null;

  if (!(parms.isSymbol() && token.equals("{"))) throw new MyParseException("ParmParserImpl.parseMap Map must start with open brace{"
                                                                         +"token="+token                    +"\t"
                                                                         +"where="+parms.whereCurrentToken()+"}");

  if (createFlag)   // This is always true right now. 
     {
      if (Debug)debug("parseMap_ construct target");
      target  = constructObject((Class<?>)target,null,parms);
     }

  String   keyClassName   = getKeyClassName  (targetGenericType);
  String   valueClassName = getValueClassName(targetGenericType);
  Class<?> keyClass       = getClassForName  (parms,keyClassName  );
  Class<?> valueClass     = getClassForName  (parms,valueClassName);
  if (keyClass   == null)throw new MyParseException("ParmParserImpl.ParseMap key class not found{"
                                                   +"keyClassName="  +keyClassName             +"\t"
                                                   +"valueClassName="+valueClassName           +"\t"
                                                   +"where="         +parms.whereCurrentToken()+"}");
  if (valueClass == null)throw new MyParseException("ParmParserImpl.ParseMap value class not found{"
                                                   +"keyClassName="  +keyClassName             +"\t"
                                                   +"valueClassName="+valueClassName           +"\t"
                                                   +"where="         +parms.whereCurrentToken()+"}");
  for (;;)
      {
       for (;;)
           { // absorb all separators
            token = parms.getTokenOrDie();
            if (parms.isSymbol())
               {
                if (parms.isSeparator(token))continue;  
                if (token.equals("}"))return target;
               }
            parms.ungetToken();
            break;
           }
       if (Debug)debug("parseMap_ note keyClassName{"
                                  +"keyClassName="+keyClassName             +"\t"
                                  +"token="       +token                    +"\t"
                                  +"where="       +parms.whereCurrentToken()+"}");
       Object key   = parse((Object)keyClass  ,keyClassName  ,parms,null,true,true);
       token        = parms.getTokenOrDie();
       if (!(parms.isSymbol() && token.equals("=")))throw new MyParseException("\"ParmParser.parseMap expecting =\"{"
                                                                              +"where="+parms.whereCurrentToken()+"}");
       Object value = parse((Object)valueClass,valueClassName,parms,null,true,true);

       ((Map<Object,Object>)target).put(key,value);
      }
 }

// private static Object parseObject(Object target, String targetGenericType, MyStreamTokenizer parms, boolean createFlag, BooleanWrapper created)
// throws IllegalAccessException, MyIOException, MyParseException
// {
//  return parseObject(target,targetGenericType,parms,new HashSet<String>(),createFlag,false,created);
// }

// suppoort for including file names is only in parseObject.  They are only legal in objects for now.
// cases at entry
// 1. EOF
// 2. open brace
// 3. string
// 4. name=value pairs to EOF.

 // A string not followed by = is considered a file name. 
 // parseValue is called to parse the value part of name=value pairs and constructor calls.  Constructor calls are in parens.

// parseObject may be called several times to parse several files in the same object so arrays and initialized groups must persist.
// is value is used to distinguish calls which occur right after an =.  Initial strings in such calls can not be file names.  It's a bit
// hokey but it seems to work.

 private static Object parseObject(Object target, String targetGenericType, MyStreamTokenizer parms,
                                   HashSet<String> initializedGroups, boolean createFlag, boolean isValue, BooleanWrapper created)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debugEnter("parseObject enter{"
                                  +"created="          +created                  +"\t"
                                  +"createFlag="       +createFlag               +"\t"
                                  +"isValue="          +isValue                  +"\t"
                                  +"targetGenericType="+targetGenericType        +"\t"
                                  +"where="            +parms.whereCurrentToken()+"}");
  Object result = parseObject_(target,targetGenericType,parms,initializedGroups,createFlag,isValue,created);
  if (Debug)debug("parseObject debug1");
  if (result != null &&
      implements_(result.getClass(), Initializable.class)) ((Initializable)result).initialize();

  if (Debug)debugReturn("parseObject return");
  return result;
 }

 private static Object parseObject_(Object target, String targetGenericType, MyStreamTokenizer parms,
                                   HashSet<String> initializedGroups, boolean createFlag, boolean isValue, BooleanWrapper created)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (created == null)created = new BooleanWrapper();
  String   token       = getToken(parms);
  if (Debug)Utilities.debug("ParmParserImpl.parseObject_ note token{"
                           +"token="+token+"}");
  Class<?> targetClass = getClass(target, createFlag);
  HashMap<String,ArrayList<Object>> arrays = new HashMap<String,ArrayList<Object>>();
  if (token == null)
     {
      if (isValue)
         {
          if (Debug)debug("parseObject_ EOF note nest{"
                                                  +"nest="+Utilities.nest()+"}");
          throw new MyParseException("parseObject_ unexpected EOF, expecting value{"
                                            +"where="+parms.where()+"}");
         }
      // nothing to be provided. Instantiate target if needed and return target.
      if (createFlag)
         {
          target =constructObject(targetClass,null,parms);
          created.value=true;
         }
      return target;
     }

  if (parms.isSymbol() && token.equals("("))
     {
      String parm      = getDequotedTokenOrDie(parms);
      Object result    = constructObject(targetClass,parm,parms);
      String nextToken = parms.getTokenOrDie();
      if (!(parms.isSymbol()&&nextToken.equals(")")))throw new MyParseException("parseObject expected close paren{"
                                                           +"where="+parms.whereCurrentToken()+"}");
      return result;
     }

  boolean parseToCloseBrace = parms.isSymbol() && token.equals("{");

  if (parms.isSymbol() && token.equals("="))throw new MyParseException("\"ParmParserImpl.parse unexpected = as first parm\"{"
                                                                      +"where="+parms.whereCurrentToken()+"}");

// actually, it should be impossible for target to be an array here.
//  if (parms.isSymbol() && token.equals("[") && !targetClass.isArray())throw new MyParseException("ParmParserImpl.parse target is not array and token is [{"
//                                                                                                     +"where="+parms.whereCurrentToken()+"}");
//
  if (parms.isSymbol() && !token.equals("{"))throw new MyParseException("ParmParserImpl.parseObject_ unexpected initial symbol{"
                                                                       +"token="+Utilities.quote(token)   +"\t"
                                                                       +"where="+parms.whereCurrentToken()+"}");


  // parms.isSymbol(token) should be a redundant check.

  if (Debug)debug("parseObject_ debug1{"
                             +"isSymbol="         +parms.isSymbol()         +"\t"
                             +"isSymbol(token)="  +parms.isSymbol(token)    +"\t"
                             +"isValue="          +isValue                  +"\t"
                             +"token="            +Utilities.quote(token)   +"\t"
                             +"whereCurrentToken="+parms.whereCurrentToken()+"}");

  if (!(parms.isSymbol() && parms.isSymbol(token))) // The first token is a string. It could be the value from an assignment.
     {
      if (isValue)
         {              // string is value from an assignment to an object.  Process it.
          token = getSubstitution(token);
          if (token.equals("null"))target = null;
          else                     target = constructObject(targetClass,Utilities.dequote(token),parms);
          return target;
         }
      // token is a string.  It may be a continued string, so we don't want to unget it.
     }
  else
     token = Utilities.dequote(parms.getToken());

  for (;token!=null;token=getDequotedToken(parms))
      {
       long tokenId = parms.getCurrentTokenId();
       if (Debug)debug("parseObject_ note token{"
                                               +"token="+Utilities.quote(token)+"}");
       if (parms.isSymbol())
           {
            if (parms.isSeparator(token))continue;  
            if (token.equals("}"))
               {
                if (!parseToCloseBrace)throw new MyParseException("parseObject unexpected close brace{"
                                                                 +"where="+parms.whereCurrentToken()+"}");
                parseToCloseBrace = false;
                if (createFlag && !created.value)
                   {
                    target  = constructObject((Class<?>)target,null,parms);
                    created.value = true;
                   }
                break;
               }
            throw new MyParseException("ParseObject unexpected symbol{"
                                      +"token=\""+token+"\""               +"\t"
                                      +"where="  +parms.whereCurrentToken()+"}");
           }

       // token is a string.  If the nextToken is eof or not = then token is a file name

       token            = Utilities.dequote(token);
       String nextToken = Utilities.dequote(parms.getToken());    // Note that parms.getToken is called, not getToken(parms).  
                                                                  // This avoids having to backtrack over a continued string.

       if (nextToken == null)
          {
           postProcessArrays(target,initializedGroups,arrays,parms);
           target = parseFile(target,targetGenericType,token,initializedGroups,createFlag&&!created.value,created,arrays,parms);
           break;
          }

       if (Debug)debug("parseObject_ note token nextToken{"
                                               +"nextToken="+nextToken+"\t"
                                               +"token="    +token    +"}");
//       if (!parms.isSymbol() ||
//            parms.isSymbol() && nextToken.equals("}"))
       if (!parms.isSymbol() || nextToken.equals("}")) // over simplify logic so coverage report is happy.
          {                  
           parms.ungetToken(); // This is the token after the file name. Put it back.
           postProcessArrays(target,initializedGroups,arrays,parms);
           target = parseFile(target,targetGenericType,token,initializedGroups,createFlag&&!created.value,created,arrays,parms);
           continue;
          }

       if (nextToken.equals("=")) // We already know it is a symbol if we got this far.
          {
           if (token.startsWith("@")) // It's a substitution
              {
               String substitution = getTokenOrDie(parms);
               noteSubstitution(token, substitution);
               if (Debug)debug("parseObject_ noteSubstitution{"
                                          +"substitution="+substitution+"\t"
                                          +"token="       +token       +"}");
               continue;
              }
           // We are assigning a field in target so it is fair to ensure the target exists.
           if (createFlag && !created.value)
              {
               if (Debug)debug("parse construct target");
               target        = constructObject((Class<?>)target,null,parms);
               created.value = true;
              }

           String fieldName  = token;
           doAssignment(target,fieldName,initializedGroups,arrays,parms,tokenId);
           continue;
          }

       parms.ungetToken();  // put nextToken back so all possibilities can be handled in one place.
      }

  if (parseToCloseBrace)
      throw new MyParseException("ParmParserImpl.parse unexpected end of file");

  postProcessArrays(target,initializedGroups,arrays,parms);
  return target;
 }

 private static Object parsePrimitive(Object target, String targetGenericType, MyStreamTokenizer parms, boolean createFlag, boolean isValue)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debug("parsePrimitive");
  Class<?> targetClass = getClass(target,createFlag);
  if (Debug)debug("parsePrimitive");
  String token = Utilities.dequote(parms.getToken());
  if (token == null)
     {
      if (isValue)throw new MyParseException("parsePrimitive unexpected EOF{"
                                            +"where="+parms.where()+"}");
      if (createFlag)target=0;  // (must always be create.  We can't update a primitive.
      return target;
     }
  if (token.equals("{") ||
      token.equals("[") ||
      token.equals("("))
      throw new MyParseException("ParmParserImpl.parsePrimitive exception target field is primitive and source is object{"
                                +"where="+parms.whereCurrentToken()+"}"); 
  return createPrimitive(targetClass,token,parms);
 }

// private static Object parseValue(Object target, String targetGenericType, MyStreamTokenizer parms, boolean createFlag)
// {
//  String token = getToken(parms);
//  if (parms.isSymbol() && token.equals("("))
//     {
//      String parm = getToken(parms);
//      if (parm  == null     )throw new MyParseException("ParmParserImpl.parse unexpected EOF after ( "    +parms.where());
//      token = parms.getToken();
//      if (token == null     )throw new MyParseException("ParmParserImpl.parse unexpected EOF expecting ) "+parms.where());
//      if (!token.equals(")"))throw new MyParseException("ParmParserImpl.parse expected ) "                +parms.where());
//      Class<?> targetClass = createFlag?(Class<?>)target:target.getClass();  // Actually, createFlag must be true
//      return constructObject(targetClass,parm,parms); // oops. What if (...) is followed by {?
//     }
// }

 private static void postProcessArrays(Object target, HashSet<String> initializedGroups,HashMap<String,ArrayList<Object>> arrays, MyStreamTokenizer parms)
 throws IllegalAccessException, MyIOException, MyParseException
 {
  if (Debug)debug("postProcessArrays");

  // target should already exist.

  Class<?> targetClass = target.getClass();

  for (Map.Entry<String,ArrayList<Object>> pair : arrays.entrySet())
      {
       String            fieldName = pair.getKey  ();
       ArrayList<Object> arrayList = pair.getValue();
       if (fieldName == null)continue;  // This should be impossible.

       Field field = getField(targetClass,fieldName,parms);
       boolean accessible = field.isAccessible();
       field.setAccessible(true);
       Class<?> fieldClass = field.getType();
       if (!fieldClass.isArray())throw new MyParseException("ParmParserImpl.postProcessArrays Internal Error. Field is not array.{" // internal error
                                                           +"field="   +fieldName    +"\t"
                                                           +"location="+parms.whereCurrentToken()+"}");
       String fieldClassName = fieldClass.getCanonicalName();
       if (fieldClassName == null)throw new MyParseException("ParmParserImpl.parse Internal Error. fieldClassName is null "+parms.whereCurrentToken());
       if (!fieldClassName.endsWith("[]"))throw new MyParseException("ParmParserImpl.parse Internal Error. fieldClassName is not array{"
                                                                  +"fieldClassName="+fieldClassName+"\t"
                                                                  +"location={"     +parms.whereCurrentToken()+"}");
       String   elementClassName = fieldClassName.substring(0,fieldClassName.length()-2);
       Class<?> elementClass     = null;
       elementClass              = fieldClass.getComponentType();
//       catch (ClassNotFoundException e){throw new MyParseException("ParmParserImpl.parse Internal Error.  ClassNotFoundException{"
//                                                                  +"elementClassName="         +elementClassName              +"\t"
//                                                                  +"Exception="                +e.toString()                  +"\t"
//                                                                  +"fieldClass.DeclaringClass="+fieldClass.getDeclaringClass()+"\t"
//                                                                  +"fieldClass.EnclosingClass="+fieldClass.getEnclosingClass()+"\t"
//                                                                  +"fieldClass.SimpleName="    +fieldClass.getSimpleName()    +"\t"
//                                                                  +"fieldClassName="           +fieldClassName                +"\t"
//                                                                  +"where="                    +parms.whereCurrentToken()     +"}");} 
//
       Object existingArray = field.get(target);
       if (initializedGroups.contains(fieldName))
           copyArrayToArrayListStart(existingArray,arrayList);

       initializedGroups.add(fieldName);

       Object targetArray = Array.newInstance(elementClass,arrayList.size());
       copyArrayListToArray(arrayList,targetArray,parms);
       field.set(target,targetArray);
       field.setAccessible(accessible);
      }
  arrays.clear();
 }

 public static void setReadBufferSize(int readBufferSize){ReadBufferSize=readBufferSize;}

 private static Object toArray(Class<?> elementClass,ArrayList<Object> arrayList, MyStreamTokenizer parms)
 throws MyIOException, MyParseException
 {
  int size = arrayList==null?0:arrayList.size();
  Object array = Array.newInstance(elementClass,size);
  return copyArrayListToArray(arrayList,array,parms);
 }

 private static boolean splitAppendString(StringBuffer result,String value,boolean anyFieldFound)
 {
  boolean thisFieldFound = false;
  String[] strings = value.split("\\t",-1);
  for (String string : strings)
      {
       if (anyFieldFound)result.append("\t");
       thisFieldFound = true;
       anyFieldFound |= thisFieldFound;
       if (string.startsWith("\""))result.append(string);
       else                        result.append("\"").append(string).append("\"");
      }
  return thisFieldFound;
 }

 private static Object tryConstructor(Constructor<?> constructor,String parm, MyStreamTokenizer parms)
 throws MyIOException,MyParseException,
        IllegalAccessException   ,
        InstantiationException   ,
        InvocationTargetException,
        NoSuchMethodException 
 {
  if (Debug)debug("tryConstructor enter{"
                             +"constructor="+constructor     +"\t"
                             +"nest="       +Utilities.nest()+"\t"
                             +"parm="       +parm            +"}");
  boolean accessible = constructor.isAccessible();
  constructor.setAccessible(true);
  try {
       if (parm == null)return constructor.newInstance(new Object[]{}    );
       else             return constructor.newInstance(new Object[]{parm});
      }
  catch (Exception e)
        {
         if (Debug)try {debug("tryConstructor exception{"
                                         +"constructor="+constructor                    +"\t"
                                         +"parm="       +parm                           +"\t"
                                         +"Message="    +Utilities.quote(e.getMessage())+"}");}
                               catch (MyParseException e2){debug("tryConstructor exception exception{"
                                                                +"constructor="+constructor    +"\t"
                                                                +"parm="       +parm           +"\t"
                                                                +"Message="    +e.getMessage() +"\t"
                                                                +"Message2="   +e2.getMessage()+"}");}
        }
  finally{constructor.setAccessible(accessible);}
  return null;
 }

 private static String usageText = "\"Parameters are name=value pairs. They can be provided on the command line or in input files or from stdin.\"\t"
                                  +"\"Values can be expanded with braces to provide more name=value pairs or just a list of values as needed.\"\t"
                                  +"Strings can be quoted with double quotes using \\ for transparency for backslash, tab, nl, cr, and double quote (\\\\,\\t,\\n,\\r,\\\").\t"
                                  +"Strings may be continued by placing a standalone backslash(\\) between segments. e.g. to use multiple lines.\t"
                                  +"\"/* ... */ outside of double quotes comments comments the text they surround.\"\t"
                                  +"\"// outside of double quotes comments the remainder of the line.\"\t"
                                  +"\"Any top level string that is not part of a name=value pair or list element is considered a file name.\"\t" 
                                  +"\"That file is used to provide more name=value pairs.\"\t"
                                  +"A rudimentary substitution macro facility is provided.\t"
                                  +"Names with an @ prefix can be assigned values which can be used later by using @Name as a value.\t"
                                  +"Thus files can be used for bulky parameters with a few substitutions specified on the command line.\t\t";

 private static ArrayList<String> UsageNest = new ArrayList<String>();

 private static String getUsageNest()
 {
  StringBuffer result = new StringBuffer();
  for (String field : UsageNest)
      {
       if (result.length() > 0)
          result.append("={");
       result.append(field);
      }
  for (int i=0;i<UsageNest.size()-1;i++)result.append("}");
  return result.toString();
 }

 public static String usage(Class<?> class_)
 {
  String result = usageText+"\"Compile Time:"+CompileDate.getDate()+"\"\t\t"+usageClass(class_);
  if (Debug)Utilities.debug("ParmParserImpl.usage note result{result="+result+"}");
//  try {return StructuredMessageFormatter.format(result);}
  try {return new StructuredMessageFormatter2().format(result);}
  catch (MyIOException    e){Utilities.fatalError("ParmParserImpl.usage exception formatting message{"
                                                 +"e="+Utilities.toString(e)+"}");}
  catch (MyParseException e){Utilities.fatalError("ParmParserImpl.usage exception formatting message{"
                                                 +"e="+Utilities.toString(e)+"}");}
  return null;
 }

 public static String usageClass(Class<?> class_)
 { // Just use for highest level class
  if (Debug)Utilities.debug("ParmParserImpl.usageClass{"
                           +"class_="+class_.toString()+"}");
  if (class_ == null)return "";

  StringBuffer result = new StringBuffer();
  ParmParserParm[] parmParserParms = getParmParserParms(class_);
  String parmsString=makeParmsString(parmParserParms);
  result.append(parmsString);

  if (Debug)Utilities.debug("ParmParserImpl.usageClass note result before fieldDoc{"
                           +"result="+result.toString()+"}");
  String fieldUsage = usageFields(class_);

  if (fieldUsage.length()>0)
     {
      if (result.length()>0)result.append("\t");
      result.append(fieldUsage);                      
     }
  return result.toString();
 }

 public static String usageFields(Class<?> class_)
 {
  if (Debug)Utilities.debug("ParmParserImpl.usageFields{"
                           +"class_="+class_.toString()+"}");
  if (class_ == null)return "";
  boolean anyFieldFound = false;
  StringBuffer result = new StringBuffer();
  for (Class<?> class__=class_;class__!=null;class__=class__.getSuperclass())
      {
       Field[] fields = class__.getDeclaredFields();
       for (Field field: fields)
           {
            if (Debug)Utilities.debug("ParmParserImpl.usageFields field{"
                                     +"class__="+class__         +"\t"
                                     +"field=" +field.getName()+"}");
            UsageNest.add(field.getName());
            ParmParserParm[] fieldParms = getParmParserParms(field);
            if (fieldParms.length > 0)
                {
                 String fieldParmsString = makeParmsString(fieldParms);
                 String classUsage       = "";
                 String fieldUsage       = fieldParmsString;
                 String fieldClassUsage  = "";
                 Class<?> fieldClass = field.getType();
                 if (fieldClass.isArray())
                    {
                     classUsage ="May be repeated or grouped in braces";
                     fieldClass=fieldClass.getComponentType();
                    }

                 String prev = UsageClassCache.get(fieldClass);
                 if (prev != null)
                    {
                     if (classUsage.length()>0)classUsage += "\t";
                     classUsage += "\"see " + prev + "\"";
                    }
                 else
                     {
                      ParmParserParm[] classParms = getParmParserParms(fieldClass);
                      String classParmsString     = makeParmsString(classParms);
                      if (classParmsString.length() > 0)
                         {
                          if (classUsage.length()>0)classUsage+="\t";
                          classUsage += classParmsString;
                         }
                      fieldClassUsage             = usageFields(fieldClass);
                      if (classParmsString.length()>0 ||
                          fieldClassUsage .length()>0)
                         {
                          UsageClassCache.put(fieldClass, getUsageNest());
                          if (Debug)Utilities.debug("ParmParserImpl.usageFields note usage nest{"
                                                   +"fieldClass_="    +class_.toString()            +"\t"
                                                   +"UsageNest=" +Utilities.toString(UsageNest)+"}");
                         }
                     }
                 
                 if (fieldUsage.length()>0 &&
                     classUsage.length()>0)
                     fieldUsage += "\t"+classUsage;
                 else
                     if (classUsage.length()>0)
                         fieldUsage = classUsage;
                 if (fieldUsage.length() > 0)
                    {
                     if (result.length() > 0)result.append("\t");
                     result.append(field.getName()).append("=").append(fieldUsage);
                    }

                 if (Debug && field.getName().equals("By")) Utilities.debug("ParmParserImpl.usageFields note By annotation{"
                                                                          +"fieldUsage="+fieldUsage+"}");
                 if (fieldClassUsage.length() > 0)
                    {
                     if (result.length()>0)result.append("\t");
                     result.append(field.getName()).append("={").append(fieldClassUsage).append("}");
                    }
                 if (fieldUsage     .length()==0 && 
                     fieldClassUsage.length()==0)
                    {
                     if (result.length()>0)result.append("\t");
                     result.append(field.getName()).append("=\t");
                    }
                }
            UsageNest.remove(UsageNest.size()-1);
           }
      }
  return result.toString();
 }

 private static int DebugLevel=0;
 private static void debug(String msg)
 throws MyIOException,MyParseException
 {
  debug(msg,false);
 }

 private static void debug(String msg,boolean expand)
 throws MyIOException,MyParseException
 {
             Utilities.debug(Utilities.toString(DebugLevel)+Utilities.spaces(DebugLevel)+msg);
  if (expand)Utilities.debug(Utilities.toString(DebugLevel)+StructuredMessageFormatter.format(msg,DebugLevel));
 }

 private static void  debugEnter(String msg)
 throws MyIOException,MyParseException
 {
  debugEnter(msg,false);
 }

 private static void  debugEnter(String msg,boolean expand)
 throws MyIOException,MyParseException
 {
  debug(msg,expand);
  DebugLevel++;
 }

 private static void  debugReturn(String msg)
 throws MyIOException,MyParseException
 {
  debugReturn(msg,false);
 }

 private static void  debugReturn(String msg,boolean expand)
 throws MyIOException,MyParseException
 {
  DebugLevel--;
  debug(msg,expand);
 }
}

