package jetbrains.kotlin.course.last.push

val ball = """                                                       
  ○
""".trimIndent()

val rhombus = """
    X
   / \
   \ /
    X
""".trimIndent()

val vase = """  
  ____   
 /    \
/ /  \ \
\ \__/ /
 \____/
""".trimIndent()

val robot = """     
+---+---+
| o   o |
|   ^   |
|  ---  |
+---+---+
""".trimIndent()

val cube = """     
   .+------+
 .' |    .'|
+---+--+'  |
|   |  |   |
|  ,+--+---+
|.'    | .' 
+------+'
""".trimIndent()

val allPatternsMap = mapOf(
  "ball" to ball,
  "rhombus" to rhombus,
  "vase" to vase,
  "robot" to robot,
  "cube" to cube,
)