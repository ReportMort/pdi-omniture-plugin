# pdi-omniture-plugin
A Kettle Plugin for Omniture

## Installation Instructions

1. Locate the Kettle Plugins Folder associated with your Spoon or DI Server installation. The "plugins" folder is typically located at a path similar to: pdi/data-integration/plugins
2. Create a folder inside your plugins folder called "pdi-omniture-plugin".
3. Copy and paste into the new folder the pdi-omniture-plugin.jar file, which is located inside this repository's /dist/steps/pdi-omniture-plugin folder
4. Create another folder inside of it called "lib" where you will put any supporting jars
5. Copy and paste all of the .jar files inside this repository's /lib folder
6. You may need to restart Spoon or DI Server before it is visible under the available design nodes

The final folder structure should be:

```
+-- pdi/data-integrations/plugins/pdi-omniture-plugin
|   +-- pdi-omniture-plugin.jar
|   +-- lib
|       +-- ...All .jar files in /lib folder
```