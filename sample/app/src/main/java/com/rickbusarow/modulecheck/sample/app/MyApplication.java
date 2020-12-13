package com.rickbusarow.modulecheck.sample.app;

import com.rickbusarow.modulecheck.ClassB;
import com.rickbusarow.modulecheck.sample.app.internal.MyInternalClass;
import com.rickbusarow.modulecheck.sample.libraryc.KotlinOuterClass;

public class MyApplication {

    //    ClassA classA;
    ClassB classB;
    // static   ClassA classA;
    static ClassB classBstatic;
    KotlinOuterClass kotlinOuterClass;

    MyInternalClass myInternalClass;

    static void someStaticVoidMethod() {
    }

    static class StaticInnerClass {
        static class StaticInnerInnerClass {
        }
    }

}
