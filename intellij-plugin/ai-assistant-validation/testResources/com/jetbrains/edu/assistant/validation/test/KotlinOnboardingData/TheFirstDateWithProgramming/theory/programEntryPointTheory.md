Every program written in any programming language has an **entry point**.
As a rule, it is a special section in the program that controls all
subsequent operations.

In Kotlin, the [entry point](https://kotlinlang.org/docs/basic-syntax.html#program-entry-point) is the special `main` function, which looks like this:
```kotlin
fun main() {
    // Some code here
}
```

Everything that happens _within_ the function (between the curly braces)
will be executed while the program runs.
This function can be placed in _any_ file in your project;
you can even add _several_ `main` functions to one project.
In the latter case, you can choose which entry point to run.

To `run` a program, you should click on the **green triangle** next to the `main` function,
and then the result of the program will be displayed in the _console_ inside the IDE:

![Program entry point and console](../../utils/src/main/resources/images/part1/first.date/entry_point.png "Program entry point and console")