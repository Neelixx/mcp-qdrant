---
source: Team-Support.html
original_path: org.eclipse.mylyn.help.ui_4.9.0.v20250920-1858/Mylyn/FAQ/Team-Support.html
---

Team Support  
[ ![Previous](../../images/prev.gif) ](<Java-Development.html> "Java Development") [ ![Next](../../images/next.gif) ](<WikiText.html> "WikiText")  
Java Development WikiText  
  
* * *

# Team Support

## My change set is missing or doesn’t contain elements it should. Help!

If a task change set disappears or is missing items, toggle the Incoming/Outgoing mode of the Synchronize view via its toolbar button. 

## Why does task change set not appear when I modify files?

A task change set should appear if you activate a task and modify a file connected to a compatible source repository (e.g. CVS, SVN). If it does not try the following:

  * Ensure you have installed the [ <http://www.eclipse.org/mylyn/downloads/> _Mylyn Bridge: Eclipse IDE_ feature]. Note that previously this feature was named Eclipse SDK and not distributed with the Java-only Eclipse download ( [bug 191793](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=191793>)). 


  * If you are using Subclipse or Subversive, make sure you have the respective integration for Mylyn installed. Note that change sets are not currently supported in the EGit connector.


  * Verify that the [[Mylyn/User_Guide#Task-focused_Change_Sets|configured _Synchronize View_ is configured for change sets]]. 


  * Verify that Mylyn Tasks UI and Mylyn Team UI are enabled under General → Startup and Shutdown in the Eclipse preferences.


If the above are working but the change set appears to be missing relevant files:

  * You may be seeing the _Synchronize_ view’s refresh problem ( [bug 142395](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=142395>)). Toggle from _Incoming/Ougtoing_ to _Incoming_ mode and back again to refresh the view. 


  * If the files or change set are still missing deactivate and reactivate the task to force a full refresh.


## Why do files disappear from the change set when I close them?

If you have the _Preferences → Mylyn → Context → Manage open editors to match task context_ option enabled, Mylyn will perform editor management so that you don’t have to. Closing a file makes it less interesting, and causes it to disappear from the active task context, and hence from the change set. This option prevents you from having to manage the set of open files, will automatically close editors for files that become uninteresting, and will ensure that the open editors match the task context. 

## Why am I missing elements when I retrieve someone else’s context?

To identify elements within a project, Mylyn relies on the name of the project being consistent across workspaces. If the project name in the workspace that the context was created with is different than its name in your workspace, the task context will not show elements within that project ( [bug 164058](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=164058>)). The other case where context can be lost is if the elements change names outside of the workspace ( [bug 164243](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=164243>)). 

The work-around is to use the same project names across your team’s workspaces. We recommend the following two approaches for standardizing on project names:

  * Use **Team Project Sets** : these are a very useful Eclipse facility that allows your entire team to check out numerous projects from version control by importing a single file. Create a project set via _File → Export → Team Project Set_ , host this file somewhere accessible, then have others import vai _File → Import → Team Project Set_. 
  * Have developers check out projects into their workspace without renaming them. The above is a shortcut for doing this. If alphabetical sort order in the _Package Explorer_ is a problem, organize your projects via _Project Explorer → view menu → Top Level Elements → Working Sets_. 


* * *

[ ![Previous](../../images/prev.gif) ](<Java-Development.html> "Java Development") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<WikiText.html> "WikiText")  
Java Development WikiText
