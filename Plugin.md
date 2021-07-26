# Plugin indroduction
## Getting started
to start a plugin project, you need to import the whole dependences in libs,including current rukkit release.
## First Plugin
to start with this framework:

- src
    - MyPlugin.java
- plugin.yml

### plugin.yml
```yaml
author: author-name
version: 1.0.0
name: my-plugin
#Plugin API version to support.
apiVersion: 0.5.x
#Main Plugin Class to load.
pluginClass: MyPlugin
```

### MyPlugin.java
```java
public class MyPlugin extends RukkitPlugin {
    //Executes when PluginManager init
    @Override
    public void onStart() {
        //Do something...
    }
    //Executes when Server Started.
    @Override
    public void onDone() {
        //Do something...
    }
    //Executes when Server stopped.
    @Override
    public void onStop() {
        //Do something...
    }
}
```

Packed them to jar, drop jar into server/plugins folder.You might see this:
```txt
PluginManager::Loading my-plugin(v1.0.0)...
```
