---
source: Upgrading-From-Mylyn-WikiText-1-x-to-2-x.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.9.0.v20251119-1306/help/Upgrading-From-Mylyn-WikiText-1-x-to-2-x.html
---

Upgrading From Mylyn WikiText 1.x to 2.x  
[![Previous](images/prev.gif)](<Upgrading-From-Mylyn-WikiText-2-7-to-2-8.html> "Upgrading From Mylyn WikiText 2.7 to 2.8")[![Next](images/next.gif)](<More-Information.html> "More Information")  
Upgrading From Mylyn WikiText 2.7 to 2.8More Information  
  
* * *

# Upgrading From Mylyn WikiText 1.x to 2.x

For 2.0 Mylyn WikiText has been reorganized into several new jar files (OSGi bundles) to better manage dependencies.

Notable changes when upgrading from 1.x to 2.0:

## Ant Usage in 2.0

  1. The Ant classpath has changed. The Ant classpath definition should include all jars provided with the standalone distribution of Mylyn WikiText. The classpath should be defined with `<include name="*.jar"/>` (see the [examples](<Markup-Generation.md#ContentGenerationfromWikiMarkupusingAnt>) ). The following jar files will be needed for running Ant tasks:
     * `org.eclipse.mylyn.wikitext.jar`
     * `org.eclipse.mylyn.wikitext.ant.jar` (new for 2.0)
     * `org.eclipse.mylyn.wikitext.textile.jar` (only required for Textile)
     * `org.eclipse.mylyn.wikitext.confluence.jar` (only required for Confluence)
     * `org.eclipse.mylyn.wikitext.markdown.jar` (only required for Markdown)
     * `org.eclipse.mylyn.wikitext.creole.jar` (only required for Creole)
     * `org.eclipse.mylyn.wikitext.mediawiki.jar` (only required for MediaWiki)
     * `org.eclipse.mylyn.wikitext.tracwiki.jar` (only required for TracWiki)
     * `org.eclipse.mylyn.wikitext.twiki.jar` (only required for TWiki)
     * `guava-12.0.jar` Guava is now required (new for 2.0)
  2. Ant taskdefs must reference a new properties file `<taskdef resource="org/eclipse/mylyn/wikitext/core/ant/tasks.properties"` see [Ant Examples](<Markup-Generation.md#AntExamples>) for more details


## API Changes in 2.0

Many API-breaking changes have been made to Mylyn WikiText 2.0. In most cases client code upgrading to 2.0 will simply need to be recompiled, however in a few cases client code will have to change. All changes have been annodated with the javadoc tag `@since 2.0`.

  * Many methods in `org.eclipse.mylyn.wikitext.parser.markup.MarkupLanguage` have been moved to `org.eclipse.mylyn.wikitext.parser.markup.AbstractMarkupLanguage`.
  * `org.eclipse.mylyn.wikitext.validation.MarkupValidator` no longer accepts an `IProgressMonitor`
  * `org.eclipse.mylyn.wikitext.WikiText` has been moved to `org.eclipse.mylyn.wikitext.ui.WikiText`
  * Applications running in OSGi should use `org.eclipse.mylyn.wikitext.osgi.OsgiServiceLocator` instead of `org.eclipse.mylyn.wikitext.util.ServiceLocator`.


* * *

[![Previous](images/prev.gif)](<Upgrading-From-Mylyn-WikiText-2-7-to-2-8.html> "Upgrading From Mylyn WikiText 2.7 to 2.8")[![Mylyn-WikiText-User-Guide](images/home.gif)](<Mylyn-WikiText-User-Guide.html> "Mylyn-WikiText-User-Guide")[![Next](images/next.gif)](<More-Information.html> "More Information")  
Upgrading From Mylyn WikiText 2.7 to 2.8More Information
