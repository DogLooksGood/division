(ns division.core
  "
  Chain-style stylesheet generator for CLJSRN or CSS.

  Usage:
  (defstyles awesome-style
    :view
    [{:width \"500px\", :height \"100px\"} view]  

    :round-view
    [:view (radius :percent 50)]

    [:list-container [:odd-item :even-item]]
    [view list! (striped! :odd  {:color \"blue\"}
                          :even {:color \"red\"})])
  ")

(defn take-style
  "Take style from stylesheet by the key of style.
  Throw an error when style not found."
  [stylesheet style-key]
  (if-let [style (get stylesheet style-key)]
    style
    (throw (ex-info
            (str "No style for key: " style-key)
            {:error      :take-style-failed
             :style-key  style-key
             :stylesheet stylesheet}))))

(defn parse-pipe-item
  "Parse pipeline item into pipeline function.
  Pipeline function will take one argument, the style, return new style(s).

  A pipe item can be one of the following:
     1. A form for function call.
        e.g. (panel :radius 5)
     2. A function return function.
        e.g. panel, this is the shortcut for (panel).
     3. A map.
        e.g. {:color \"blue\"}.
        this is the shortcut for #(merge % {:color \"blue\"})
     4. A keyword.
        A reference for an defined style in current stylesheet."
  [pipe-item stylesheet]
  (cond
    (list? pipe-item)    (eval pipe-item)
    (fn? pipe-item)      (pipe-item)
    (symbol? pipe-item)  ((var-get (resolve pipe-item)))
    (map? pipe-item)     #(merge % pipe-item)
    (keyword? pipe-item) #(merge % (take-style stylesheet pipe-item))
    :else                (throw (ex-info (str "Unsupport pipe-item type: " (type pipe-item))
                                         {:error     :invalid-pipe-item
                                          :pipe-item pipe-item}))))

(defn apply-pipe-fn
  "Apply pipe function on a style(or styles).
  Will return the new style(or styles)."
  [styles pipe-fn]
  (if (map? styles)
    (pipe-fn styles)
    (let [style     (peek styles)
          new-style (apply-pipe-fn style pipe-fn)]
      (into [] (concat (drop-last styles)
                       (list new-style))))))

(defn extract-target-style
  ([target-exp raw-styles]
   (extract-target-style target-exp raw-styles {}))
  ([target-exp raw-styles stylesheet]
   (cond
     (and (keyword? target-exp) (map? raw-styles))
     (assoc stylesheet target-exp raw-styles)

     (and (vector? target-exp) (vector? raw-styles))
     (apply merge
            stylesheet
            (map extract-target-style target-exp raw-styles))

     :else
     (throw (ex-info "Unmatched generated styles and target expression."
                     {:target target-exp
                      :styles raw-styles})))))     

(defn parse-exp
  "Generate styles by apply pipeline to an empty map.
  Then assign the result to targets.
  Return the new stylesheet that merged the old one."
  [target-exp pipe-items stylesheet]
  (let [parse-pipe-item* (fn [p] (parse-pipe-item p stylesheet))
        pipe-fns         (map parse-pipe-item* pipe-items)
        raw-styles       (reduce apply-pipe-fn {} pipe-fns)
        new-stylesheet   (extract-target-style target-exp raw-styles)]
    (merge stylesheet new-stylesheet)))

(defmacro styles [& clauses]
  (let [exps       (partition 2 clauses)
        stylesheet (reduce (fn [stylesheet [target-exp pipe-items]]
                             (parse-exp target-exp pipe-items stylesheet))
                           {} exps)]
    stylesheet))

(defmacro defstyles
  "Create stylesheet."
  [name & clauses]
  `(def ^:const ~name (styles ~@clauses)))






