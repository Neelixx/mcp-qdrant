---
source: Preferences.html
original_path: org.eclipse.mylyn.help.ui_4.10.0.v20260106-1815/Mylyn/User_Guide/Preferences.html
---

Preferences  
[ ![Previous](../../images/prev.gif) ](<Shortcuts.html> "Shortcuts") [ ![Next](../../images/next.gif) ](<Task-Repository-Connectors.html> "Task Repository Connectors")  
Shortcuts Task Repository Connectors  
  
* * *

# Preferences

You can access the following Mylyn preference pages by navigating to Window -> Preferences -> Tasks.

## Tasks

  * **Synchronization** \- Set how often queries in your task list should update from your task repository. The default is 20 minutes. 
  * **Scheduling** \- Set the day when your week begins. This is used to determine whether tasks should appear as scheduled for this week. 
  * **Task Editing** \- Select whether tasks should be opened in the rich editor or an integrated browser window displaying the web interface for the task. 


  
Click "Advanced" to reveal the following additional settings. 

  * **Task Timing** \- When a task is active, the time spent working on the task is recorded. If you check "Enable inactivity timeouts", time will not be accumulated while you are not actively working. You can set the number of minutes after which time will stop being accumulated toward the active task. 
  * **Task Data** \- Specify the location where your task list and task context data is stored. 


## Context

Use the following checkboxes to set your preferences for the task-focused interface.

  * **Auto focus navigator view on task activation** \- Automatically toggle "Focus on Active Task" on when activating a task in navigation views such as the Package Explorer. 
  * **Auto expand tree views when focused** \- When toggling "Focus on Active Task", automatically expand trees so that all interesting elements are visible. 
  * **Manage open editors to match task context** \- When checked, activating a task will automatically open editors corresponding to the most interesting files in the task context. When deactivating a task, all editors will automatically close. While a task is active, files that become less interesting will automatically close as you work. 
  * **Remove file from context when editor is closed** \- When this option is checked, closing an editor will be considered an indication that you not interested in the corresponding file. Therefore, files you close will be removed from the task context. If you tend to close editors for files that you may want to return to, try unchecking this setting. 
  * **Open last used perspective on task activation** \- When this option is checked, activating a task will automatically switch to the perspective that was in use when the task was last active. 


## Resources

Use this preference page to add or remove resources that should not be included in the context of a task. Typically, excluded files are hidden backup or lock files that are not intented to be opened directly by the user of an application.

## Breakpoints

Use this preference page to enable breakpoints in context.

  * **Include breakpoints in task context (Experimental)** \- When this option is checked, each task will have its own set of breakpoints which will be shared when you attach context to the task. Breakpoints created while a task is active will be removed from the workspace on task deactivation and restored the next time the task is activated. This feature has the following limitations (tracked on [bug 428378](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=428378>)): 
    * Breakpoints in closed projects are deleted from the context on task activation and cannot be recovered. 
    * Breakpoints stored in context will not have their locations updated as the code changes, so they may be restored at the wrong location.


## Team

  * **Automatically create and manage with task context** \- Enables automatic change set management. Change sets will be created automatically so that you can commit or update only resources that are in a task's context. 
  * **Commit Message Template** \- Set the values that will appear in commit messages that are automatically generated when committing resources associated with a task. Pressing Ctrl+Space activates content assist which displays a list of the variables. Clicking once on a variable shows a description of that variable. For example, the variable ${task.id} provides the ID of the task associated with the commit. 


* * *

[ ![Previous](../../images/prev.gif) ](<Shortcuts.html> "Shortcuts") [ ![Mylyn User Guide](../../images/home.gif) ](<User-Guide.html> "Mylyn User Guide") [ ![Next](../../images/next.gif) ](<Task-Repository-Connectors.html> "Task Repository Connectors")  
Shortcuts Task Repository Connectors
