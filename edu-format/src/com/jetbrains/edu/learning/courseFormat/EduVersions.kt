@file:JvmName("EduVersions")

package com.jetbrains.edu.learning.courseFormat

// If you change version of any format, add point about it in `documentation/Versions.md`
const val JSON_FORMAT_VERSION: Int = 17

/**
 * Since that version we deprecated [com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE] and started to use
 * new [com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_ID] and
 * [com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_VERSION] properties
 *
 * @see [com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin.programmingLanguage]
 * @see [com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin.languageId]
 * @see [com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin.languageVersion]
 */
const val JSON_FORMAT_VERSION_WITH_NEW_LANGUAGE_VERSION: Int = 16