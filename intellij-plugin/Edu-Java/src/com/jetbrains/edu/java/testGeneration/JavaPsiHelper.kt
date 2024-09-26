package com.jetbrains.edu.java.testGeneration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiClassWrapper
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiMethodWrapper
import com.jetbrains.edu.coursecreator.testGeneration.psi.TestLanguage
import com.jetbrains.edu.coursecreator.testGeneration.util.SettingsArguments

class JavaPsiHelper : PsiHelper() {
    override var psiFile: PsiFile? = null

    override val language: TestLanguage get() = TestLanguage.Java

    private val log = Logger.getInstance(this::class.java)

    override fun generateMethodDescriptor(
        psiMethod: PsiMethodWrapper,
    ): String {
        val methodDescriptor = psiMethod.methodDescriptor
        log.info("Method description: $methodDescriptor")
        return methodDescriptor
    }

    override fun getSurroundingClass(
        caretOffset: Int,
    ): PsiClassWrapper? {
        val classElements = PsiTreeUtil.findChildrenOfAnyType(psiFile, PsiClass::class.java)
        for (cls in classElements) {
            if (cls.containsOffset(caretOffset)) {
                val javaClassWrapper = JavaPsiClassWrapper(cls)
                if (javaClassWrapper.isTestableClass()) {
                    log.info("Surrounding class for caret in $caretOffset is ${javaClassWrapper.qualifiedName}")
                    return javaClassWrapper
                }
            }
        }
        log.info("No surrounding class for caret in $caretOffset")
        return null
    }

    override fun getSurroundingMethod(
        caretOffset: Int,
    ): PsiMethodWrapper? {
        val methodElements = PsiTreeUtil.findChildrenOfAnyType(psiFile, PsiMethod::class.java)
        for (method in methodElements) {
            if (method.body != null && method.containsOffset(caretOffset)) {
                val surroundingClass =
                    PsiTreeUtil.getParentOfType(method, PsiClass::class.java) ?: continue
                val surroundingClassWrapper = JavaPsiClassWrapper(surroundingClass)
                if (surroundingClassWrapper.isTestableClass()) {
                    val javaMethod = JavaPsiMethodWrapper(method)
                    log.info("Surrounding method for caret in $caretOffset is ${javaMethod.methodDescriptor}")
                    return javaMethod
                }
            }
        }
        log.info("No surrounding method for caret in $caretOffset")
        return null
    }

    override fun getSurroundingLine(
        caretOffset: Int,
    ): Int? {
        val doc = PsiDocumentManager.getInstance(psiFile!!.project).getDocument(psiFile!!) ?: return null

        val selectedLine = doc.getLineNumber(caretOffset)
        val selectedLineText =
            doc.getText(TextRange(doc.getLineStartOffset(selectedLine), doc.getLineEndOffset(selectedLine)))

        if (selectedLineText.isBlank()) {
            log.info("Line $selectedLine at caret $caretOffset is blank")
            return null
        }
        log.info("Surrounding line at caret $caretOffset is $selectedLine")

        // increase by one is necessary due to different start of numbering
        return selectedLine + 1
    }

    override fun collectClassesToTest(
        project: Project,
        classesToTest: MutableList<PsiClassWrapper>,
        caretOffset: Int,
    ) {
        // check if cut has any none java super class
        val maxPolymorphismDepth = 5 //TODO

        val cutPsiClass = getSurroundingClass(caretOffset)!!
        var currentPsiClass = cutPsiClass
        for (index in 0 until maxPolymorphismDepth) {
            if (!classesToTest.contains(currentPsiClass)) {
                classesToTest.add(currentPsiClass)
            }

            if (currentPsiClass.superClass == null ||
                currentPsiClass.superClass!!.qualifiedName.startsWith("java.")
            ) {
                break
            }
            currentPsiClass = currentPsiClass.superClass!!
        }
        log.info("There are ${classesToTest.size} classes to test")
    }

    override fun getInterestingPsiClassesWithQualifiedNames(
        project: Project,
        classesToTest: List<PsiClassWrapper>,
        polyDepthReducing: Int,
    ): MutableSet<PsiClassWrapper> {
        val interestingPsiClasses: MutableSet<JavaPsiClassWrapper> = mutableSetOf()

        var currentLevelClasses =
            mutableListOf<PsiClassWrapper>().apply { addAll(classesToTest) }

        repeat(SettingsArguments(project).maxInputParamsDepth(polyDepthReducing)) {
            val tempListOfClasses = mutableSetOf<JavaPsiClassWrapper>()

            currentLevelClasses.forEach { classIt ->
                classIt.methods.forEach { methodIt ->
                    (methodIt as JavaPsiMethodWrapper).parameterList.parameters.forEach { paramIt ->
                        PsiTypesUtil.getPsiClass(paramIt.type)?.let { typeIt ->
                            JavaPsiClassWrapper(typeIt).let {
                                if (!it.qualifiedName.startsWith("java.")) {
                                    interestingPsiClasses.add(it)
                                }
                            }
                        }
                    }
                }
            }
            currentLevelClasses = mutableListOf<PsiClassWrapper>().apply { addAll(tempListOfClasses) }
            interestingPsiClasses.addAll(tempListOfClasses)
        }
        log.info("There are ${interestingPsiClasses.size} interesting psi classes")
        return interestingPsiClasses.toMutableSet()
    }

    override fun getInterestingPsiClassesWithQualifiedNames(
        cut: PsiClassWrapper,
        psiMethod: PsiMethodWrapper,
    ): MutableSet<PsiClassWrapper> {
        val interestingPsiClasses = cut.getInterestingPsiClassesWithQualifiedNames(psiMethod)
        log.info("There are ${interestingPsiClasses.size} interesting psi classes from method ${psiMethod.methodDescriptor}")
        return interestingPsiClasses
    }

    override fun getPackageName(): String {
        val psiPackage = JavaDirectoryService.getInstance().getPackage(psiFile!!.containingDirectory) // TODO
        return psiPackage?.qualifiedName ?: ""
    }

    private fun PsiElement.containsOffset(caretOffset: Int): Boolean {
        return (textRange.startOffset <= caretOffset) && (textRange.endOffset >= caretOffset)
    }

}
