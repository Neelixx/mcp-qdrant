---
source: WikiText.html
original_path: org.eclipse.mylyn.help.ui_4.10.0.v20260106-1815/Mylyn/FAQ/WikiText.html
---

WikiText  
[ ![Previous](../../images/prev.gif) ](<Team-Support.html> "Team Support") [ ![Next](../../images/next.gif) ](<Integration-with-other-tools.html> "Integration with other tools")  
Team Support Integration with other tools  
  
* * *

# WikiText

## What is WikiText?

WikiText is a set of plug-ins for Eclipse that provide lightweight markup (wiki) parsing, editing and display capabilities to the Eclipse platform and Mylyn. WikiText provides a parser for wiki markup and converts the markup to HTML, Docbook, DITA, or Eclipse Help format, either via the API or by using Ant tasks. WikiText also provides UI components (such as an editor) integrating with Eclipse and the Mylyn task editor.

## Where can I get WikiText?

WikiText is available on the Mylyn update site. Unreleased weekly builds are also available via the Mylyn Weekly Builds update site. See the [Mylyn Downloads](<http://www.eclipse.org/mylyn/downloads/>) for details. 

A stand-alone WikiText package is also available for download from the [Mylyn Downloads](<http://www.eclipse.org/mylyn/downloads/>) page. 

Incubation features and unreleased builds are available from the [nightly update site](<http://download.eclipse.org/mylyn/snapshots/nightly/docs>) by installing the WikiText Extras feature. These builds should be used with caution as they are untested and potentially unstable. 

## How does WikiText integrate with Mylyn?

WikiText extends the Mylyn task editor to be markup-aware. Comments and description text is formatted according to the configured markup language. The description and comment editors are aware of markup and provide content assist, markup help and preview.

More details can be found here: [Rich Editing for Tasks via Mylyn WikiText (Mik Kersten)](<http://tasktop.com/blog/eclipse/rich-editing-for-tasks-via-mylyn-wikitext>)

## How do I enable/disable WikiText extensions to Mylyn?

This is done on a per-repository basis. When WikiText is installed it is automatically enabled for all configured task repositories for which there is a default markup language setting.

To change the default settings open the Mylyn _Task Repositories_ view, right-click your task repository and select _Properties_ from the context menu. In the properties dialog choose the _Editor_ settings (you may need to click on it to expand the section).

To disable WikiText for your repository, select _Plain Text_.

To enable WikiText for your repository, select the desired markup language. The default markup language if available for your repository is labeled _(default)_

## Where can I find WikiText documentation?

WikiText documentation is installed into the Eclipse help system when WikiText is installed. To see the WikiText documentation open Eclipse, from the _Help_ menu open _Help Contents_. You will find the _WikiText User Guide_ under _Tasks_ in the table of contents. If you're interested in integrating with WikiText then take a look at the _WikiText Developer Guide_.

The same WikiText documentation is also available in the stand-alone distribution and online:

  * [WikiText User Guide](<http://help.eclipse.org/luna/topic/org.eclipse.mylyn.wikitext.help.ui/help/Mylyn%20WikiText%20User%20Guide.md>)
  * [WikiText Developer Guide](<http://help.eclipse.org/luna/topic/org.eclipse.mylyn.wikitext.help.ui/help/devguide/WikiText%20Developer%20Guide.md>)


## How do I run the WikiText Ant tasks?

The WikiText documentation provides detailed information on how this is done in the _WikiText User Guide_.

## Can I use WikiText without Eclipse?

Yes, the WikiText markup parser and Ant tasks may be used outside of Eclipse without reference to any Eclipse classes.

Detailed information about using WikiText APIs is available within the _WikiText Developer Guide_. Information about using WikiText Ant tasks is available within the _WikiText User Guide_.

## What output can WikiText create?

WikiText can create HTML, Eclipse Help, DITA, DocBook and XSL-FO from wiki markup. Using the WikiText APIs you can also extend WikiText to create other output formats. DITA, DocBook and XSL-FO can all be used to create PDF. More information is available in the _WikiText User Guide_.

## What wiki markup languages does WikiText support?

WikiText can parse the following markup languages:

  * Confluence
  * MediaWiki
  * Markdown
  * Textile
  * TracWiki
  * TWiki


Additionally the following markup languages are in incubation status, available from the WikiText [nightly update site](<http://download.eclipse.org/mylyn/snapshots/nightly/docs>): 

  * Creole (see [Mylyn WikiText Creole HOWTO](<http://wiki.eclipse.org/Mylyn/WikiText/Creole> "Mylyn/WikiText/Creole")) 
  * AsciiDoc (see [AsciiDoc HOWTO](<http://wiki.eclipse.org/Mylyn/WikiText/AsciiDoc> "Mylyn/WikiText/AsciiDoc")) 
  * A Markdown implementation that conforms to the [CommonMark](<http://commonmark.org>) specification 


WikiText is also designed to make it easy to add support for new markup languages.

## Why doesn't the preview tab show up in the WikiText editor?

The preview tab is not shown if the SWT browser is not configured correctly. See [The SWT FAQ](<http://www.eclipse.org/swt/faq.php#browserspecifydefault>) for details. 

## Where can I find out more about WikiText?

  * [Mylyn WikiText 1.0 Released (David Green)](<http://greensopinion.blogspot.com/2009/03/mylyn-wikitext-10-released.md>)
  * [Rich Editing for Tasks via Mylyn WikiText (Mik Kersten)](<http://tasktop.com/blog/eclipse/rich-editing-for-tasks-via-mylyn-wikitext>)
  * [Getting started with WikiText (Peter Friese)](<http://www.peterfriese.de/getting-started-with-wikitext/>)
  * [Advanced WikiText (Peter Friese)](<http://www.peterfriese.de/advanced-wikitext/>)
  * [DocumentationGuidelines/Example](<http://wiki.eclipse.org/DocumentationGuidelines/Example> "DocumentationGuidelines/Example")
  * [Mylyn WikiText Produces PDF (David Green)](<http://greensopinion.blogspot.com/2009/04/mylyn-wikitext-produces-pdf.md>)
  * [Mylyn WikiText wiki](<http://wiki.eclipse.org/Mylyn/WikiText> "Mylyn/WikiText")
  * [WikiText & Eclipse: Making Documentation Easier (Jon Dowdle)](<http://jdowdle.com/wp/2010/04/wikitext-eclipse-creating-documentation/>)
  * [Diagrams in wiki markup with Mylyn WikiText, DOT, and Zest (Fabian Steeg)](<http://fsteeg.wordpress.com/2010/02/07/diagrams-in-wiki-markup-with-mylyn-wikitext-dot-and-zest/>)
  * [The original WikiText incubation project page](<http://wiki.eclipse.org/Mylyn/Incubator/WikiText> "Mylyn/Incubator/WikiText")
  * [Textile-J Is Moving to Mylyn WikiText](<http://greensopinion.blogspot.com/2008/08/textile-j-is-moving-to-mylyn-wikitext.md>)


* * *

[ ![Previous](../../images/prev.gif) ](<Team-Support.html> "Team Support") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<Integration-with-other-tools.html> "Integration with other tools")  
Team Support Integration with other tools
