In programming, the concept of a **variable** is essential.
A variable is a kind of box that stores some value.
Each variable has a _name_, a _type_, and a _value_.

To declare a variable in Kotlin,
you need to use the [`val`](https://kotlinlang.org/docs/basic-syntax.html#variables) keyword,
specify its _name_ and _type_, and after the `=` sign,
specify the _value_ of this variable.

For example, to create a `firstAnswer` variable and assign it an empty [`String`](https://kotlinlang.org/docs/basic-types.html#strings),
you should write the following:
```kotlin
val firstAnswer: String = ""
```

It is **important** to note that the `val` variable _cannot be changed_.
This means that if you put a value in it, then no other value can be assigned to it:

```kotlin
val firstAnswer: String = ""
firstAnswer = "new non-empty string" // ERROR!!
```

In Kotlin, the variable type can often be _skipped_ if it can be inferred from the context:
```kotlin
val firstAnswer = ""
```

According to [the Kotlin naming convention](https://kotlinlang.org/docs/coding-conventions.html#function-names),
variable names should start with a lowercase letter and use [camel case](https://en.wikipedia.org/wiki/Camel_case) and no underscores.
For example, `firstAnswer` is a correct name, and `first_answer` or `FirstAnswer` are incorrect.