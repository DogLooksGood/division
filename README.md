# Division

*Work in Progress*.

Chain-style stylesheet generator for CLJSRN and CSS for Clojure.

# Usage

```clojure
(defstyles awesome-style
  :view
  [{:width \"500px\", :height \"100px\"} view]  
  :round-view
  [:view (radius :percent 50)]
  [:list-container [:odd-item :even-item]]
  [view list! (striped! :odd  {:color \"blue\"}
                        :even {:color \"red\"})])
```





