package modulecheck.api.context

import modulecheck.api.ConfigurationName
import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class AnvilScopeReferences(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<ConfiguredProjectDependency>>
) : ConcurrentMap<ConfigurationName, Set<ConfiguredProjectDependency>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeReferences>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilScopeReferences> {
    override operator fun invoke(project: Project2): AnvilScopeReferences {
      val map = project
        .configurations
        .mapValues { (configurationName, _) ->

          val projectDependencies = project
            .projectDependencies
            .value[configurationName]
            .orEmpty()

          project[JvmFiles][configurationName.toSourceSetName()]
            .orEmpty()
            .flatMap { jvmFile ->

              jvmFile
                .maybeExtraReferences
                .mapNotNull { possible ->
                  projectDependencies
                    .firstOrNull {
                      it.project[Declarations][SourceSetName.MAIN].orEmpty()
                        .any { it == possible }
                    }
                }
            }
            .toSet()
        }

      return AnvilScopeReferences(ConcurrentHashMap(map))
    }
  }
}
