# Example Override of ApplicationDescriptors and ScopeDescriptors for JAX-RS Applications

This example assists with identifying and overriding OAuth 2 scopes related language keys 
for JAX-RS applications deployed to Liferay Portal.

As soon as the example module is deployed, all language keys are revealed via the ```OAuth 2 Administration``` portlet (on the Scopes tab of any OAuth 2 application).
They are rendered as ```languageKey=currentTranslation``` instead of ```currentTranslation```.

You can then modify the example module's ```/src/main/resources/content/Language.properties``` and corresponding (```Language_XX.properties``` files) to override these.

When you are happy with the overrides...
1. Update the service properties of ```com.example.oauth2.provider.scope.internal.spi.CustomApplicationScopeDescriptorsRegistrator``` to set dev.mode=false. 
2. Add an additional property ```osgi.jaxrs.name=X``` for each JAX-RS application that you want your overrides to take effect for. 
The correct names can be found in the system log because the OSGi component writes INFO statements describing them upon activation.

Please note that this example module is just that, an example, and the code is not supported by Liferay Inc. 
But nevertheless you may find it useful in your server environment because it allows managing all language keys 
from a single module.
