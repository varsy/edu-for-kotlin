### 1. The `random` function

How can you generate a word?
For example, you can specify a possible alphabet
(a list of characters that can be used in the word)
and **randomly** select the characters the desired number of times.

To do that, you can use a special function, [`random`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/random.html),
which works with a _list_ (or with a _string_, which, as we have already found out,
can be represented as a list):

```kotlin
// Return any single symbol from the string "ABCD"
"ABCD".random()
```

### 2. How to create a new list with random elements

To get a _list of random elements_, you need to create a list with the desired number
of elements, specifying in a _lambda expression_ (the condition)
how each element will be generated:
```kotlin
// Create a list with 5 elements, each of them is 6
List(5) { 6 }
// Create a list with 5 elements, each of them is 'A'
List(5) { 'A' }
// Create a list with 5 elements, each of them is a random symbol from the string "ABCD"
List(5) { "ABCD".random() }
```