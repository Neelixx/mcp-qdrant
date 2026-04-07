---
source: WikiText-Developer-Guide.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.9.0.v20251119-1306/help/devguide/WikiText-Developer-Guide.html
---

WikiText-Developer-Guide  
[![Next](images/next.gif)](<Using-The-WikiText-Parser.html> "Using The WikiText Parser")  
Using The WikiText Parser  
  
* * *

# Overview

This document provides detailed information for software developers that wish to include or use WikiText as part of their application.

For information about using WikiText please see the **WikiText User Guide**.

## WikiText API

WikiText follows Eclipse conventions with respect to package naming. Classes that appear in packages with `internal` in their name are not intended for use externally.

Most API classes have good javadoc. When using WikiText classes, expect them to behave according to their javadoc. If you find somewhere where the javadoc is not clear, please [file a bug](<Contributing.md#IssueTracking>).

## How It Works

The markup parser uses regular expressions to parse wiki markup, converting the markup to XHTML or other outputs depending on the configured `DocumentBuilder`. See also [Markup Language Concepts](<Markup-Languages.md#MarkupLanguageConcepts>)

### JFace Viewer

The WikiText JFace viewer extends the standard JFace source viewer. The WikiText JFace viewer uses the markup parser to convert the text to XHTML. It then parses the XHTML and converts it to a standard unicode character sequence and a TextPresentation. It uses standard JFace techniques to apply styles to the rendered text and uses unicode character features such as newlines, tabs and bullet characters to make the text look as it is intended.

As the wiki markup is converted to text with a TextPresentation an AnnotationModel is (optionally) constructed, such that the text viewer has access to annotations for the following:

  * Tags with id (from `<p id="some-id">`)
  * Anchor href (from `<a href="http://www.foo-bar.com">`)
  * Anchor name (from `<a name="foo-bar">`)
  * CSS classes
  * Titles (from `<acronym title="some title">`)


The viewer can then present pop-ups (tool-tip hover information, for example), and handle in-document and out-of-document hyperlinks.

The viewer implementation has the benefit of being lightweight compared to a browser and it supports standard viewer features such as text selection and clipboard copy.

#### JFace Viewer Limitations

The following limitations apply to the JFace text-only markup rendering viewer. The limitations can be avoided by using the SWT Browser widget instead.

Since the viewer is a text-only rendering solution for markup it cannot display advanced markup constructs such as tables. Images can be displayed due to WikiText's custom painting.

The viewer is also generally only useful for read-only usage, since the viewer does not maintain a mapping back to the original markup.

Anchors (ie: hyperlinks) within the page are implemented. If you use footnotes, clicking on the footnote anchor will scroll the viewer to display the footnote text at the bottom.

## Classpath and Dependencies

WikiText is designed for use both within an Eclipse runtime, and stand-alone.

### Within Eclipse

WikiText use within an Eclipse runtime is simple: simply add the desired WikiText bundles as dependencies to your plug-in project. This is the recommended approach for Eclipse plug-ins and for RCP applications.

### Stand-Alone

Stand-alone usage of WikiText is also possible. To use WikiText outside of an Eclipse runtime, simply add the WikiText jar files to your classpath. Note that stand-alone usage of WikiText is limited to the use of the markup parser framework and Ant tasks.

Add the `org.eclipse.mylyn.wikitext*.jar` to your classpath, and at least one the following language-specific jars:

  * `org.eclipse.mylyn.wikitext.asciidoc*.jar`
  * `org.eclipse.mylyn.wikitext.confluence*.jar`
  * `org.eclipse.mylyn.wikitext.markdown*.jar`
  * `org.eclipse.mylyn.wikitext.mediawiki*.jar`
  * `org.eclipse.mylyn.wikitext.textile*.jar`
  * `org.eclipse.mylyn.wikitext.tracwiki*.jar`
  * `org.eclipse.mylyn.wikitext.twiki*.jar`


* * *

[![Next](images/next.gif)](<Using-The-WikiText-Parser.html> "Using The WikiText Parser")  
Using The WikiText Parser
