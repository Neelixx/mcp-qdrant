---
source: WikiText-and-the-UI.html
original_path: org.eclipse.mylyn.wikitext.help.ui_4.9.0.v20251119-1306/help/devguide/WikiText-and-the-UI.html
---

WikiText and the UI  
[![Previous](images/prev.gif)](<Using-The-WikiText-Parser.html> "Using The WikiText Parser")[![Next](images/next.gif)](<Markup-Languages.html> "Markup Languages")  
Using The WikiText ParserMarkup Languages  
  
* * *

# WikiText and the UI

WikiText can be used to integrate wiki markup capabilities into your application user interface.

## WikiText JFace Viewer

WikiText provides a JFace `SourceViewer` for displaying wiki markup without having to use a `Browser` widget. 
    
    
    MarkupViewer viewer = new MarkupViewer(parent, null, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    viewer.setMarkupLanguage(markupLanguage);
    
    MarkupViewerConfiguration configuration = new MarkupViewerConfiguration(viewer);
    viewer.configure(configuration);
    viewer.getTextWidget().setEditable(false);
    viewer.setMarkup(markup);
    
    

## WikiText and Browser

WikiText can be used to display wiki markup in an SWT `Browser` widget:
    
    
    Browser browser = new Browser(parent, SWT.NONE);
    
    MarkupParser markupParser = new MarkupParser();
    markupParser.setMarkupLanguage(new TextileLanguage());
    String htmlContent = markupParser.parseToHtml(markupContent);
    		
    browser.setText(htmlContent);
    
    

* * *

[![Previous](images/prev.gif)](<Using-The-WikiText-Parser.html> "Using The WikiText Parser")[![WikiText-Developer-Guide](images/home.gif)](<WikiText-Developer-Guide.html> "WikiText-Developer-Guide")[![Next](images/next.gif)](<Markup-Languages.html> "Markup Languages")  
Using The WikiText ParserMarkup Languages
