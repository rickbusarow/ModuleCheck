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

package modulecheck.parsing.source.element

import modulecheck.parsing.source.element.McVisibility.McJavaVisibility
import modulecheck.parsing.source.element.McVisibility.McKtVisibility

interface HasVisibility {
  val visibility: McVisibility
}

interface HasJavaVisibility : HasVisibility {
  override val visibility: McJavaVisibility
}

interface HasKtVisibility : HasVisibility {
  override val visibility: McKtVisibility
}

sealed interface McVisibility {
  sealed interface McJavaVisibility : McVisibility {
    object PackagePrivate : McJavaVisibility
  }

  sealed interface McKtVisibility : McVisibility {

    object Internal : McKtVisibility
  }

  object Public : McJavaVisibility, McKtVisibility
  object Protected : McJavaVisibility, McKtVisibility
  object Private : McJavaVisibility, McKtVisibility
}
