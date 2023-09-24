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

package modulecheck.builds

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Compares the contents of two zip files, ignoring metadata like timestamps. */
fun File.zipContentEquals(other: File): Boolean {

  require(extension == "zip") { "This file is not a zip file: file://$path" }
  require(other.extension == "zip") { "This file is not a zip file: file://$other" }

  fun ZipFile.getZipEntries(): Set<ZipEntry> {
    return entries()
      .asSequence()
      .filter { !it.isDirectory }
      .toHashSet()
  }

  return ZipFile(this).use { zip1 ->
    ZipFile(other).use use2@{ zip2 ->

      val zip1Entries = zip1.getZipEntries()
      val zip1Names = zip1Entries.mapTo(mutableSetOf()) { it.name }
      val zip2Entries = zip2.getZipEntries()
      val zip2Names = zip2Entries.mapTo(mutableSetOf()) { it.name }

      // Check if any file is contained in one archive but not the other
      if (zip1Names != zip2Names) {
        return@use false
      }

      // Check if the contents of any files with the same path are different
      for (file in zip1Names) {
        val zip1Entry = zip1.getEntry(file)
        val zip2Entry = zip2.getEntry(file)

        if (zip1Entry.size != zip2Entry.size) {
          return@use false
        }

        val inputStream1 = zip1.getInputStream(zip1Entry)
        val inputStream2 = zip2.getInputStream(zip2Entry)
        val content1 = inputStream1.readBytes()
        val content2 = inputStream2.readBytes()
        inputStream1.close()
        inputStream2.close()

        if (!content1.contentEquals(content2)) {
          return@use false
        }
      }
      return@use true
    }
  }
}
