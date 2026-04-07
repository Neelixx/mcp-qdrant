---
source: Textile-Syntax.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.10.0.v20260106-1815/help/Textile-Syntax.html
---

Textile Syntax  
[![Previous](images/prev.gif)](<Markup-Generation.html> "Markup Generation")[![Next](images/next.gif)](<Tips-and-Tricks.html> "Tips and Tricks")  
Markup GenerationTips and Tricks  
  
* * *

# Textile Syntax

The WikiText editor supports most standard Textile markup. In addition some markup extensions are supported. See the markup 'cheat-sheet' for details (press **F1** in the editor).

## Textile Syntax Tips

### Whitespace

Textile markup is sensitive to whitespace. For example, a line that starts with '`h1.`' is only a heading if the '`h1.`' is immediately followed by a space character. This can bite you if you're not careful!

A less obvious example of the same problem is with '`bc.`' Usually content in a 'block code' section is on the line following the '`bc.`' If the '`bc.`' is not immediately followed by a space character _before the end of the line_ , then the area is simply considered a normal paragraph.

### HTML Literals

Textile markup can handle literal HTML. For example, the following is valid Textile:
    
    
    some <b>bold text</b> here
    

Care must be taken with literal HTML: the start tag must be completed on one line, and the end tag must be completed on one line. The following is an example of embedded HTML that won't work with Textile:
    
    
    here is <a
    href="#">a bad example</a>
    

The above example can be fixed by moving the `href` up on to the same line as the `<a` portion:
    
    
    here is <a href="#">a working example</a>
    

### Images and DocBook

DocBook is quite flexible about how images are handled. This section discusses how DocBook rendering can be altered with the use of specific markup.

#### Inline Images v.s. Block Images

When handling images in a conversion of Textile markup to DocBook markup, there are several choices for the resulting DocBook markup. By default images are converted as follows:
    
    
    !images/foo.png!
    

results in:
    
    
    <mediaobject><imageobject><imagedata fileref="images/foo.png"/></imageobject></mediaobject>
    

`<mediaobject/>` is useful for some cases, however there are times when `<inlinemediaobject/>` should be used instead. To achieve this result, add the `inline` class to your image as follows:
    
    
    !(inline)images/foo.png!
    

The conversion will then result in the following:
    
    
    <inlinemediaobject><imageobject><imagedata fileref="images/foo.png"/></imageobject></inlinemediaobject>
    

#### Image Scaling

DocBook supports image scaling with the `scale` attribute. For example:
    
    
    <imagedata fileref="images/foo.png" scale="80"/>
    

To achieve this effect with Textile markup use syntax as follows:
    
    
    !{width:80%}images/foo.png!
    

More information about image scaling in DocBook is available at [imagedata: Scaling](<http://www.docbook.org/tdg/en/html/imagedata-x.md>).

#### Image Size

Image size can be specified in DocBook using `width` and `depth` attributes for width and height, respectively:
    
    
    <imagedata fileref="images/foo.png" width="32px" height="64px"/>
    

This is achieved with Textile markup as follows:
    
    
    !{width:32px;height:64px}images/foo.png!
    

### Extended Blocks

Textile extended blocks (starting with `bc..`, `pre..` and `bq..`) are useful for blocks that may have multiple whitespace lines. Extended blocks must be terminated by an explicit Textile block (such as `p.`). For example:
    
    
     bc..
     a block code section has started
    
     and continues
    
     p. a paragraph starts
    
    
    

## Textile Extensions

Mylyn WikiText adds the following extensions to Textile:

  * `{toc}` \- emit a table of contents. Parameters may be specified as follows:
    * `{toc:style=circle}` Emit with list-style=circle
    * `{toc:maxLevel=2}` Emit with heading levels up to and including h2.
    * `{toc:class=tableOfContents}` Emit with CSS class of "tableOfContents". Defaults to "toc" if unspecified.
  * `{glossary}` \- emit a glossary of terms in a definition list. Glossary terms are specified using Textile syntax for acronyms.


## Examples

This document was written in Textile markup. The original source code for this document [is available here](<https://git.eclipse.org/c/mylyn/org.eclipse.mylyn.docs.git/tree/wikitext/ui/org.eclipse.mylyn.wikitext.help.ui/help/Mylyn-WikiText-User-Guide.textile>)

## Textile Reference

Textile syntax and references can be found here:

  * [Textile (markup language)](<http://en.wikipedia.org/wiki/Textile_%28markup_language%29>) (wikipedia.org)
  * [TxStyle · Textile Documentation](<https://txstyle.org/>) an online form for trying out Textile syntax (txstyle.org)
  * [Textile Reference](<http://redcloth.org/hobix.com/textile/>) an example-based Textile reference (hobix.com)


* * *

[![Previous](images/prev.gif)](<Markup-Generation.html> "Markup Generation")[![Mylyn-WikiText-User-Guide](images/home.gif)](<Mylyn-WikiText-User-Guide.html> "Mylyn-WikiText-User-Guide")[![Next](images/next.gif)](<Tips-and-Tricks.html> "Tips and Tricks")  
Markup GenerationTips and Tricks
