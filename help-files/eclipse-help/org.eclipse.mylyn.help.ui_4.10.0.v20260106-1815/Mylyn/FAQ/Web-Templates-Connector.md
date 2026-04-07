---
source: Web-Templates-Connector.html
original_path: org.eclipse.mylyn.help.ui_4.10.0.v20260106-1815/Mylyn/FAQ/Web-Templates-Connector.html
---

Web Templates Connector  
[ ![Previous](../../images/prev.gif) ](<Trac-Connector.html> "Trac Connector") [ ![Next](../../images/next.gif) ](<Task-Focused-UI.html> "Task-Focused UI")  
Trac Connector Task-Focused UI  
  
* * *

# Web Templates Connector

## Where can I find the Web Templates connector?

It is available from the incubator update site. Please see <http://www.eclipse.org/mylyn/downloads/> for a list of update sites. ( <http://www.eclipse.org/mylyn/downloads> is not an update site itself.) 

## Why can’t I connect using one of the existing templates?

The Web Connector have a “Preview” button in “Advanced Configuration” section in the “Query” preferences dialog. If you get an error or preview table does not show the expected list, you can use “Open” button to verify if web page downloaded by the Web Connector is correct. 

Alternatively, you can use Query Pattern like ({Description}.+?)({Id}\n) to see content of the retrieved page in preview table and compare it with the source of the web page for the same url loaded into the web browser.

If retrieved web page is correct, but “Preview” don’t show expected results you need to check the “Query Pattern”. A popular mistake is an unmasked ‘?’ that separates request parameters in the url). For example:
    
    
    <a href="show_bug.cgi\?id\=(.+?)">.+?<span class="summary">(.+?)</span>
    

If you don’t see the correct web page, check the following:

  * query URL or substituted parameters are correct 
    * **Note:** only the parameters are URL-encoded, e.g. the query URL `x:${y}` with the substituted parameter `${y}` set to `a:b` gets “`x:a%3Ab`” (only the substituted parameters are URL encoded 
  * web application require user to login and login configuration in the Web Connector repository configuration is correct (see Web Connector repository settings)


To debug web application auth further, you could use the wget tool (on Windows it available as a standalone command line tool and also included with Cygwin) and run the following command:
    
    
    wget -v -d -o query.log -O query.html "http://your.query.url"
    

If repository require basic http auth, you’ll need to use _\--http-user=USER_ and _\--http-password=PASS_ options. 

Then open file _query.html_ in the web browser and check if it show list of issues you need to fetch from the web server. If it does have correct list, then there could be a problem with query pattern used for parsing. 

If _query.html_ does not show expected list of issues you can check _query.log_ for server responses and try to identify if server require authentication, redirecting your requests somewhere or issue any other errors. 

## Known limitations

  * In Mylyn 1.0: if a web server responds by requesting a redirect to an absolute URL while fetching a resource, the connector falsely prepends “/” to the URL and the request will fail. The workaround is to use final url where redirect is pointing to (you can find it as describe above). See [bug 167282](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=167282>) for details. 


## Where can I find additional templates?

  * [Instructions for Google Code](<http://alblue.blogspot.com/2009/04/google-code-and-mylyn-redux.md>)


* * *

[ ![Previous](../../images/prev.gif) ](<Trac-Connector.html> "Trac Connector") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<Task-Focused-UI.html> "Task-Focused UI")  
Trac Connector Task-Focused UI
