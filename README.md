# synchrotron

Server-side clojurescript utils for interacting with the [SuperCollider server](https://supercollider.github.io/).

## Overview

SuperCollider already has its own programming language called `sclang`, but it allows for interaction with its server from other languages.

I enjoy programming in Clojure, and while there's already [a great solution](http://overtone.github.io/) for programming SuperCollider in Clojure.

However, the ability to run on a Raspberry Pi is important to me. From my experience the startup time for a JVM-based Clojure process is miserable on a Raspberry Pi. Nodejs, on the other-hand, starts in milliseconds.

Some hypothetical features for synchrotron to eventually have are:
* Spawning and communicating to an scsynth process
* Running OSC Server Commands
* Parsing binary synthdef files to edn and translating edn back into binary synthdef format
* DSL for defining synthdefs
* Scheduling - Some kind of logical time scheduling model, maybe relying on scsynth for precise timing using OSC timetags 
* MIDI handling?
* Signal flow graph model for connecting objects (and schedulers? and MIDI?)

## Dev Setup

I'm using figwheel and emacs (w/ piggieback so I can eval code inline) for my development workflow.

It's really great except for sometimes when it's not, and when you have to set it up.

1. SuperCollider must be installed
2. Go to the synchrotron project in a terminal
3. Run `lein deps` (should install the npm dependencies)
4. Open emacs and jump to a file in the synchrotron project
5. Run `cider-jack-in-clojurescript`
6. Open up the repl buffer and run this: 
```
(do (require 'figwheel-sidecar.repl-api)
  (figwheel-sidecar.repl-api/start-figwheel!)
  (figwheel-sidecar.repl-api/cljs-repl))
```
7. Run `node out/synchrotron.js`. The node process should connect to figwheel and you should be able to begin developing interactively.

## License

Copyright © 2017 Brian Fay / Licensed under the MIT license
