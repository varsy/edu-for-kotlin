In this task, we will implement a function that allows the user to choose which
of the predefined pictures they want to modify.

### Task

Implement the `choosePicture` function that allows choosing a predefined picture by its name.

<div class="hint" title="Click me to see the signature of the choosePicture function">

The signature of the function is:
```kotlin
fun choosePicture(): String
```
</div>

You should prompt the user to choose a picture by its name:
`Please choose a picture. The possible options are: <picture names>`.
You need to prompt the user until they input one of the correct options.

You can get the list of predefined pictures by calling the pre-defined function `allPictures`:

```kotlin
println(allPictures()) // [spongeBob, simba, brianGriffin, cat, pig, fox, monkey, elephant, android, apple]
```

To get a picture by its name, you can use the predefined function `getPictureByName`,
which returns either `String?` – an image – or `null` if the name input is incorrect:

```kotlin
println(getPictureByName("brianGriffin")) // returns a picture, since "brianGriffin" is part of the allPictures() result
println(getPictureByName("myPicture")) // returns NULL, since "myPicture" is NOT included in the allPictures() result
```

**Note**: to avoid typos, just copy the text from here and paste it into your code.

The `choosePicture` function should work as follows:

![`choosePicture` function work](../../utils/src/main/resources/images/part1/almost.done/choose_picture.gif "`choosePicture` function work")
