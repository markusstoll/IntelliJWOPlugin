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



