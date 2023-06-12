package com.jetbrains.edu.go.courseGeneration

import com.goide.GoLanguage
import com.goide.sdk.GoSdk
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse

class GoCourseBuilderTest : CourseGenerationTestBase<GoProjectSettings>() {
  override val defaultSettings: GoProjectSettings = GoProjectSettings(GoSdk.NULL)

  fun `test new educator course`() {
    val course = newCourse(GoLanguage.INSTANCE)
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("main") {
          file("main.go", """
            package main
            
            import (
            	"fmt"
            	task "task1"
            )
            
            func main() {
            	fmt.Println(task.Sum(2, 3))
            }

            """)
        }
        dir("test") {
          file("task_test.go", """
            package test
            
            import (
            	task "task1"
            	"testing"
            )
            
            //todo: replace this with an actual test
            func TestSum(t *testing.T) {
            	type args struct {
            		a int
            		b int
            	}
            	tests := []struct {
            		name string
            		args args
            		want int
            	}{
            		{"1", args{1, 1}, 2},
            		{"2", args{1, 2}, 3},
            	}
            	for _, tt := range tests {
            		t.Run(tt.name, func(t *testing.T) {
            			if got := task.Sum(tt.args.a, tt.args.b); got != tt.want {
            				t.Errorf("Sum() = %v, want %v", got, tt.want)
            			}
            		})
            	}
            }

            """)
        }
        file("task.go")
        file("task.md")
        file("go.mod")
      }
    }.assertEquals(rootDir)
  }

  fun `test study course structure`() {
    val course = course(language = GoLanguage.INSTANCE) {
      lesson {
        eduTask {
          taskFile("main/main.go")
          taskFile("test/task_test.go")
          taskFile("task.go")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("main") {
          file("main.go")
        }
        dir("test") {
          file("task_test.go")
        }
        file("task.go")
        file("task.md")
      }
    }.assertEquals(rootDir)
  }
}
