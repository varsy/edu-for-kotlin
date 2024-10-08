To correctly work with such _nullable_ values, Kotlin provides a mechanism for [null safety](https://kotlinlang.org/docs/null-safety.html).
In simpler terms, in Kotlin, it is necessary to handle `null` values separately for the program to work correctly.
The simplest processing mechanisms are the [`!!`](https://kotlinlang.org/docs/null-safety.html#the-operator) and [`?:`](https://kotlinlang.org/docs/null-safety.html#elvis-operator) (the Elvis operator) operators.

### 1. The `!!` operator

The `!!` operator simply disregards the `null` value and works with the type as ithoughf there
could be no `null` value. However, if the program encounters the `null` value,
it will exit with an error ([`Null pointer exception`](https://kotlinlang.org/docs/null-safety.html#nullable-types-and-non-null-types), or `NPE`).
```kotlin
var a: String? = null
a!!.length // CORRECT, but will throw NPE
```

### 2. The `Elvis` operator and the smart casts mechanism

The `Elvis` operator checks if the value is `null` and handles the `null` value separately.
```kotlin
var a: String? = null
a?.length ?: error("Find null value") // CORRECT, the case with null will be handled separately
```

The latter example with the Elvis operator is the same as the following code:
```kotlin
var a: String? = null
if (a != null) {
    a.length // We can use just length here (without ?) thanks to the smart casts mechanism
} else {
    error("Find null value")
}
```

In this example, we noted the [smart casts mechanism](https://kotlinlang.org/docs/typecasts.html#smart-casts).
It is a special mechanism in Kotlin that can identify certain cases when a nullable value is always non-null.