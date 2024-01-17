/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.parsing.kotlin.compiler.impl

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.protobuf.ByteString
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil

fun ModuleDescriptor.declarations(): Sequence<DeclarationDescriptor> {
  return getPackageFragments()
    .flatMap { DescriptorUtils.getAllDescriptors(it.getMemberScope()).asSequence() }
    .filterNot { it.isInBuiltInModule() }
}

fun ModuleDescriptor.getPackageFragments(): Sequence<PackageFragmentDescriptor> {
  return KotlinJavascriptSerializationUtil
    .getPackagesFqNames(this)
    .flatMap { getPackage(it).fragments }
    .asSequence()
}

internal fun DeclarationDescriptor.isInBuiltInModule(): Boolean {
  return module == module.builtIns.builtInsModule
}

internal fun DeserializedDescriptor.protoByteString(): ByteString {
  return protoByteStringOrNull()
    ?: error("not sure how to find a proto byte string from ${this::class}")
}

internal fun DeserializedDescriptor.protoByteStringOrNull(): ByteString? {
  return when (this) {
    is DeserializedMemberDescriptor -> proto.toByteString()
    is DeserializedClassDescriptor -> classProto.toByteString()
    else -> null
  }
}
