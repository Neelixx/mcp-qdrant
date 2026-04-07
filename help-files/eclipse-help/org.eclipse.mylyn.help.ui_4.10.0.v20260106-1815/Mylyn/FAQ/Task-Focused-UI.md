---
source: Task-Focused-UI.html
original_path: org.eclipse.mylyn.help.ui_4.10.0.v20260106-1815/Mylyn/FAQ/Task-Focused-UI.html
---

Task-Focused UI  
[ ![Previous](../../images/prev.gif) ](<Web-Templates-Connector.html> "Web Templates Connector") [ ![Next](../../images/next.gif) ](<Context-and-Timing-data.html> "Context and Timing data")  
Web Templates Connector Context and Timing data  
  
* * *

# Task-Focused UI

## What is the Task-Focused UI?

When you activate a task, Mylyn automatically maintains a task context by monitoring your interaction. The task context provides a predictable degree-of-interest weighting of the relevance of the elements (files, classes, methods, etc.) and relations that you work with to the active task. The Task-Focused UI uses the task context to reduce information overload and to automate the management of editors, views, change sets and other UI elements. This increases productivity while working on the task and also makes it much easier to multitask because you can switch task contexts with a single click. 

Here are some of the ways the context is used to focus the UI on the active task:

  * Filtering uninteresting elements from views (e.g. the Package Explorer) and decorating the most interesting elements
  * Automatic expansion management in views 
  * Automatic code folding in editors
  * Reordering of content assist proposals
  * Automatic management of open editors
  * Automatic change set management
  * Commit message auto-population


## Why do files disappear from Focused views when I close them?

By default Mylyn automatically manages the set of open files to match the task context, so that you don’t have to. This ensures that the editor list (viewable via mechanisms like Ctrl+E) corresponds to what you in views like the _Package Explorer_ when they are in Focused mode. When you close a file manually, you express that it is uninteresting, and as such it will be removed from the task context. It will also disappear from the corresponding automatically managed change sets for the same reason. This behavior can be turned off via _Preferences → Mylyn → Context → Manage open editors to match task context_. However, it is highly recommended since Mylyn will prevent the number of editors from bloating by automatically closing editors for elements that have decayed in interest, and always keep the editors for interesting elements open. 

## Why did all my editor tabs disappear?

When you deactivate a task, all editors will be closed, and then when you reactivate the task all the windows will be open again. Try it, it’s fun. 

Mylyn actively manages your open editors with task context. It takes some getting used to, but enables switching between tasks when you are multitasking without cluttering your editor tabs. This editor management can be disabled via _Window → Preferences → Mylyn → Context_. However, before disabling it consider reading the _“Managing open editors…”_ section of [ Task-focused programming with Mylar](<http://www-128.ibm.com/developerworks/java/library/j-mylar2/index.md#N10116>). 

## How do I get rid of an element if it is not interesting?

When a view is focused elements will disappear on their own if they are not repeatedly selected via a mechanism called interest decay. Note that decay does not use the wall clock, but instead relies on the number of selections that you have made when working on that task to determine which elements disappear. This helps make it feel very predictable.

If you want want to force one or more elements to disappear from the context use the **Remove from Focus** action on the context menu. Note that the element will still be part of the task context since it was previously part of the interaction. 

If you want to permanently delete the element from the interaction history use the **Remove from Context** action on the **Context** tab of the **Task Editor**. For example, if a company private element was part of a task context that is to be shared with a public repository, this action can be used to clean it up before sharing. Note that elements will be removed from the task context recursively. 

## Which Focused UI features can I turn off?

**All of them.** When no task is active neither are any of Mylyn’s features. When working with task contexts Mylyn’s Focused UI features are all optional and in general configurable. While many find it the key benefit of Mylyn, the entire Focused UI is optional and can be uninstalled via _Help → Software Updates → Manage Configuration_. 

The following table summarizes how the key features can be toggled:

**UI Mechanism** **Example/description** **Toggle using**  
Interest filtering Package Explorer _Focus on Active Task_ button on view toolbar   
Automatic toggling of filtering Package Explorer ''Preferences → Mylar → Context → Toggle focused mode..  
Interest decoration Bolding of landmark elements _Preferences - > General -> Appearance -> Label Decoration_  
Content assist Ranking of interesting elements Eclipse 3.2: _Java - > Editor -> Content Assist -> Work in Progress ->_ turn off Mylyn, turn on Java   
Active change sets Grouping of changes by tasks _Preferences - > Mylyn -> Team_  
Editor management Auto opening/closing of editors _Preferences - > Mylyn -> Editor Management_  
Active views Active Search and Hierarchy Only on if view is active  
  
For additional configuration options see the _Mylyn_ and _General → Appearance_ preference pages. 

**Note** : if you have turned off automatic focusing of views consider using the _Navigate → Quick Context View_ facility. 

## Why can’t I Alt+Click to references libraries?

Due to [bug 200832](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=200832>), if you have the “Show ‘Referenced Libraries’ Node” option enabled in the view menu of the _Package Explorer_ you will not be able to Alt+click to library nodes if there isn’t a library visible already. Disable this option to make Alt+Click work. 

## Why is the _Link with Editor_ button disabled? 

Mylyn automatically turns on editor linking when the view is focused, since the main use case for turning it off (having the view jump around) is remedied by focusing the view. In other words, the button is pressed for you automatically and we need to ensure that it cannot be manually unchecked, which is why it appears disabled.

## What happened to the Active Search and Active Hierarchy views?

These views were not included in the Mylyn 1.0 release because they never made it beyond the experimental phase. 

  * The _Active Search_ view has not been sufficiently tuned to adapt to the lifecycle of a task context, and as a result requires manual manipulation of the degree-of-separation scope as you work on long-running tasks. Otherwise it becomes overloaded with elements. In addition, it is not yet clear whether an additional view is the right UI for this facility, and it is hard to find room for an additional view of this size on screen resolutions of 1600x1200 and smaller. 
  * The _Active Hierarchy_ view is also hard to allocate space for, especially when using the in-place hierarchy (Ctrl+T) on a landmark can be a quick way to see the relevant part of the task context’s hierarchy. 


These features still show promise in displaying task context and saving repetitive searches, so we have not removed them. They have instead moved to the Mylyn Sandbox, and can be used and experimented with by following the [instructions on the Contributors page](<http://wiki.eclipse.org/index.php/Mylyn_Contributor_Reference#Sandbox>). For feedback on these views please use the corresponding bug reports or newsgroup. 

## Why does startup of org.eclipse.mylyn.context.ui take so long?

If you are seeing the Eclipse splash screen stall for 10s of seconds while loading org.eclipse.mylyn.context.ui remove `workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.mylyn.resources.ui.prefs` as a work-around. This files stores editor mementos and can sometimes grow very large and affect startup time. This issue is being tracked on [bug 226618](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=226618>). 

* * *

[ ![Previous](../../images/prev.gif) ](<Web-Templates-Connector.html> "Web Templates Connector") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<Context-and-Timing-data.html> "Context and Timing data")  
Web Templates Connector Context and Timing data
