This lesson focuses on the topics that you covered in the previous lesson.
The main difference is that the final project will not be divided into intermediate stages
and you can try to implement it from scratch by yourself.
We have no doubt that you will succeed!

----

<p align="center">
    <img src="../../utils/src/main/resources/images/part1/last.push/game.png" alt="Patterns generator" width="400"/>
</p>

### Project description

The project of this lesson is **Patterns generator**.
The purpose of this project is to create an application
for automatically generating character images of a given size and pattern.

Firstly, you need to ask the user:
```text
Do you want to use a predefined pattern or a custom one?
Please input 'yes' for a predefined pattern or 'no' for a custom one.
```

You need to handle the user's answer and ask the question again if the answer is incorrect.
If the user wants to use a custom pattern, you just need to ask them to input it.
If the user wants to use a predefined pattern,
you need to ask them to choose one of the predefined patterns.
You can get a list with all of them by calling the already defined `allPatterns` function.

Secondly, you need to ask the user to choose the generator: `canvas` or `canvasGaps`.
And finally, ask the user to input the `width` and `height` of the generated picture.

### Project example

![The patterns generator example](../../utils/src/main/resources/images/part1/last.push/app.gif "The patterns generator example")