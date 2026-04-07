---
source: Upgrading-From-Mylyn-WikiText-2-x-to-3-x.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.10.0.v20260106-1815/help/Upgrading-From-Mylyn-WikiText-2-x-to-3-x.html
---

Upgrading From Mylyn WikiText 2.x to 3.x  
[![Previous](images/prev.gif)](<Preferences.html> "Preferences")[![Next](images/next.gif)](<Upgrading-From-Mylyn-WikiText-2-7-to-2-8.html> "Upgrading From Mylyn WikiText 2.7 to 2.8")  
PreferencesUpgrading From Mylyn WikiText 2.7 to 2.8  
  
* * *

# Upgrading From Mylyn WikiText 2.x to 3.x

Some API changes and changes affecting Ant usage have been introduced in Mylyn WikiText 3.0. Notably:

  * Java package naming conventions: `core` has been removed from Java package names, for example the `org.eclipse.mylyn.wikitext.core` Java package has been renamed to `org.eclipse.mylyn.wikitext`
  * Ant taskdef resource paths no longer have `/core/`, for example Ant build scripts that had `<taskdef resource="org/eclipse/mylyn/wikitext/core/ant/tasks.properties"` should now use `<taskdef resource="org/eclipse/mylyn/wikitext/ant/tasks.properties"`
  * OSGi bundles:
    * Symbolic Names: Symbolic names of some OSGi bundles has changed, removing the ".core" suffix. For example, `org.eclipse.mylyn.wikitext.core` has become `org.eclipse.mylyn.wikitext`
    * Exports: internal packages are no longer exported by Mylyn WikiText OSGi bundles
    * Dependencies: dependencies have been changed from `Require-Bundle` to `Import-Package`


* * *

[![Previous](images/prev.gif)](<Preferences.html> "Preferences")[![Mylyn-WikiText-User-Guide](images/home.gif)](<Mylyn-WikiText-User-Guide.html> "Mylyn-WikiText-User-Guide")[![Next](images/next.gif)](<Upgrading-From-Mylyn-WikiText-2-7-to-2-8.html> "Upgrading From Mylyn WikiText 2.7 to 2.8")  
PreferencesUpgrading From Mylyn WikiText 2.7 to 2.8
