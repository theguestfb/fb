# Fiction Branches

JAX-RS implementation of a collaborative interactive story. This is intended as a replacement to the current
engine at https://fictionbranches.net/

Fiction Branches is an online software engine which allows the production of multi-plotted stories. Each story
is divided into episodes comprising a single page of text. Each page will have a title, a link back to the
parent episode and the story text for that episode. At the end of each episode there can be one or more links
to subsequent episodes, plus the option to add another branch to the story at that point.

## Setup:

1. Install Java 8 (1.8) JDK

2. Clone repository (`git clone https://github.com/fictionbranches/fb`)

4. Choose a working directory for the backend. The default is /opt/fb. If you are on *nix (Linux, macOS, BSD, etc), you must either create this directory and make sure it is owned by your user, or use another directory. If you are on Windows, you will *have* to create a different directory, since "/opt/fb" is not a valid path. If you do not/cannot use /opt/fb, you must change the BASE_DIR in src/main/java/fb/Strings.java.

5. Copy the contents of optfb in the repository to your working directory

6. Modify domain.txt in the working directory to your domain (localhost:8080 should work here)

7. Set up postgres with database named fictionbranches, and a postgres roll named fictionbranches with access to it

8. Database
    - If you have raw scrape data, put it in ~/fbscrape and do `./gradlew run -PrunArgs='init'` to initiate a scrape. It will ask your for passwords for the root account and mine, the begin to import the raw scrape into the db. 
    - If you already have the db, name it fictionbranches in postgres and make sure the postgres roll fictionbranches can be logged in to with no password and has access to the db in postgres. 

9. `./gradlew run` to run the app.

10. `./gradlew jar` to build a runnable jar, including all dependencies
    - jar will be written to build/libs/fb.jar
    - run with `java -Xmx2048m -jar build/libs/fb.jar`

The backend is now running. It can be accessed at http://localhost:8080/fb 

Note that when you cleanly (using ctrl+c, SIGINT, etc (or whatever the Windows equivalents of those are) shut down the server, it will write transient data (active login sessions, unconfirmed new user accounts, and unconfirmed email address changes) to json files in ~/fbtemp. When the server starts, it will look for this directory and, if it finds it, read in the json files and delete the directory. 
