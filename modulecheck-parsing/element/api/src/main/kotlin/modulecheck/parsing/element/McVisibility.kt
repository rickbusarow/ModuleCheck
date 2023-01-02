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

package modulecheck.parsing.element

import kotlinx.serialization.Serializable
import modulecheck.parsing.element.McVisibility.McJavaVisibility
import modulecheck.parsing.element.McVisibility.McKtVisibility

interface HasVisibility {
  val visibility: McVisibility
}

interface HasJavaVisibility : HasVisibility {
  override val visibility: McJavaVisibility
}

interface HasKtVisibility : HasVisibility {
  override val visibility: McKtVisibility
}

@Serializable
sealed interface McVisibility {

  @Serializable sealed interface McJavaVisibility : McVisibility {
    @Serializable  object PackagePrivate : McJavaVisibility
  }

  @Serializable
  sealed interface McKtVisibility : McVisibility {
    @Serializable
    object Internal : McKtVisibility
  }

  @Serializable object Public : McJavaVisibility, McKtVisibility
  @Serializable object Protected : McJavaVisibility, McKtVisibility
  @Serializable object Private : McJavaVisibility, McKtVisibility
}
