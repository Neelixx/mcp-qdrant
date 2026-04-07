---
source: Upgrading-From-Mylyn-WikiText-2-7-to-2-8.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.9.0.v20251119-1306/help/Upgrading-From-Mylyn-WikiText-2-7-to-2-8.html
---

Upgrading From Mylyn WikiText 2.7 to 2.8  
[![Previous](images/prev.gif)](<Upgrading-From-Mylyn-WikiText-2-x-to-3-x.html> "Upgrading From Mylyn WikiText 2.x to 3.x")[![Next](images/next.gif)](<Upgrading-From-Mylyn-WikiText-1-x-to-2-x.html> "Upgrading From Mylyn WikiText 1.x to 2.x")  
Upgrading From Mylyn WikiText 2.x to 3.xUpgrading From Mylyn WikiText 1.x to 2.x  
  
* * *

# Upgrading From Mylyn WikiText 2.7 to 2.8

With version 2.8 of Mylyn WikiText, the dependency to `org.apache.ant` was removed from `org.eclipse.mylyn.wikitext.mediawiki`.

The two tasks `mediawiki-fetch-images` and `mediawiki-to-eclipse-help` were moved to `org.eclipse.mylyn.wikitext.mediawiki.ant`. If you use those tasks you should update your classpath.

## Ant Usage in 2.8

The following jar files will be needed for running Ant tasks:

  * `org.eclipse.mylyn.wikitext.jar`
  * `org.eclipse.mylyn.wikitext.ant.jar`
  * `org.eclipse.mylyn.wikitext.textile.jar` (only required for Textile)
  * `org.eclipse.mylyn.wikitext.confluence.jar` (only required for Confluence)
  * `org.eclipse.mylyn.wikitext.markdown.jar` (only required for Markdown)
  * `org.eclipse.mylyn.wikitext.creole.jar` (only required for Creole)
  * `org.eclipse.mylyn.wikitext.mediawiki.jar` (only required for MediaWiki)
  * `org.eclipse.mylyn.wikitext.mediawiki.ant.jar` (only required for the MediaWiki Ant tasks)
  * `org.eclipse.mylyn.wikitext.tracwiki.jar` (only required for TracWiki)
  * `org.eclipse.mylyn.wikitext.twiki.jar` (only required for TWiki)
  * `guava-12.0.jar`


* * *

[![Previous](images/prev.gif)](<Upgrading-From-Mylyn-WikiText-2-x-to-3-x.html> "Upgrading From Mylyn WikiText 2.x to 3.x")[![Mylyn-WikiText-User-Guide](images/home.gif)](<Mylyn-WikiText-User-Guide.html> "Mylyn-WikiText-User-Guide")[![Next](images/next.gif)](<Upgrading-From-Mylyn-WikiText-1-x-to-2-x.html> "Upgrading From Mylyn WikiText 1.x to 2.x")  
Upgrading From Mylyn WikiText 2.x to 3.xUpgrading From Mylyn WikiText 1.x to 2.x
