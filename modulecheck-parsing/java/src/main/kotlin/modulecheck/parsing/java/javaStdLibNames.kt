/*
 * Copyright (C) 2021-2023 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package modulecheck.parsing.java

import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName.Companion.asPackageName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.SimpleName.Companion.stripPackageNameFromFqName

internal fun String.javaLangFqNameOrNull(): QualifiedDeclaredName? {

  val maybeJavaLang = "java.lang.$this"

  return if (maybeJavaLang in javaStdLibNames) {

    val javaLangPackage = "java.lang".asPackageName()
    val simple = maybeJavaLang.stripPackageNameFromFqName(javaLangPackage)

    DeclaredName.java(javaLangPackage, simple)
  } else {
    null
  }
}

internal val javaStdLibNames = setOf(

  "java.lang.Appendable",
  "java.lang.AutoCloseable",
  "java.lang.CharSequence",
  "java.lang.Cloneable",
  "java.lang.Comparable",
  "java.lang.Iterable",
  "java.lang.Readable",
  "java.lang.Runnable",
  "java.lang.Thread.UncaughtExceptionHandler",
  "java.lang.Boolean",
  "java.lang.Byte",
  "java.lang.Character",
  "java.lang.Character.Subset",
  "java.lang.Character.UnicodeBlock",
  "java.lang.Class",
  "java.lang.ClassLoader",
  "java.lang.ClassValue",
  "java.lang.Compiler",
  "java.lang.Double",
  "java.lang.Enum",
  "java.lang.Float",
  "java.lang.InheritableThreadLocal",
  "java.lang.Integer",
  "java.lang.Long",
  "java.lang.Math",
  "java.lang.Number",
  "java.lang.Object",
  "java.lang.Package",
  "java.lang.Process",
  "java.lang.ProcessBuilder",
  "java.lang.ProcessBuilder.Redirect",
  "java.lang.Runtime",
  "java.lang.RuntimePermission",
  "java.lang.SecurityManager",
  "java.lang.Short",
  "java.lang.StackTraceElement",
  "java.lang.StrictMath",
  "java.lang.String",
  "java.lang.StringBuffer",
  "java.lang.StringBuilder",
  "java.lang.System",
  "java.lang.Thread",
  "java.lang.ThreadGroup",
  "java.lang.ThreadLocal",
  "java.lang.Throwable",
  "java.lang.Void",
  "java.lang.ArithmeticException",
  "java.lang.ArrayIndexOutOfBoundsException",
  "java.lang.ArrayStoreException",
  "java.lang.ClassCastException",
  "java.lang.ClassNotFoundException",
  "java.lang.CloneNotSupportedException",
  "java.lang.EnumConstantNotPresentException",
  "java.lang.Exception",
  "java.lang.IllegalAccessException",
  "java.lang.IllegalArgumentException",
  "java.lang.IllegalMonitorStateException",
  "java.lang.IllegalStateException",
  "java.lang.IllegalThreadStateException",
  "java.lang.IndexOutOfBoundsException",
  "java.lang.InstantiationException",
  "java.lang.InterruptedException",
  "java.lang.NegativeArraySizeException",
  "java.lang.NoSuchFieldException",
  "java.lang.NoSuchMethodException",
  "java.lang.NullPointerException",
  "java.lang.NumberFormatException",
  "java.lang.ReflectiveOperationException",
  "java.lang.RuntimeException",
  "java.lang.SecurityException",
  "java.lang.StringIndexOutOfBoundsException",
  "java.lang.TypeNotPresentException",
  "java.lang.UnsupportedOperationException",
  "java.lang.AbstractMethodError",
  "java.lang.AssertionError",
  "java.lang.BootstrapMethodError",
  "java.lang.ClassCircularityError",
  "java.lang.ClassFormatError",
  "java.lang.Error",
  "java.lang.ExceptionInInitializerError",
  "java.lang.IllegalAccessError",
  "java.lang.IncompatibleClassChangeError",
  "java.lang.InstantiationError",
  "java.lang.InternalError",
  "java.lang.LinkageError",
  "java.lang.NoClassDefFoundError",
  "java.lang.NoSuchFieldError",
  "java.lang.NoSuchMethodError",
  "java.lang.OutOfMemoryError",
  "java.lang.StackOverflowError",
  "java.lang.ThreadDeath",
  "java.lang.UnknownError",
  "java.lang.UnsatisfiedLinkError",
  "java.lang.UnsupportedClassVersionError",
  "java.lang.VerifyError",
  "java.lang.VirtualMachineError",
  "java.lang.Deprecated",
  "java.lang.Override",
  "java.lang.SafeVarargs",
  "java.lang.SuppressWarnings"
)
