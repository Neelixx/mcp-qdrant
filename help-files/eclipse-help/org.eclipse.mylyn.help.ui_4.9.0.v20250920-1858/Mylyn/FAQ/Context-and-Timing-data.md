---
source: Context-and-Timing-data.html
original_path: org.eclipse.mylyn.help.ui_4.9.0.v20250920-1858/Mylyn/FAQ/Context-and-Timing-data.html
---

Context and Timing data  
[ ![Previous](../../images/prev.gif) ](<Task-Focused-UI.html> "Task-Focused UI") [ ![Next](../../images/next.gif) ](<Java-Development.html> "Java Development")  
Task-Focused UI Java Development  
  
* * *

# Context and Timing data

## How do I prevent code checked out from polluting my task context?

When checking out a new project, you must first deactivate the active task. Otherwise all newly created files will become interesting. 

## Why do I see strange elapsed times on my Planning tab?

Some platform/jvm combinations can fill with invalid timestamps. This is known to have happened on Mac OSX 10.4 with Java 1.5. See [bug 207419](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=207419>). To resolve you can try and manually edit <workspace>/.metadata/.mylyn/contexts/activity.xml.zip. 

## Is the backwards compatibility and refactoring of task context handled?

  * The Task List and Context Store are compatible across all currently-supported Eclipse versions. This means that you can use the same .mylyn data in both Eclipse 3.x. 


  * If elements have been renamed they may not appear as interesting when the context is activated.


* * *

[ ![Previous](../../images/prev.gif) ](<Task-Focused-UI.html> "Task-Focused UI") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<Java-Development.html> "Java Development")  
Task-Focused UI Java Development
