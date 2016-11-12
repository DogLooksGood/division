(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.2" :scope "test"]])

(require '[adzerk.boot-test :refer [test]])

(deftask run-test
  []
  (set-env! :source-paths
            #(conj % "test"))
  (comp (watch)
        (speak)
        (test)))

