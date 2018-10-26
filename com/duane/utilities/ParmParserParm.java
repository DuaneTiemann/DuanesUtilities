package com.duane.utilities;
import  java.lang.annotation.Repeatable;
import  java.lang.annotation.Retention;
import  java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ParmParserParm
{
 String value() default "";
}
