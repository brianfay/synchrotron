;;after running "clj -A:nrepl start-nrepl.clj", you can cider-connect to localhost, and then eval this code
(do (require 'cljs.repl.node)
    (cider.piggieback/cljs-repl (cljs.repl.node/repl-env :path ["/home/bfay/devel/synchrotron/node_modules"])
                                :npm-deps {"osc-min" "1.1.1"}
                                :install-deps true))
