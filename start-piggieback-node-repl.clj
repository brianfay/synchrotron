;;after running "clj -A:nrepl start-nrepl.clj", you can cider-connect to localhost, and then eval this code
(do (require 'cider.piggieback)
    (require 'cljs-remote-node-repl)
    (cider.piggieback/cljs-repl (cljs-remote-node-repl/repl-env :host "192.168.0.106" :port 5001)))
