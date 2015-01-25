tor-service-android
===================

This is a big reformat of the Orbot app's service/ package

WARNING: This is alpha software and is either unstable or not functional at all!

DONE:
+ Removing all dependencies on SharedPreferences.
+ Simplifying interactions with Tor via new TorController class.
+ Heavy refactoring of TorResourceInstaller - now all installation of binaries is handled in ResourceManager.
+ Sending logs, traffic info and status changes via new Broadcaster class.
+ Extracting common code that is not domain-specific into utility classes.

TODO:
+ Transparent proxying (almost!)
+ VPN
+ Real binary resources, currently faking them in ResourceManager.
+ Handling sockets/ports in TorController.
+ Notifications, and how they're managed.
+ Moving jtorctl into libs/ - dependency structure isn't great at the moment.
+ Lots more, deal with all the TODOs in code.
