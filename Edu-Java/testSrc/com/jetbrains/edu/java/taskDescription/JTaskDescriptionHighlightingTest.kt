package com.jetbrains.edu.java.taskDescription

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase

class JTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = JavaLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()

  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    ```

    Code block with specific language:
    ```java
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    ```

    Inline code `if (condition) {} else {}`
  """, """
    <html>
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <span class="code-block"><pre>  <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre> </span>
      <p>Code block with specific language:</p>
      <span class="code-block"><pre>  <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre> </span>
      <p>Inline code <span class="code"><span style="...">if </span>(condition) {} <span style="...">else </span>{}</span></p>
     </body>
    </html>
  """)

  fun `test html description highlighting`() = doHtmlTest("""
    <html>
    <p>Code block with default language:</p>
    <pre><code>
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-java">
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    </code></pre>
    <p>Inline code <code>if (condition) {} else {}</code></p>
    </html>
  """, """
    <html>
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <span class="code-block"><pre>
      <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre> </span>
      <p>Code block with specific language:</p>
      <span class="code-block"><pre>
      <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre> </span>
      <p>Inline code <span class="code"><span style="...">if </span>(condition) {} <span style="...">else </span>{}</span></p>
     </body>
    </html>
  """)

  fun `test html description no highlighting class`() = doHtmlTest("""
    <html>
    <pre><code class="no-highlight">
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    </code></pre>
    </html>
  """, """
    <html>
     <head></head>
     <body>
      <span class="code-block"><pre>
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    </pre> </span>
     </body>
    </html>
  """)
}
