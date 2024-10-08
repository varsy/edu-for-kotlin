When defining a type, you can specify that it could also have a special `null` value.
It's a `null` reference, which doesn't refer to anything.
We will delve into more details about this in the following parts of the course;
for now, it is enough to know some basic things.

To indicate that a type might be `null`, you should add `?` to the type, for example:
```kotlin
// 'a' can be String or null
var a: String? = null
```

If a value can be `null`, then the various built-in functions that we talked about earlier
cannot automatically work with such a value, for example:
```kotlin
var a: String? = null
a.length // INCORRECT!!

var a: String = "text"
a.length // CORRECT
```