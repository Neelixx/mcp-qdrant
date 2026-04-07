---
source: Trac-Connector.html
original_path: org.eclipse.mylyn.help.ui_4.10.0.v20260106-1815/Mylyn/FAQ/Trac-Connector.html
---

Trac Connector  
[ ![Previous](../../images/prev.gif) ](<JIRA-Connector.html> "JIRA Connector") [ ![Next](../../images/next.gif) ](<Web-Templates-Connector.html> "Web Templates Connector")  
JIRA Connector Web Templates Connector  
  
* * *

# Trac Connector

## What are the server requirements?

The Trac connector supports two different access methods: The recommended XML-RPC mode offers complete integration with Mylyn but requires additional setup and privileges which may not be available to all users. Web mode offers less functionality but will work with any public Trac repository. 

#### XML-RPC (recommended)

Requirements:

  * Trac 0.10 or later
  * [XmlRpcPlugin](<http://trac-hacks.org/wiki/XmlRpcPlugin>) rev. 1950 or later 


This access method offers editing of tasks in a rich editor, attachment support and offline editing. It requires the [XmlRpcPlugin](<http://trac-hacks.org/wiki/XmlRpcPlugin>) for Trac to be enabled for the accessed repository. The XmlRpcPlugin provides a remote interface to the Trac repository and is distributed separately from Trac (see [#217](<http://trac.edgewall.org/ticket/217>)). See these [install instructions](<http://trac-hacks.org/wiki/XmlRpcPlugin#Installation>) for requirements and documentation. 

In order to access the repository through XML_RPC each user needs to have the corresponding permission which can be configured through Trac's admin tool:
    
    
    trac-admin </path/to/projenv> permission add <user> XML_RPC
    

#### Web

Requirements:

  * Trac 0.9 or later


In this mode the standard Trac web interface is used for repository access. Tickets may be created and edited through a web browser. Offline editing and attachments are not supported.

## Recommended Trac version

Mylyn works with any stable Trac release that is version 0.9 or later (see below for known limitations). Mylyn works best when used with Trac’s XML-RPC Plugin (see above) but we do not recommend a particular Trac version. 

The Trac connector tests are run against these Trac versions:

  * Trac 0.11.6, XML-RPC Plugin 1.0.6 (r7194)
  * Trac 0.10.5, XML-RPC Plugin 0.0.2 (r2125)
  * Trac 0.9.6 (support ended with Mylyn 3.3)


## Does Mylyn support Trac 0.11?

When Mylyn 3.2.1 or later is used with the latest version of the [Trac XmlRpcPlugin](<http://trac-hacks.org/wiki/XmlRpcPlugin>) from [SVN trunk](<http://trac-hacks.org/svn/xmlrpcplugin/trunk/>) the Mylyn Trac connector is fully functional ( [bug 175211](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=175211>). 

## Why do I get an HTTP Error 500 Internal server error when creating a ticket that contains non-ASCII characters?

Problems related to character encodings have been reported when Trac is run with Python 2.3. Upgrading to Python 2.4 may help to resolve this. Please comment on [bug #188363](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=188363>) if you encounter an internal server error when creating a ticket that contains non-ASCII characters. 

## Known limitations

  * Trac 0.10.1 
    * Known incompatibility with Trac XML-RPC Plugin. See [bug 164272](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=164272>) for details. 
  * Trac 0.10.3 
    * Known incompatibility with old versions of the Trac HttpAuthPlugin (fixed in revision 1890 or later). The plug-in enables basic auth for XML-RPC when Trac AccountManagerPlugin for form based authentication is used. See [bug 168413](<https://bugs.eclipse.org/bugs/show_bug.cgi?id=168413>) for details. 
  * Trac 0.11 
    * Known incompatibility with XML-RPC Plugin version 1.0.2 - 1.0.5. Using 1.0.6 or later is recommended.


## Why are tasks opened in a web browser and not in the rich editor?

Please make sure that the access type in the repository properties is set to XML-RPC. This requires Trac 0.10 and XML-RPC (see above).

## Which URLs does Mylyn access in a Trac repository?

XML-RPC:

  * The expected URL is either /xmlrpc or /login/xmlrpc (if login credentials are provided)


Web:

  * Authentication: /login 
  * Querying: /query?format=tab…
  * Synchronizing ticket details: /ticket/…
  * Getting repository configuration to populate query dialog: /query or /newticket


The web mode relies on screen scraping and is likely to fail if the design (i.e. HTML output) of the Trac repository is heavily customized.

## Problems opening the web editor on Linux

If you’re having problems opening the web task editor on Linux and the message **Could not create Browser page: XPCOM error -2147221164** appears in the error log, try installing the packages **xulrunner** and **xulrunner-gnome-support** on your Linux distribution. 

## Which Trac Plugins are supported by Mylyn?

  * [XML-RPC Plugin](<http://trac-hacks.org/wiki/XmlRpcPlugin>) for rich-editing and attachment support 
  * [Account Manager Plugin](<http://trac-hacks.org/wiki/AccountManagerPlugin>) for form-based authentication (Mylyn 2.0 or higher) 
  * [Master Tickets Plugin](<http://trac-hacks.org/wiki/MasterTicketsPlugin>) for sub-task support (Mylyn 2.3 or higher) 


* * *

[ ![Previous](../../images/prev.gif) ](<JIRA-Connector.html> "JIRA Connector") [ ![Mylyn FAQ](../../images/home.gif) ](<FAQ.html> "Mylyn FAQ") [ ![Next](../../images/next.gif) ](<Web-Templates-Connector.html> "Web Templates Connector")  
JIRA Connector Web Templates Connector
