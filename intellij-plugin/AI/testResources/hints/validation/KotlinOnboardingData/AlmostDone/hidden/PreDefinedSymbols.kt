@file:Suppress("MayBeConst")

package jetbrains.kotlin.course.almost.done

val borderSymbol = '#'
val separator = ' '
val newLineSymbol = System.lineSeparator()

fun getPictureWidth(picture: String) = picture.lines().maxOfOrNull { it.length } ?: 0

fun getPictureByName(name: String) = allImages[name]

fun allPictures() = allImages.keys