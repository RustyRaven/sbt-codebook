# sbt-codebook

## How to setup

In project/plugins.sbt

```
resolvers += "RustyRaven" at "http://rustyraven.github.io"

addSbtPlugin("com.rustyraven" % "sbt-codebook" % "1.0-SNAPSHOT")
```

And add 

```
enablePlugins(CodebookPlugin)
```

to your project.

## Commands
 
### generate

Generating the protocol server code.
It will automatically execute on compile time.

### document

Generating the protocol document.

### skeleton

Generating the api handler skeleton code.

### clientCode

Generating the protocol client code.

## Licence

MIT