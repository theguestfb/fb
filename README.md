# Fiction Branches

JAX-RS implementation of a collaborative interactive story. This is intended as a replacement to the current
engine at https://fictionbranches.net/

Fiction Branches is an online software engine which allows the production of multi-plotted stories. Each story
is divided into episodes comprising a single page of text. Each page will have a title, a link back to the
parent episode and the story text for that episode. At the end of each episode there can be one or more links
to subsequent episodes, plus the option to add another branch to the story at that point.

How to get started:

## tl;dr

Configure Eclipse with Gradle and Eclipse WTP, add a Tomcat server, and run the project on the server.

## Environment Setup

1. Install Eclipse (recommended to use latest Eclipse Oxygen release)
    - Also check for updates, Help > Check for Updates

2. Help > Install New Software > http://download.eclipse.org/releases/oxygen (oxygen=release)
  - Under *Web, XML, Java EE Developmentand OSGi Enterprise Development*, select
    - Eclipse Java EE Developer Tools
    - Eclipse Java Web Developer Tools
    - Eclipse Web Developer Tools
    - JST Server Adapters
    - JST Server Adapters Extensions

3. Help > Eclipse Marketplace
    - Search for buildship
    - Install/Update Buildship Gradle Integration 2.0

4. File > Import > Git > Projects from Git > Next
    - Clone URI > Next
    - Set URI: to
    - Next > Next
    - Change destination directory if desired > Next > Next > Finish

5. Wait for gradle to sync/build/etc

6. Window > Show View > Other > Gradle > Gradle Tasks

7. Gradle Tasks > fb > ide > eclipseWtp (double click)

8. Download Apache Tomcat 8.5(.23) from http://supergsego.com/apache/tomcat/tomcat-8/v8.5.23/bin/apache-tomcat-8.5.23.zip
    - Unzip this somewhere (I usually use the Eclipse workspace (NOT THE PROJECT FOLDER)).
    - The server is actually using Tomcat8, but it shouldn't really matter what version is used as long as it's recent

9. Window (Eclipse on macOS) > Preferences > Server > Runtime Environments > Add
    - Apache > Apache Tomcat v8.5 > Create a new local server > Next
    - Browse > browse to wherever you unzipped Tomcat from step 8 > Open
    - Finish > Apply and Close

10. Choose a working directory for the backend. The default is /opt/fb. If you are on *nix (Linux, macOS, BSD, etc), you must either create this directory and make sure it is owned by your user, or use another directory. If you are on Windows, you will *have* to create a different directory, since "/opt/fb" is not a valid path. If you do not/cannot use /opt/fb, you must change the directory in:
    - src/main/java/fb/Strings.java, change BASE_DIR to the directory
    - src/main/resources/hibernate.cfg.xml, change "jdbc:h2:/opt/fb/storydb" to "jdbc:h2:/path/to/dir/storydb" (or "jdbc:h2:C:\path\to\dir\storydb" on Windows)

11. Copy the contents of optfb in the repository to your working directory

12. Move database file (storydb.mv.db) into the working directory 

13. Modify domain.txt in the working directory to your domain (localhost:8080 should work here)

14. Right click on project in Package Explorer > Run As > Run on Server
    - (Optional) Check Always use this server when running this project
    - Finish

The backend is now running. It can be accessed at http://localhost:8080/fb 
