package jetbrains.kotlin.course.almost.done

fun trimPicture(picture: String) = picture.trimIndent()

fun applyBordersFilter(picture: String): String {
  val pictureRows = picture.lines()
  val pictureWidth = getPictureWidth(picture)
  val horizontalBorder = "$borderSymbol".repeat(pictureWidth + 4)

  val sb = StringBuilder()
  sb.append("$horizontalBorder$newLineSymbol")
  for (row in pictureRows) {
    sb.append("$borderSymbol$separator$row")
    if (row.length < pictureWidth) {
      sb.append("$separator".repeat(pictureWidth - row.length))
    }
    sb.append("$separator$borderSymbol$newLineSymbol")
  }
  sb.append("$horizontalBorder$newLineSymbol")
  return sb.toString()
}

fun applySquaredFilter(picture: String): String {
  val bordered = applyBordersFilter(picture.trimIndent())
  val pictureRows = bordered.lines()

  val sbTop = StringBuilder()
  val sbBottom = StringBuilder()
  for (index in pictureRows.indices) {
    val newRow = pictureRows[index].repeat(2)
    when (index) {
      0 -> sbTop.append("$newRow$newLineSymbol")
      pictureRows.indices.last -> sbBottom.append(newRow)
      else -> {
        sbTop.append("$newRow$newLineSymbol")
        sbBottom.append("$newRow$newLineSymbol")
      }
    }
  }
  return "$sbTop$sbBottom"
}

fun applyFilter(picture: String, filter: String): String {
  val trimmedPicture = trimPicture(picture)
  return when(filter) {
    "borders" -> applyBordersFilter(trimmedPicture)
    "squared" -> applySquaredFilter(trimmedPicture)
    else -> error("Unexpected filter")
  }
}

fun safeReadLine(): String = readlnOrNull() ?: error("Your input is incorrect, sorry")

fun chooseFilter(): String {
  var toContinue = true
  var filter = ""
  println("Please choose the filter: 'borders' or 'squared'.")
  do {
    when (val input = safeReadLine()) {
      "borders", "squared" -> {
        toContinue = false
        filter = input
      }
      else -> println("Please input 'borders' or 'squared'")
    }
  } while (toContinue)
  return filter
}

fun choosePicture(): String {
  do {
    println("Please choose a picture. The possible options are: ${allPictures().joinToString(", ")}")
    val name = safeReadLine()
    val picture = getPictureByName(name)
    picture?.let {
      return@choosePicture picture
    }
  } while (true)
}

fun getPicture(): String {
  println("Do you want to use a predefined picture or a custom one? " +
          "Please input 'yes' for a predefined image or 'no' for a custom one")
  do {
    when (safeReadLine()) {
      "yes" -> {
        return choosePicture()
      }
      "no" -> {
        println("Please input a custom picture")
        return safeReadLine()
      }
      else -> println("Please input 'yes' or 'no'")
    }
  } while (true)
}

fun main() {
  // Uncomment this code on the last step of the game

  // photoshop()
}