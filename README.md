# synchrotron

Server-side clojurescript utils for interacting with the [SuperCollider server](https://supercollider.github.io/).

## Overview

While [overtone](http://overtone.github.io/) exists for writing SuperCollider applications in Clojure, the startup time for a JVM-based Clojure process is too slow to make this an appealing option on the Raspberry Pi.

Alternatively, I'm writing clojurescript and targetting nodejs for the runtime. Synchrotron aims to provide functionality that I need from SuperCollider, but will likely never achieve parity with overtone or sclang.

Some hypothetical features for synchrotron to eventually have include:
* Spawning and communicating to an scsynth process
* Running OSC Server Commands
* Parsing binary synthdef files to edn and translating edn back into binary synthdef format
* DSL for defining synthdefs
* Scheduling - Some kind of logical time scheduling model, maybe relying on scsynth for precise timing using OSC timetags 
* MIDI handling?
* Signal flow graph model for connecting objects (and schedulers? and MIDI?)

## Dev Setup

How I'm working with this right now:

1. SuperCollider must be installed
2. Open emacs and jump to a file in the synchrotron project
3. Run `cider-jack-in-clojurescript`
4. When it prompts you to Select ClojureScript REPL type for `cider-connect-sibling-cljs`, choose `node`
5. Wait a couple seconds, and you should be connected

## License

Copyright © 2018 Brian Fay / Licensed under the MIT license
