---
source: Mylyn-WikiText-User-Guide.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.9.0.v20251119-1306/help/Mylyn-WikiText-User-Guide.html
---

Mylyn-WikiText-User-Guide  
[![Next](images/next.gif)](<Getting-Started.html> "Getting Started")  
Getting Started  
  
* * *

# Overview

This guide provides basic instructions on the use of WikiText.  
WikiText provides extensions to the Mylyn task editor supporting [lightweight markup languages](<http://en.wikipedia.org/wiki/Lightweight_markup_language>) such as Textile and MediaWiki. WikiText also provides a wiki text editor for Eclipse, a Maven plug-ins and Ant tasks for generating from lightweight markup to HTML and other formats.

For information about integrating your application with WikiText please see the **WikiText Developer Guide**.

## Table of Contents

  1. Overview
     1. Table of Contents
  2. [Getting Started](<Getting-Started.md#GettingStarted>)
     1. [Creating A New File](<Getting-Started.md#CreatingANewFile>)
     2. [WikiText Editor Overview](<Getting-Started.md#WikiTextEditorOverview>)
        1. [Markup Source Tab](<Getting-Started.md#MarkupSourceTab>)
        2. [Preview Tab](<Getting-Started.md#PreviewTab>)
        3. [Outline](<Getting-Started.md#Outline>)
        4. [Folding](<Getting-Started.md#Folding>)
           1. [Active Folding](<Getting-Started.md#ActiveFolding>)
     3. [Switching Markup Languages](<Getting-Started.md#SwitchingMarkupLanguages>)
     4. [Accessing the Markup Cheat-Sheet](<Getting-Started.md#AccessingtheMarkupCheatSheet>)
     5. [Project Settings](<Getting-Started.md#ProjectSettings>)
  3. [Task Editor Integration](<Task-Editor-Integration.md#TaskEditorIntegration>)
     1. [Repository Configuration](<Task-Editor-Integration.md#RepositoryConfiguration>)
     2. [Task Editor Appearance](<Task-Editor-Integration.md#TaskEditorAppearance>)
        1. [Task Editor Fonts](<Task-Editor-Integration.md#TaskEditorFonts>)
     3. [Markup for Task Repositories](<Task-Editor-Integration.md#MarkupforTaskRepositories>)
        1. [Markup for Bugzilla](<Task-Editor-Integration.md#MarkupforBugzilla>)
  4. [Markup Generation](<Markup-Generation.md#MarkupGeneration>)
     1. [Generation In Eclipse](<Markup-Generation.md#GenerationInEclipse>)
        1. [Content Generation from Wiki Markup](<Markup-Generation.md#ContentGenerationfromWikiMarkup>)
     2. [Content Generation from Wiki Markup using Maven](<Markup-Generation.md#ContentGenerationfromWikiMarkupusingMaven>)
     3. [Content Generation from Wiki Markup using Ant](<Markup-Generation.md#ContentGenerationfromWikiMarkupusingAnt>)
        1. [Javadoc Links](<Markup-Generation.md#JavadocLinks>)
        2. [PDF and XSLFO](<Markup-Generation.md#PDFandXSLFO>)
           1. [PDF from XSL-FO Quick-Start](<Markup-Generation.md#PDFfromXSLFOQuickStart>)
        3. [DocBook](<Markup-Generation.md#DocBook>)
        4. [DITA](<Markup-Generation.md#DITA>)
           1. [wikitext-to-dita - Multiple Files Example](<Markup-Generation.md#wikitexttoditaMultipleFilesExample>)
           2. [wikitext-to-dita - Single Output File Example](<Markup-Generation.md#wikitexttoditaSingleOutputFileExample>)
        5. [MediaWiki To Eclipse Help](<Markup-Generation.md#MediaWikiToEclipseHelp>)
     4. [Html To WikiText](<Markup-Generation.md#HtmlToWikiText>)
     5. [Ant Examples](<Markup-Generation.md#AntExamples>)
     6. [Markup Language Customization](<Markup-Generation.md#MarkupLanguageCustomization>)
  5. [Textile Syntax](<Textile-Syntax.md#TextileSyntax>)
     1. [Textile Syntax Tips](<Textile-Syntax.md#TextileSyntaxTips>)
        1. [Whitespace](<Textile-Syntax.md#Whitespace>)
        2. [HTML Literals](<Textile-Syntax.md#HTMLLiterals>)
        3. [Images and DocBook](<Textile-Syntax.md#ImagesandDocBook>)
           1. [Inline Images v.s. Block Images](<Textile-Syntax.md#InlineImagesv.s.BlockImages>)
           2. [Image Scaling](<Textile-Syntax.md#ImageScaling>)
           3. [Image Size](<Textile-Syntax.md#ImageSize>)
        4. [Extended Blocks](<Textile-Syntax.md#ExtendedBlocks>)
     2. [Textile Extensions](<Textile-Syntax.md#TextileExtensions>)
     3. [Examples](<Textile-Syntax.md#Examples>)
     4. [Textile Reference](<Textile-Syntax.md#TextileReference>)
  6. [Tips and Tricks](<Tips-and-Tricks.md#TipsandTricks>)
     1. [Hot-Keys](<Tips-and-Tricks.md#HotKeys>)
     2. [Word Completion](<Tips-and-Tricks.md#WordCompletion>)
     3. [Spelling](<Tips-and-Tricks.md#Spelling>)
     4. [Content Assist](<Tips-and-Tricks.md#ContentAssist>)
        1. [Cross-References and Content Assist](<Tips-and-Tricks.md#CrossReferencesandContentAssist>)
        2. [Template-Based Content Assist](<Tips-and-Tricks.md#TemplateBasedContentAssist>)
           1. [Creating Custom Templates](<Tips-and-Tricks.md#CreatingCustomTemplates>)
        3. [Selection and Content Assist](<Tips-and-Tricks.md#SelectionandContentAssist>)
     5. [Quick Outline](<Tips-and-Tricks.md#QuickOutline>)
  7. [Preferences](<Preferences.md#Preferences>)
     1. [Editor Preferences](<Preferences.md#EditorPreferences>)
        1. [Open in Preview Mode](<Preferences.md#OpeninPreviewMode>)
        2. [Font Preferences](<Preferences.md#FontPreferences>)
     2. [Rendering Appearance](<Preferences.md#RenderingAppearance>)
  8. [Upgrading From Mylyn WikiText 2.x to 3.x](<Upgrading-From-Mylyn-WikiText-2-x-to-3-x.md#UpgradingFromMylynWikiText2.xto3.x>)
  9. [Upgrading From Mylyn WikiText 2.7 to 2.8](<Upgrading-From-Mylyn-WikiText-2-7-to-2-8.md#UpgradingFromMylynWikiText2.7to2.8>)
     1. [Ant Usage in 2.8](<Upgrading-From-Mylyn-WikiText-2-7-to-2-8.md#AntUsagein2.8>)
  10. [Upgrading From Mylyn WikiText 1.x to 2.x](<Upgrading-From-Mylyn-WikiText-1-x-to-2-x.md#UpgradingFromMylynWikiText1.xto2.x>)
     1. [Ant Usage in 2.0](<Upgrading-From-Mylyn-WikiText-1-x-to-2-x.md#AntUsagein2.0>)
     2. [API Changes in 2.0](<Upgrading-From-Mylyn-WikiText-1-x-to-2-x.md#APIChangesin2.0>)
  11. [More Information](<More-Information.md#MoreInformation>)
     1. [Feedback](<More-Information.md#Feedback>)


* * *

[![Next](images/next.gif)](<Getting-Started.html> "Getting Started")  
Getting Started
