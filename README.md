# WaybackAuthLib
Addon for Mojang's AuthLib to support Yggdrasil authentication again
### This library required the AuthLib to be installed as a dependency

## Why?
With the release of AuthLib 5.0, Mojang has removed all API methods to log in via the Yggdrasil Authentication system, as Yggdrasil has been officially terminated. However, many third-party log in methods use the Yggdrasil interface as a reference, this library is an addon for the AuthLib which adds all old methods back using the internals of the AuthLib to be as compact as possible.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/FlorianMichael/WaybackAuthLib/issues).  
If you just want to talk or need help with WaybackAuthLib feel free to join my
[Discord](https://discord.gg/BwWhCHUKDf).

## How to add this to your project
### Gradle/Maven
To use WaybackAuthLib with Gradle/Maven you can use this [Maven server](https://maven.lenni0451.net/#/releases/de/florianmichael/WaybackAuthLib) or [Jitpack](https://jitpack.io/#FlorianMichael/WaybackAuthLib).  
You can also find instructions how to implement it into your build script there.

### Jar File
If you just want the latest jar file you can download it from the GitHub [Actions](https://github.com/FlorianMichael/WaybackAuthLib/actions) or use the [Release](https://github.com/FlorianMichael/WaybackAuthLib/releases).

## Example usage
### Login with username and password / access token, log out and verify session
```java
final WaybackAuthLib authenticator = new WaybackAuthLib("<your auth host server>", clientToken, Proxy.NO_PROXY);

authenticator.setUsername(username);
authenticator.setPassword(password);

// You can also use authenticator.setAcessToken(), the logIn method will then refresh the acess token
// if it is expired (once you logged in using username/password, the access token field will also be updated automatically)
authenticator.logIn();

if (authenticator.isLoggedIn()) {
    final GameProfile profile = authenticator.getCurrentProfile();
    
    // Do something with the profile
}

final boolean isTokenValid = authenticator.checkTokenValidity();
if (isTokenValid) {
    // Do something
}

authenticator.logOut(); // This will invalidate the access token and reset all storages
```