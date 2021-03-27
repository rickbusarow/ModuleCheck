package modulecheck.psi

import hermit.test.junit.HermitJUnit5
import hermit.test.resets
import modulecheck.psi.internal.asKtFile
import modulecheck.psi.internal.getByNameOrIndex
import modulecheck.testing.tempFile
import org.jetbrains.kotlin.psi.annotationEntryRecursiveVisitor
import org.junit.jupiter.api.Test

internal class AnnotationVisitorTest : HermitJUnit5() {

  val testFile by tempFile()
  val testFileKt by resets { testFile.asKtFile() }

  @Test
  fun `foo`() {
    val block = """
      import app.my.AppScope
      import com.squareup.anvil.annotations.ContributesTo

      @ContributesTo(AppScope::class)
      interface MyComponent
    """.trimIndent()

    testFile.writeText(block)

    val visitor = annotationEntryRecursiveVisitor {

      val scope = it.valueArgumentList?.getByNameOrIndex(0, "scope")?.text

      println(
        """
          scope = $scope
          value arguments  -> ${it.valueArgumentList?.arguments?.map { it.text }}
        ${it.typeReference?.text}
        ${it.children.map { it.text }}
      """.trimIndent()
      )

      println("annotation entry --> ${it.text}")
    }

    testFileKt.accept(visitor)
  }
}
