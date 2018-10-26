package com.duane.utilities;

public class MyPair<K,V>
{
 private K Key  ;
 private V Value;
 public K getKey  (){return Key;  }
 public V getValue(){return Value;}
 public MyPair(K key,V value){Key=key;Value=value;}
}
