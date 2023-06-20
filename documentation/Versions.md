## Format Versions Changelog

### XML format version

9.  Drop `AnswerPlaceholderSubtaskInfo` and move all info into `AnswerPlaceholder` object

	Previous format of `AnswerPlaceholder`:
	```xml
	<AnswerPlaceholder>
	  <option name="index" value="0" />
	  <option name="initialState">
	    <MyInitialState>
	      <option name="length" value="6" />
	      <option name="offset" value="22" />
	    </MyInitialState>
	  </option>
	  <option name="length" value="6" />
	  <option name="offset" value="22" />
	  <option name="selected" value="false" />
	  <option name="subtaskInfos">
	    <map>
	      <entry key="0">
	        <value>
	          <AnswerPlaceholderSubtaskInfo>
	            <option name="answer" value="" />
	            <option name="hasFrame" value="true" />
	            <option name="hints">
	              <list>
	                <option value="" />
	              </list>
	            </option>
	            <option name="needInsertText" value="false" />
	            <option name="placeholderText" value="TODO()" />
	            <option name="possibleAnswer" value="&quot;OK&quot;" />
	            <option name="selected" value="false" />
	            <option name="status" value="Unchecked" />
	          </AnswerPlaceholderSubtaskInfo>
	        </value>
	      </entry>
	    </map>
	  </option>
	  <option name="useLength" value="true" />
	</AnswerPlaceholder>
	```

	Current format of `AnswerPlaceholder`:
	```xml
	<AnswerPlaceholder>
	  <option name="index" value="0" />
	  <option name="initialState">
	    <MyInitialState>
	      <option name="length" value="6" />
	      <option name="offset" value="22" />
	    </MyInitialState>
	  </option>
	  <option name="length" value="6" />
	  <option name="offset" value="22" />
	  <option name="selected" value="false" />
	  <option name="useLength" value="true" />
	  <option name="placeholderText" value="TODO()" />
	  <option name="possibleAnswer" value="&quot;OK&quot;" />
	  <option name="status" value="TODO()" />
	  <option name="hints">
	    <list>
	      <option value="" />
	    </list>
	  </option>
	</AnswerPlaceholder>
	```
	
	Replace `taskTexts` map by `descriptionText` field because without subtasks
	`taskTexts` map always contains only one entry.
	Add `descriptionFormat` field which describes description text format. Can be `HTML` and `MD` for html and markdown syntax accordingly.
	
	Before:
	```xml
    <EduTask>
      <!-- other fields -->
      <option name="taskTexts">
        <map>
          <entry key="task" value="&lt;html&gt;&#10;Write your task text here.&#10;&lt;br&gt;&#10;&lt;/html&gt;"/>
        </map>
      </option>
    </EduTask>
    ```
    
    After:
    ```xml
    <EduTask>
      <!-- other fields -->
      <option name="descriptionText" value="&lt;html&gt;&#10;Write your task text here.&#10;&lt;br&gt;&#10;&lt;/html&gt;"/>
      <option name="descriptionFormat" value="HTML" />
    </EduTask>
    ```
    
10. Convert additional files map to have `visible` property for each additional file
    Before:
    ```xml
    <EduTask>
      <!-- other fields -->
      <option name="additionalFiles">
        <map>
          <entry key="additional_file.txt" value="some text" />
        </map>
      </option>
    </EduTask>
    ```
    
    After:
    ```xml
    <EduTask>
      <!-- other fields -->
      <option name="additionalFiles">
        <map>
          <entry key="additional_file.txt">
            <value>
              <AdditionalFile>
                <option name="text" value="some text" />
                <option name="visible" value="true" />
              </AdditionalFile>
            </value>
          </entry>
        </map>
      </option>
    </EduTask>
    ```

11. Make all paths relative to task folder

    Before:
    ```xml
    <EduTask>
      <option name="testsText">
        <map>
          <entry key="Tests.kt" value="some code" />
        </map>
      </option>
      <option name="taskFiles">
        <map>
          <entry key="Task.kt">
            <value>
              <TaskFile>
                <option name="name" value="Task.kt" />
                <option name="answerPlaceholders">
                  <list>
                    <AnswerPlaceholder>
                      <option name="placeholderDependency">
                        <AnswerPlaceholderDependency>
                          <option name="fileName" value="Task.kt" />
                          <!-- other fields -->
                        </AnswerPlaceholderDependency>
                      </option>
                      <!-- other fields -->
                    </AnswerPlaceholder>
                  </list>
                </option>
                <!-- other fields -->
              </TaskFile>
            </value>
          </entry>
        </map>
      </option>
      <!-- other fields -->
    </EduTask>
    ```

    After:
    ```xml
    <EduTask>
      <option name="testsText">
        <map>
          <entry key="test/Tests.kt" value="some code" />
        </map>
      </option>
      <option name="taskFiles">
        <map>
          <entry key="src/Task.kt">
            <value>
              <TaskFile>
                <option name="name" value="src/Task.kt" />
                <option name="answerPlaceholders">
                  <list>
                    <AnswerPlaceholder>
                      <option name="placeholderDependency">
                        <AnswerPlaceholderDependency>
                          <option name="fileName" value="src/Task.kt" />
                          <!-- other fields -->
                        </AnswerPlaceholderDependency>
                      </option>
                      <!-- other fields -->
                    </AnswerPlaceholder>
                  </list>
                </option>            
                <!-- other fields -->
              </TaskFile>
            </value>
          </entry>
        </map>
      </option>
      <!-- other fields -->
    </EduTask>
    ```

12. Extract Coursera course, Unify local and remote courses

    Before:
    ```xml
    <RemoteCourse>
      <option name="courseType" value="Coursera" />
      <!-- other fields -->
    </RemoteCourse>
    ```

    After:
    ```xml
    <CourseraCourse>
      <option name="courseType" value="Coursera" />
      <!-- other fields -->
    </CourseraCourse>

    ```
    and

    Before:
    ```xml
    <Course>
      <!-- other fields -->
    </Course>
    ```

    After:
    ```xml
    <EduCourse>
      <!-- other fields -->
    </EduCourse>

    and

    Before:
    ```xml
    <RemoteCourse>
      <!-- other fields -->
    </RemoteCourse>
    ```

    After:
    ```xml
    <EduCourse>
      <!-- other fields -->
    </EduCourse>

13. Merge task, test and additional files into one map

    Before:
    ```xml
    <EduTask>
      <!-- other fields -->
      <option name="additionalFiles">
        <map>
          <entry key="additional_file.txt">
            <value>
              <AdditionalFile>
                <option name="text" value="some additional text" />
                <option name="visible" value="true" />
              </AdditionalFile>
            </value>
          </entry>
        </map>
      </option>
      <option name="testsText">
        <map>
          <entry key="Tests.kt" value="some test text" />
        </map>
      </option>
      <option name="taskFiles">
        <map>
          <entry key="Task.kt">
            <value>
              <TaskFile>
                <!-- other fields -->
                <option name="answerPlaceholders">
                  <list />
                </option>
                <option name="text" value="some task text" />
                <option name="visible" value="true" />
                <option name="name" value="Task.kt" />
              </TaskFile>
            </value>
          </entry>
        </map>
      </option>
    </EduTask>
    ```
    
    After:
    ```xml
    <EduTask>
      <!-- other fields -->
      <option name="files">
        <map>
          <entry key="additional_file.txt">
            <value>
              <TaskFile>
                <option name="text" value="some additional text" />
                <option name="visible" value="true" />
                <option name="name" value="additional_file.txt" />
              </TaskFile>
            </value>
          </entry>
          <entry key="Task.kt">
            <value>
              <TaskFile>
                <!-- other fields -->
                <option name="text" value="some task text" />
                <option name="visible" value="true" />
                <option name="name" value="Task.kt" />
              </TaskFile>
            </value>
          </entry>
          <entry key="Tests.kt">
            <value>
              <TaskFile>
                <option name="text" value="some test text" />
                <option name="visible" value="false" />
                <option name="name" value="Tests.kt" />
              </TaskFile>
            </value>
          </entry>
        </map>
      </option>
    </EduTask>
    ```

14. Rename stepId to id in Task

    Before:
    ```xml
    <EduTask>
      <!-- other fields -->
      <option name="stepId" value="662903" />
    </EduTask>
    ```

    After:
    ```xml
    <EduTask>
      <!-- other fields -->
      <option name="id" value="662903" />
    </EduTask>
    ```
15. - Renamed choiceVariants to choiceOptions to match Stepik naming and added status (Correct/Incorrect/Unknown) to format

     Before:
      ```xml
      <ChoiceTask>
        <option name="choiceVariants">
          <list>
            <option value="1995"/>
            <option value="2004"/>
            <option value="1987"/>
          </list>
        </option>
      </ChoiceTask>
      ```
    
     After:
     ```xml
     <ChoiceTask>
       <option name="choiceOptions">
         <list>
           <ChoiceOption>
             <option name="text" value="1995" />
             <option name="status" value="INCORRECT" />
           </ChoiceOption>
           <ChoiceOption>
             <option name="text" value="2004" />
             <option name="status" value="CORRECT" />
           </ChoiceOption>
           <ChoiceOption>
             <option name="text" value="1987" />
             <option name="status" value="UNKNOWN" />
           </ChoiceOption>
         </list>
       </option>
     </ChoiceTask>
     ```

  - Scala now has 2 environments: sbt and Gradle. Empty environment now named Gradle

    Before:
    ```xml
    <EduCourse>
      <!-- other fields -->
      <option name="environment" value="" />
      <option name="language" value="Scala" />
    </EduCourse>
    ```

    After:
    ```xml
    <EduCourse>
      <!-- other fields -->
      <option name="environment" value="Gradle" />
      <option name="language" value="Scala" />
    </EduCourse>
    ```

16. AnswerPlaceholder.length contains visible length. In course creator xml it now contains possibleAnswer.length

     Before:
      ```xml
      <AnswerPlaceholder>
        <!-- other fields -->
        <option name="length" value="1" />
        <option name="offset" value="13" />
        <option name="placeholderText" value="1" />
        <option name="possibleAnswer" value="solution" />
      </AnswerPlaceholder>
      ```

     After:
     ```xml
     <AnswerPlaceholder>
       <!-- other fields -->
       <option name="length" value="8" />
       <option name="offset" value="13" />
       <option name="placeholderText" value="1" />
       <option name="possibleAnswer" value="solution" />
     </AnswerPlaceholder>
     ```

### JSON format version

4. Skipped. 

    Name of additional materials was changed from `PyCharm additional materials` to `Edu additional materials`.
    We created the corresponding migration but only for local json representation (for Stepik we convert it into `PyCharm additional materials` again).
    So version wasn't really changed. Skip it to keep migration code in consistency.
    
5. Drop `AnswerPlaceholderSubtaskInfo` and move all info into `AnswerPlaceholder` object.

    Previous format of `AnswerPlaceholder` in task object:
    ```json
    {
      "offset": 1,
      "length": 10,
      "subtask_infos": [
        {
          "hints": [
            "hint 1",
            "hint 2"
          ],
          "possible_answer": "answer1",
          "placeholder_text": "type here",
          "has_frame": true,
          "need_insert_text": false,
          "index": 0
        }
      ]
    }
    ```
    or in submission reply object:
    ```json
    {
      "offset": 1,
      "length": 10,
      "subtask_infos": {
        "0": {
          "hints": [
            "hint 1",
            "hint 2"
          ],
          "possible_answer": "answer1",
          "placeholder_text": "type here",
          "has_frame": true,
          "need_insert_text": false,
          "selected": false,
          "status": "Solved"
        }
      }
    }
    ```

    Current format of `AnswerPlaceholder`:
    ```json
    {
      "offset": 1,
      "length": 10,
      "hints": [
        "hint 1",
        "hint 2"
      ],
      "possible_answer": "answer1",
      "placeholder_text": "type here",
      "selected": false,
      "status": "Solved"
    }
    ```
    
    Replace `text` (`task_texts` for local courses) map in `option` (`task` for local courses) object by `description_text` field 
    because without subtasks `text` map always contains only one entry.
    Add `description_format` field which describes description text format. Can be `html` and `md` for html and markdown syntax accordingly.

    Before:
    ```json
    {
      ...
      "text": [
        {
           "name": "task",
           "text": "Write task description here using markdown or html"
        }
      ]
    }
    ```
    
    After:
    ```json
    {
      ...
      "description_text": "Write task description here using markdown or html",
      "description_format": "html"
    }
    ```

6. Convert `additional_files` list to map and add `is_visible` property to each element

    Before:
    ```json
    {
      // other properties
      "additional_files": [
        {
          "name": "additional_file.txt",
          "text": "some text"
        }
      ]
    }
    ```
    
    After:
    ```json
    {
      // other properties
      "additional_files": {
         "additional_file.txt": {
           "text": "some text",
           "is_visible": true  
         }
       }
    }
    ```
 
7. Make all paths relative to task folder
    
    Before:
    ```json
    {
      // other properties
      "files": [
        {
          "name": "Task.kt",
          "text": "some text",
          "placeholders": [
            {
              // other properties
              "dependency": {
                // other properties
                "file": "Task.kt"
              }
            }
          ]
        }
      ],
      "test": [
        {
          "name": "Tests.kt",
          "text": "some text"
        }
      ]
    }
    ```
    
    After:
    ```json
    {
      // other properties
      "files": [
        {
          "name": "src/Task.kt",
          "text": "some text",
          "placeholders": [
            {
              // other properties
              "dependency": {
                // other properties
                "file": "src/Task.kt"
              }
            }
          ]
        }
      ],
      "test": [
        {
          "name": "test/Tests.kt",
          "text": "some text"
        }
      ]
    }
    ```

8. Added course type.

    Before:
    ```json
    {
      // other properties
    }
    ```

    After:
    ```json
    {
      "course_type" : "pycharm"
      // other properties
    }
    ```

9. Merge task, test and additional files into one map.

    Local course format:
    
    Before:
    ```json
    {
      "name": "task1",
      "task_files": {
        "src/Task.kt": {
          "name": "src/Task.kt",
          "text": "fun foo(): String = TODO()",
          "placeholders": []
        },
        "test/VisibleTest.kt": {
          "name": "test/VisibleTest.kt",
          "text": "fun foo(): String = TODO()",
          "placeholders": []
        }
      },
      "test_files": {
        "test/Tests.kt": "some test text",
        "test/VisibleTest.kt": "fun foo(): String = TODO()"
      },
      "additional_files": {
        "visible_additional_file.txt": {
          "text": "some text",
          "is_visible": true
        },
        "invisible_additional_file.txt": {
          "text": "some text",
          "is_visible": false
        }
      }
      // other properties
    }    
    ```
    
    After:
    ```json
    {
      "name": "task1",
      "files": {
        "src/Task.kt": {
          "name": "src/Task.kt",
          "text": "fun foo(): String = TODO()",
          "placeholders": []
        },
        "test/VisibleTest.kt": {
          "name": "test/VisibleTest.kt",
          "text": "fun foo(): String = TODO()",
          "placeholders": []
        },
        "test/Tests.kt": {
          "name": "test/Tests.kt",
          "text": "some test text",
          "is_visible": false
        },
        "visible_additional_file.txt": {
          "name": "visible_additional_file.txt",
          "text": "some text",
          "is_visible": true
        },
        "invisible_additional_file.txt": {
          "name": "invisible_additional_file.txt",
          "text": "some text",
          "is_visible": false
        }
        // other properties
      }
    }    
    ```
    
    Stepik step option format:
    
    Before:
    ```json
    {
      "title": "task1",
      "test": [
        {
          "name": "test/Tests.kt",
          "text": "some test text"
        }
      ],
      "files": [
        {
          "name": "src/Task.kt",
          "placeholders": [],
          "is_visible": true,
          "text": "// type your solution here"
        }
      ],
      "additional_files": {
        "visible_additional_file.txt": {
          "is_visible": true,
          "text": "some text"
        },
        "invisible_additional_file.txt": {
          "is_visible": false,
          "text": "some text"
        }
      },
      "format_version": 8
      // other properties
    }
    ```
    
    After:
    ```json
    {
      "title": "task1",
      "files": [
        {
          "name": "src/Task.kt",
          "placeholders": [],
          "is_visible": true,
          "text": "// type your solution here"
        },
        {
          "name": "test/Tests.kt",
          "text": "some test text",
          "is_visible": false
        },
        {
          "name": "visible_additional_file.txt",
          "is_visible": true,
          "text": "some text"
        },
        {
          "name": "invisible_additional_file.txt",
          "is_visible": false,
          "text": "some text"
        }
      ],
      "format_version": 9
      // other properties
    }
    ```

10. a) Unified task "description_format" with xml. It's stored in upper case now.
    b) Removed "PyCharm additional materials" section and moved additional course files to the course item itself.

    Before:
    ```json
    {
      // other properties
      "description_format" : "md"
    }
    ```

    After:
    ```json
    {
      // other properties
      "description_format" : "MD"
    }
    ```
11. For Android courses replaced `Android` course type with `Pycharm` course type + `Android` environment

12. a) Unified Marketplace and Edu archives now all generated course archives are encrypted.
    Before: 
    Only archives with "course_type" : "Marketplace" were encrypted and needed EncryptionModule to be added to the ObjectMapper for 
    proper deserialization.
    After:
    All generated archives, regardless course type, are encrypted and needed EncryptionModule to be added to the ObjectMapper for
    proper deserialization.

    b) Feedback link format changed in task object

    Before:
    ```json
    {
      // other properties
      "feedback_link" : {
            "link" : "https://www.customLinkExample.org",
            "link_type" : "CUSTOM"
          }
    }
    ```

    After:
    ```json
    {
      // other properties
      "feedback_link" : "https://www.customLinkExample.org"
    }
    ```

    c) Feedback link added to course object
    ```json
    {
      // other properties
      "feedback_link" : "https://plugins.jetbrains.com/plugin/16628-kotlin-koans/reviews"
    }
    ```

13. Task file can be non-editable. For this the optional flag `is_editable` has been added to "files" block. To prevent the file from being
    changed, `is_editable` property should be set to "false". If the file can be changed, the flag may be absent or equals "true".

    Example:
    ```json
    {
    "id": 1,
    "name": "Lesson name",
    "files": {
        "src/html.kt": {
            "name": "src/html.kt",
            "placeholders": [],
            "is_visible": true,
            "text": "some text",
            "is_editable": false
        }
      }
    }
    ```
    
14. Output Tasks reworked, now input.txt file is being created at Output Task creation
    Before:
    ```json
    {
      // Output Task
      "files" : {
            "src/Main.kt" : {
              "name" : "src/Main.kt",
              "placeholders" : [ ],
              "is_visible" : true,
              "text" : "NDA7NYDJ4pT3F7ChgC7OnY7PiQg0Z9w3V0uXkpoQ4U1bA82PIPbBUwzzqamNfgOY"
            },
            "test/output.txt" : {
              "name" : "test/output.txt",
              "placeholders" : [ ],
              "is_visible" : false,
              "text" : "BPrPxsoPO0ddRfXxWyDgbA=="
            }
    }
    ```

    After:
    ```json
    {
      // Output Task
      "files" : {
            "src/Main.kt" : {
              "name" : "src/Main.kt",
              "placeholders" : [ ],
              "is_visible" : true,
              "text" : "NDA7NYDJ4pT3F7ChgC7OnY7PiQg0Z9w3V0uXkpoQ4U1bA82PIPbBUwzzqamNfgOY"
            },
            "test/output.txt" : {
              "name" : "test/output.txt",
              "placeholders" : [ ],
              "is_visible" : false,
              "text" : "BPrPxsoPO0ddRfXxWyDgbA=="
            },
            "test/input.txt" : {
              "name" : "test/input.txt",
              "placeholders" : [ ],
              "is_visible" : false,
              "text" : "BPrPxsoPO0ddRfXxWyDgbA=="
            }
    }
    ```
    
15. Courses now may have an `environment_settings` field with a value of type `Map<String, String>`.
  Map keys may be arbitrary and are created and interpreted by a configurator.
  If the map is empty, it is not written to JSON.
    ```json
    {
      // other properties
      "version" : 15,
      "environment_settings" : {
        "jvm_language_level" : "JDK_19_PREVIEW"
      },
      "edu_plugin_version" : "2023.1-2022.3-SNAPSHOT"
    }
    ```
    
16. a) Task files may now have a `highlight_level` field with two possible values: `ALL_PROBLEMS` and `NONE`.
    `ALL_PROBLEMS` means that all inspections are run in this file and all issues are highlighted.
    `NONE` means that no analysis is run in this file, and no problems are highlighted.
    `ALL_PROBLEMS` is a default value and may be omitted.
       ```json
          "files" : {
            "src/Task.kt" : {
              "name" : "src/Task.kt",
              "placeholders" : [ ],
              "is_visible" : true,
              "text" : "J75+qots3TIZHX0wYlhd
            "src/TaskWithNoHighlight.kt" : {
              "name" : "src/NoH.kt",
              "placeholders" : [ ],
              "is_visible" : true,
              "text" : "ZycH6ysF7mnmM4wNC6D1MA==",
              "highlight_level" : "NONE"
            }
          },
       ```

    b) Courses now have two separate fields `programming_language_id` and `programming_language_version` instead of single `programming_language`. So before
    ```json
    {
      // other properties
      "version" : 15,
      "programming_language" : "JAVA 11"
    }
    ```
    
    After
    ```json
    {
      // other properties
      "version" : 16,
      "programming_language_id" : "JAVA",
      "programming_language_version" : "11"
    }
    ```

17. Task files now specify if their contents are binary or not. The field "is_binary" is introduced, and
    it can be either `true`, `false` or `null`. `true` means that the contents are binary, so the `text` field is encoded in Base64.
    `false` means that the contents are textual, in this case the `text` field is not encoded anyhow. `null` means that the contents
    type is undefined, and the plugin should determine it somehow by itself. Usually it detects the type by the file name.

    The `null` value is the default value, if the `is_binary` field is absent.