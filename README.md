tor-service-android
===================

This is a rewrite of the Orbot app's TorService class and its dependencies.
The package has been restructured and heavily reformatted so as to move in the direction of a library project.

Changes include:
+ Removing all dependencies on SharedPreferences.
+ Simplifying interactions with Tor via new TorController class.
+ Heavy refactoring of TorResourceInstaller - now all installation of binaries is handled in ResourceManager.
+ Sending logs, traffic info and status changes via new Broadcaster class.
+ Extracting common code that is not domain-specific into utility classes.

TODO:
+ Lots!
