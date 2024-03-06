We need to enable the user to transform their own pictures. Let's do it!

### Task

Implement the `getPicture` function, which asks the user to choose a predefined picture or to input a custom picture.

<div class="hint" title="Click me to see the signature of the getPicture function">

The signature of the function is:
```kotlin
fun getPicture(): String
```
</div>

This function should work as follows:

First of all, ask this question:
```text
Do you want to use a predefined picture or a custom one? Please input 'yes' for a predefined image or 'no' for a custom one
```

Next, read the user's answer via the `safeReadLine` function and process the output:

(1) If the user wants to choose a predefined picture, run the `choosePicture` function.

(2) If the user wants to upload a custom picture, prompt them with: `Please input a custom picture` (note that only single-line images are supported).

(3) If the user typee an incorrect command, prompt them with: `Please input 'yes' or 'no'`.

**Note**: to avoid typos, just copy the text from here and paste it into your code.

The `getPicture` function should work as follows:

![`getPicture` function work](../../utils/src/main/resources/images/part1/almost.done/get_picture.gif "`getPicture` function work")

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to get a code style hint">

To check the user's answer in the `getPicture` function,
it is most convenient to use the `when` expression instead a composite `if`.
</div>