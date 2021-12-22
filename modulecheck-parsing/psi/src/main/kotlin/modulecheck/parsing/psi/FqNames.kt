/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.parsing.psi

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.Binds
import dagger.Provides
import dagger.multibindings.Multibinds
import org.jetbrains.kotlin.name.FqName
import javax.inject.Inject
import kotlin.reflect.KClass

object FqNames {
  val inject = Inject::class.fqName

  val binds = Binds::class.fqName
  val module = dagger.Module::class.fqName
  val provides = Provides::class.fqName
  val multibinds = Multibinds::class.fqName

  val contributesTo = ContributesTo::class.fqName
  val contributesBinding = ContributesBinding::class.fqName
  val contributesMultibinding = ContributesMultibinding::class.fqName

  // This one gets a hard-coded string so that we don't need to opt-in to the experimental API.
  val contributesSubcomponent = FqName("com.squareup.anvil.annotations.ContributesSubcomponent")

  val mergeComponent = MergeComponent::class.fqName
  val mergeSubcomponent = MergeSubcomponent::class.fqName
}

private val KClass<*>.fqName get() = FqName(java.canonicalName)
