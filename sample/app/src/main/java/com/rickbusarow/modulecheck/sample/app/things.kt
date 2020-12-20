package com.rickbusarow.modulecheck.sample.app

import com.rickbusarow.modulecheck.ClassB
import com.rickbusarow.modulecheck.sample.app.internal.MyInternalClass
import com.rickbusarow.modulecheck.sample.libraryc.KotlinOuterClass

class MyApplication2 {
  //    ClassA classA;
  var classB: ClassB? = null
  var kotlinOuterClass: KotlinOuterClass? = null
  var myInternalClass: MyInternalClass? = null
}
