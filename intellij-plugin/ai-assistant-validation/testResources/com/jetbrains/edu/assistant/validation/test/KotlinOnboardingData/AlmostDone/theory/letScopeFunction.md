If you need to perform several actions when working with a `nullable` value,
you can use the safe call operator (`?.`) together with the [`let`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/let.html) scope function from the standard library:
```kotlin
fun foo(x: String?): String {
    x?.let {
        println("x is not null!")
        return x
    }
    return ""
}
```
or
```kotlin
fun foo(x: String?): String {
    x?.let {
        println("x is not null!")
        return it
    }
    return ""
}
```

This code is the same as:
```kotlin
fun foo(x: String?): String {
    if (x != null) {
        println("x is not null!")
        return x
    }
    return ""
}
```