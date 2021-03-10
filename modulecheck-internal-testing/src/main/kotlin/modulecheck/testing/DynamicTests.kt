package modulecheck.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest

interface DynamicTests {

  fun <T : Any> Iterable<() -> T>.dynamic(
    testName: String,
    test: suspend CoroutineScope.(T) -> Unit
  ): List<DynamicTest> {
    return map { factory -> factory.invoke() }
      .map { subject ->

        DynamicTest.dynamicTest("$testName -- ${subject::class.simpleName}") {
          runBlocking {
            test.invoke(this, subject)
          }
        }
      }
  }

  fun <T : Any> Iterable<() -> T>.dynamic(
    testName: (T) -> String,
    test: suspend CoroutineScope.(T) -> Unit
  ): List<DynamicTest> {
    return map { factory -> factory.invoke() }
      .map { subject ->

        DynamicTest.dynamicTest("$testName -- $subject") {
          runBlocking {
            test.invoke(this, subject)
          }
        }
      }
  }
}
