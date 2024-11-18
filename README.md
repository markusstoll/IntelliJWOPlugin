# IntelliJWOPlugin

Eclipse with the WOLips plugin was the default IDE for WebObjects and Wonder applications for the last 10 years and still is.
However since I migrated my applications to maven projects debugging using Eclipse became painful.
My impression is that the issues are more Eclipse than WOLips related, so I am not hoping for improvement quite soon.

So I started this experimental plugin for IntelliJ. My first approach is a working run configuration so I can finally debug my applications again.

This first version supplies a working run configuration that starts your WebObjects application with a default set of WO-Parameters (e. g. "-WOPort -1"). My next step is trying to add a UI component for these parameters in the run configuration.


## Restrictions

This plugin will only work with mavenized WebObjects/Wonder applications!

## Installation

* Obtain the jar from releases and store on your disk
* Open "Settings... in IntelliJ, choose "Plugins" and "Install plugin from disk..."
* Choose the plugin jar from above
* You will be asked to restart the IDE to activate

After that the plugin is available. 
For running your application, create a WebObjects run configuration, choose your main class and go for it!

## Creating a Run Configuration

Create a new run configuration from the Templates, choose "WebObjects" template

Things work slightly different than using Eclipse:

* The run configuration detects whether you have full maven layout or
  a mavenized fluffy bunny layout. For full maven layout, the `nature` `org.maven.ide.eclipse.maven2Nature` is enforced in the `.project` file.
  Otherwise it enforces the `nature` not being set
* The working directory is automatically set to `./target/<appname>.woa`
* A maven goal `process-resources` is enforced on starting your WO application.

Additionally if starting with a JDK version higher than 8, these additional VM options are enforced:
* `--add-exports=java.base/sun.security.action=ALL-UNNAMED`
* `--add-exports=java.base/sun.util.calendar=ALL-UNNAMED`
* `--add-opens=java.base/java.lang=ALL-UNNAMED`

A future version will have a UI editor for these options, too

## Feature list
* Run Configuration 
* New WOComponent dialog

## TODO list / Next steps
* add UI editor for additional higher JDK VM options
* add shortcuts for switching between Java and HTML file for a WO component
* add new project wizard
* validator for wo components html content
* validator for wo components wod files
* start experimenting with custom editor for .wo folders
** add hooks for delete wo component (delete associated files)
** add hook for renaming
** create customer editor with tabs for .woo / .api
