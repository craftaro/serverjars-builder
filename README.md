# What's this?
This is an OpenSource project for [ServerJars](https://serverjars.com/) that will be in charge of building **every single jar** available in our site.

# How does it work?!?
Magic ✨...<br/>
Actually it's just a bunch of Kotlin code (don't tell anyone, but I think I'm in love with Kotlin... but don't tell Swift!!).<br/>
You're free to check out our code! But if you use it make sure to take a look to our license!

# How can I help?
Feel free to clone this repo and make some changes, then open a PR and done! Someone from the team will check it out and if it meets our ✨High standards✨ (actually, it just needs to work and be pretty :stuck_out_tongue_winking_eye:) we'll merge it!

# How can I run it?
You can run it locally, but you'll need to have Docker installed and running.<br/>
Then, just run `./gradlew run` and it should work! I hope so... Hmm, it seems it prints just a help page, welp, I guess if you're reading this... you can also READ THE HELP PAGE! Sorry for screaming, I just put a lot of work in that help page, actually, it was GitHub Copilot (_thanks copilot_)

# How do I compile it?
Just run `./gradlew shadowJar` and it should just export a jar file to `build/libs/ServerJars.jar`.<br/>
Then you can run it with `java -jar ServerJars.jar` and it should work! Probably, if not your computer will explode in 5..4..3..2..1.. 💥.... Hmm, didn't work, I guess it just runs the jar file :sweat_smile:<br/>