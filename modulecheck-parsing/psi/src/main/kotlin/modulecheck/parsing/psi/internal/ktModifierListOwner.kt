/*
 * Copyright (C) 2021-2022 Rick Busarow
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
@file:Suppress("TooManyFunctions")

package modulecheck.parsing.psi.internal

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isPublic

internal fun KtModifierListOwner.isPublicNotOverridden() = isPublic && !isOverride()
internal fun KtModifierListOwner.isPrivateOrInternal() = isPrivate() || isInternal()
internal fun KtModifierListOwner.isAbstract() = hasModifier(KtTokens.ABSTRACT_KEYWORD)
internal fun KtModifierListOwner.isOverride() = hasModifier(KtTokens.OVERRIDE_KEYWORD)
internal fun KtModifierListOwner.isOpen() = hasModifier(KtTokens.OPEN_KEYWORD)
internal fun KtModifierListOwner.isExternal() = hasModifier(KtTokens.EXTERNAL_KEYWORD)
internal fun KtModifierListOwner.isOperator() = hasModifier(KtTokens.OPERATOR_KEYWORD)
internal fun KtModifierListOwner.isConstant() = hasModifier(KtTokens.CONST_KEYWORD)
internal fun KtModifierListOwner.isInternal() = hasModifier(KtTokens.INTERNAL_KEYWORD)
internal fun KtModifierListOwner.isLateinit() = hasModifier(KtTokens.LATEINIT_KEYWORD)
internal fun KtModifierListOwner.isInline() = hasModifier(KtTokens.INLINE_KEYWORD)
internal fun KtModifierListOwner.isExpect() = hasModifier(KtTokens.EXPECT_KEYWORD)
