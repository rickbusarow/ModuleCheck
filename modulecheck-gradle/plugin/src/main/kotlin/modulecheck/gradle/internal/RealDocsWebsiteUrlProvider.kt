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

package modulecheck.gradle.internal

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.DocsWebsiteUrlProvider
import modulecheck.dagger.ModuleCheckVersionProvider
import modulecheck.dagger.SourceWebsiteUrlProvider
import modulecheck.dagger.TaskScope
import javax.inject.Inject

@ContributesBinding(TaskScope::class)
class RealDocsWebsiteUrlProvider @Inject constructor() : DocsWebsiteUrlProvider {
  override fun get(): String = BuildProperties().docsWebsite
}

@ContributesBinding(TaskScope::class)
class RealSourceWebsiteUrlProvider @Inject constructor() : SourceWebsiteUrlProvider {
  override fun get(): String = BuildProperties().sourceWebsite
}

@ContributesBinding(TaskScope::class)
class RealModuleCheckVersionProvider @Inject constructor() : ModuleCheckVersionProvider {
  override fun get(): String = BuildProperties().version
}
