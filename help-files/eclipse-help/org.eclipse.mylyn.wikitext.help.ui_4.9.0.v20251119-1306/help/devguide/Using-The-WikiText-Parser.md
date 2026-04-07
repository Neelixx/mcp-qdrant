---
source: Using-The-WikiText-Parser.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.9.0.v20251119-1306/help/devguide/Using-The-WikiText-Parser.html
---

Using The WikiText Parser  
[![Previous](images/prev.gif)](<WikiText-Developer-Guide.html> "Overview")[![Next](images/next.gif)](<WikiText-and-the-UI.html> "WikiText and the UI")  
OverviewWikiText and the UI  
  
* * *

# Using The WikiText Parser

WikiText provides a parser framework for parsing lightweight markup languages (wiki markup). The parser framework can output HTML, DITA, DocBook, and Eclipse Help directly, or integrators can extend `DocumentBuilder` to output other formats.

## Simple Example

To convert Textile wiki markup to HTML:
    
    
    MarkupParser markupParser = new MarkupParser();
    markupParser.setMarkupLanguage(new TextileLanguage());
    String htmlContent = markupParser.parseToHtml(markupContent);
    

To convert MediaWiki wiki markup to HTML:
    
    
    MarkupParser markupParser = new MarkupParser();
    markupParser.setMarkupLanguage(new MediaWikiLanguage());
    String htmlContent = markupParser.parseToHtml(markupContent);
    

### ServiceLocator

Your code may need to handle multiple markup languages or you may wish to eliminate compile-time dependencies on a specific markup language. The `ServiceLocator` can obtain markup languages by name:
    
    
    ServiceLocator.getInstance().getMarkupLanguage("Textile")
    

Other markup languages can be obtained by changing `"Textile"` to another markup language name. Valid markup language names include:

  * AsciiDoc
  * Confluence
  * Markdown
  * MediaWiki
  * Textile
  * TracWiki
  * TWiki


## Advanced Parser Usage

The `MarkupParser` emits content to a `DocumentBuilder` in order to create HTML or other output. To control the output more closely instantiate the `DocumentBuilder` of choice and configure it before it is used. 

In this example the `HtmlDocumentBuilder` is configured to avoid creating the `<html>` and `<body>` tags:
    
    
    StringWriter writer = new StringWriter();
    
    HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
    // avoid the <html> and <body> tags 
    builder.setEmitAsDocument(false);
    
    MarkupParser parser = new MarkupParser(new TextileLanguage());
    parser.setBuilder(builder);
    parser.parse(markupContent);
    
    String htmlContent = writer.toString();
    
    

## Adding CSS to HTML Output

CSS stylesheets can be embedded directly or referenced within your HTML:
    
    
    HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
    
    // Add a CSS stylesheet as <link type="text/css" rel="stylesheet" href="styles/test.css"/>
    builder.addCssStylesheet("styles/test.css");
      
    // Add a CSS stylesheet as <style type="text/css">
    builder.addCssStylesheet(new File("/path/to/file.css"));
    
    

* * *

[![Previous](images/prev.gif)](<WikiText-Developer-Guide.html> "Overview")[![WikiText-Developer-Guide](images/home.gif)](<WikiText-Developer-Guide.html> "WikiText-Developer-Guide")[![Next](images/next.gif)](<WikiText-and-the-UI.html> "WikiText and the UI")  
OverviewWikiText and the UI
