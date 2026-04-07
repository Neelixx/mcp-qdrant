---
source: Task-Editor.html
original_path: org.eclipse.mylyn.help.ui_4.9.0.v20250920-1858/Mylyn/FAQ/Task-Editor.html
---

Task Editor  
[ ![Previous](../../images/prev.gif) ](<Task-List.html> "Task List") [ ![Next](../../images/next.gif) ](<Task-Repositories.html> "Task Repositories")  
Task List Task Repositories  
  
* * *

# Task Editor

## When I submit a new bug to eclipse.org the priority isn’t updated?

Eclipse.org’s Bugzilla repository forces all new bug reports to priority 3 (P3) regardless of what is selected in Mylyn. Eventually we will disable this field in the new bug editor for bugs.eclipse.org and provide a tooltip with explanation ( [bug 204630](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=204630>) ). 

## Why am I seeing strange boxes where I expect to see proper characters?

If for example you aren’t seeing the proper single quote chacters in the summary of [https://bugs.eclipse.org/bugs/show_bug.cgi?id=197644 bug#197644](<http://wiki.eclipse.org/https://bugs.eclipse.org/bugs/show_bug.cgi?id=197644_bug#197644> "https://bugs.eclipse.org/bugs/show_bug.cgi?id=197644 bug#197644"), check that the encoding is set correctly for the repository in the asociated Task Repositories view (Properties → Additional Settings → Character Encoding). For bugs.eclipse.org/bugs set your charcter encoding to _ISO-8859-1_. 

## How can I view images or screenshots that are attached to an image?

By default Eclipse does not provide a built-in image viewer so images can either be opened with a browser or saved to disk and opened in a native image viewer. Alternatively Eclipse extension such as QuickImage can be installed which support opening images within Eclipse.

* * *

[ ![Previous](../../images/prev.gif) ](<Task-List.html> "Task List") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<Task-Repositories.html> "Task Repositories")  
Task List Task Repositories
