---
source: Bugzilla-Connector.html
original_path: org.eclipse.mylyn.help.ui_4.10.0.v20260106-1815/Mylyn/FAQ/Bugzilla-Connector.html
---

Bugzilla Connector  
[ ![Previous](../../images/prev.gif) ](<Task-Repositories.html> "Task Repositories") [ ![Next](../../images/next.gif) ](<JIRA-Connector.html> "JIRA Connector")  
Task Repositories JIRA Connector  
  
* * *

# Bugzilla Connector

## What versions are supported?

  * We **recommend** using the latest [release](<http://www.bugzilla.org/download/>) of Bugzilla (3.4.x). 


  * Bugzilla 3.0.9 and higher is **officially supported**. 


  * Bugzilla 2.18.6 is the lowest version of Bugzilla the connector is **known to work with**. If you are getting errors that indicate failure to update attributes this may be due to use of an older Bugzilla. 


  * If you are using **Bugzilla 2.18** and are getting **mid-air collisions** this is likely due to incorrectly formatted timestamp field in the underlying bugs database table ( [bug 149513](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=149513>)). This can be resolved by upgrading to a more recent release of Bugzilla server. 


### Tips for server administrators

Mylyn periodically checks config.cgi to retrieve the repository configuration. On Eclipse.org this resulted in heavy CPU Load for the regeneration and a big surge in band width use.

  * This configuration seldom changes so can be cached and served from a file instead of being regenerated every time. While the size of this file tends to be small when hosting less than a dozen projects, it can be large on repositories holding large numbers of projects, e.g. 900K on bugs.eclipse.org.
  * Generated bugzilla output contains a lot of unnecessary whitespace which can be trimmed before caching. For bugs.eclipse.org this reduces the file to about _660K_.
  * The remaining file contains a lot of redundancy so can be gzipped for further reduction. On bugs.eclipse.org this leaves about _28K_ , a considerable saving. 


Mylyn has been modified to accept gzip encoding on all requests, and will do content negotiation. See [bug 205708](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=205708>). 

**To add caching for your Bugzilla repository:**

  * Change the name of config.cgi to config-stock.cgi and get the caching code in a new config.cgi.
  * The current version of this caching config.cgi script is hosted at:


    
    
     via CVS: :pserver:anonymous@dev.eclipse.org:/cvsroot/technology/org.eclipse.phoenix/infra-scripts/bugzilla/
     or: <http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.phoenix/infra-scripts/bugzilla/?root=Technology_Project>
    

  * Modifications on this script are followed on [bug 205416](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=205416>)


## Why are queries failing?

  * If **queries are not working** correctly ensure that you have the right Bugzilla server version selected for the corresponding repository: _Task Repositories (View) →_ right+click the repository _→ Properties_. For supported versions see the [download page](<http://www.eclipse.org/mylar/dl.php>). If the repository is still not functioning it may be due to authentication or connectivity problems. If that does not resolve the problem please post a message to the newsgroup or [submit a bug](<http://www.eclipse.org/mylar/bugs.php>). 


## Why do I see my old username?

If you change the username/email address on your Bugzilla account you may notice that some tasks still have the old username/email. Explicitly synchronize the task via the _Task List_ popup menu or _Task Editor_ toobar in order to update your username. 

## Why do tasks fail to open?

  * If upon opening a bug, the Bugzilla tab only displays a warning “' _Could not download task data, possibly due to timeout or connectivity problem. Please check connection and try again._ '”, ensure that your repository is returning XML data by pointing your browser to `<your-repository-url>/show_bug.cgi?id=1&ctype=xml` to show the contents of bug 1 in XML form. The Bugzilla client requires that bugs be accessible in XML form. If the repository doesn’t support xml then it is likely that the repository is too old (Mylyn currently supports Bugzilla 2.18 and later). 


  * If **reports fail to load** or **reports fail to synchronize** (task description remains _italic_), check the error log for a “' _File not found_ '” error pointing to `bugzilla.dtd` or a “' _Failed to retrieve products from server_ '” error message, these can result when 
    * the `urlbase` parameter is not set on the bugzilla server
    * your `urlbase` parameter is incorrect, e.g. it contains `index.cgi`
    * your bugzilla installation is missing the `bugzilla.dtd` file. See: [bug#161759](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=161759>)


  * If **attributes are missing options** in the bug editor (ie. missing a recently added milestone, [bug# 155431](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=155431>)): 
    * Update attributes by choosing _Update Attributes_ from the context menu in the _Task Repositories_ view 
    * Synchronize the task in the Task List
    * Reopen the task - new options should be available.


## Why do tasks fail to submit?

  * **Error upon submit: unable to make any match for name/email entered.** If your bugzilla is configured for user names rather than full email address the QA Contact field will cause the submit to fail ( [bug# 166555](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=166555>)). To resolve, select ‘Local users enabled’ option on the Repository Configuration page. 


  * If tasks **fail to submit** with credentials error, but the repository validates fine, make sure that you’ve correctly setup your account’s username and password and are not using the HTTP authentication fields instead. 
    * Click on the "Submit disabled, please check Credentials and refresh" error, which is a link to the Task Repository page for Bugzilla, for example. Also accessible via the "Bugzilla" link at the top of the task page. Removing and re-adding the password for your User ID may help remove this error.


  * If tasks **fail to submit** with credentials error, but the repository validates fine, make sure that you’ve correctly setup your bugzilla instance’s cookie domain under the required settings. The cookie domain must lead with a dot. 


  * If **attachments are failing to submit** and you see that after processing the attachment, the bugzilla bug shows a size of “bytes” (no numbers), your database may be dropping the packet sending the file. **On MySQL** , check the [max_allowed_packet directive](<http://dev.mysql.com/doc/refman/5.0/en/packet-too-large.md>). You may see errors like: `DBD::mysql::st execute` failed: Got a packet bigger than 'max_allowed_packet' bytes [for Statement “INSERT INTO attach_data (id, thedata) VALUES (38, ?)”] at `/path/to/bugzilla/attachment.cgi` line 993. Also, **check the maximum attachment size** in _Bugzilla Parameters → Attachments_. 


  * If a submit fails with an **Invalid Username or Password** error even though repository settings do validate, make sure that cookies could be set. See also [bug 175502](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=175502>)


## What time zone is used in the task editor?

  * The **times** that appear in the Bugzilla bug editor (ie. created, modified) are in your local machine’s time zone. 


## Known limitations

  * The `usermatchmode` is not currently supported ( [bug 168204](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=168204>)) and as such full email addresses need to be used. 


  * The `usevisibilitygroups` parameter is not supported ( [bug#180876](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=180876>)). Group assignment will currently be lost if updated using the Bugzilla rich task editor. 


  * Version 1.0 of Mylyn presents an error message upon new comment submission to Red Hat’s public Bugzilla repository. The comment is in fact posted. This issue is resolved in Mylyn 2.0M2 and higher. ( [bug#183251](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=183251>)) 


  * The server setting "Timezone used to display dates and times" must be set to "Same as Server," otherwise you will get a security token error when performing any submit operation other than submitting a new bug. This may also manifest as a midair collision. ( [bug#429523](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=429523>)) 


* * *

[ ![Previous](../../images/prev.gif) ](<Task-Repositories.html> "Task Repositories") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<JIRA-Connector.html> "JIRA Connector")  
Task Repositories JIRA Connector
