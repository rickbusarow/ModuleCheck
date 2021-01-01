@file:Suppress("TooManyFunctions")

/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck.files

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isPublic

fun KtModifierListOwner.isPublicNotOverridden() = isPublic && !isOverride()
fun KtModifierListOwner.isPrivateOrInternal() = isPrivate() || isInternal()
fun KtModifierListOwner.isAbstract() = hasModifier(KtTokens.ABSTRACT_KEYWORD)
fun KtModifierListOwner.isOverride() = hasModifier(KtTokens.OVERRIDE_KEYWORD)
fun KtModifierListOwner.isOpen() = hasModifier(KtTokens.OPEN_KEYWORD)
fun KtModifierListOwner.isExternal() = hasModifier(KtTokens.EXTERNAL_KEYWORD)
fun KtModifierListOwner.isOperator() = hasModifier(KtTokens.OPERATOR_KEYWORD)
fun KtModifierListOwner.isConstant() = hasModifier(KtTokens.CONST_KEYWORD)
fun KtModifierListOwner.isInternal() = hasModifier(KtTokens.INTERNAL_KEYWORD)
fun KtModifierListOwner.isLateinit() = hasModifier(KtTokens.LATEINIT_KEYWORD)
fun KtModifierListOwner.isInline() = hasModifier(KtTokens.INLINE_KEYWORD)
fun KtModifierListOwner.isExpect() = hasModifier(KtTokens.EXPECT_KEYWORD)
