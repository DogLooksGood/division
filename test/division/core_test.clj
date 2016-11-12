(ns division.core-test
  (:require [division.core :as sut]
            [clojure.test :as t]))

;; -----------------------------------------------------------------------------
;; Pipeline functions for testing.
;; -----------------------------------------------------------------------------

(defn view
  [& {:keys [radius]
      :or   {radius 0}}]
  (fn [style]
    (assoc style :border-radius radius)))

(defn striped!
  [& {:keys [odd even]}]
  (fn [style]
    (let [odd-style (merge style odd)
          even-style (merge style even)]
      [odd-style even-style])))

(defn list!
  []
  (fn [style]
    (let [container (dissoc style
                            :background-color
                            :color)
          item      {}]
      [container item])))

;; -----------------------------------------------------------------------------
;; Test cases
;; -----------------------------------------------------------------------------

(t/deftest test-parse-pipe-item
  (t/testing "Parse a form will return the eval result of form."
    (t/is (= ((sut/parse-pipe-item (list view :radius 3) {})
              {})
             {:border-radius 3})))

  (t/testing "Parse a function will return the function call with no args."
    (t/is (= ((sut/parse-pipe-item view {})
              {})
             {:border-radius 0})))

  (t/testing "Parse a map will return a function that merge this map."
    (t/is (= ((sut/parse-pipe-item {:color "red"} {})
              {:background-color "black"})
             {:color "red", :background-color "black"})))

  (t/testing "Parse a keyword will return a function that merge the correspond style in stylesheet"
    (t/is (= ((sut/parse-pipe-item :view {:view {:color "red"}})
              {:background-color "black"})
             {:color "red", :background-color "black"}))))

(t/deftest test-extract-target-style
  (t/testing "Extract single map, assign to a keyword."
    (t/is (= (sut/extract-target-style :view {:color "blue"})
             {:view {:color "blue"}})))

  (t/testing "Extract vector of maps, assign to a vector of keywords."
    (t/is (= (sut/extract-target-style [:odd :even]
                                       [{:color "blue"} {:color "red"}])
             {:odd {:color "blue"} :even {:color "red"}})))

  (t/testing "Extract nested vector of maps, assign to nested vector of keywords."
    (t/is (= (sut/extract-target-style [:container [:odd :even]]
                                       [{:width  "500px"
                                         :height "100px"} [{:color "blue"}
                                                           {:color "red"}]])
             {:container {:width  "500px"
                          :height "100px"}
              :odd       {:color "blue"}
              :even      {:color "red"}}))))

(t/deftest test-defstyles
  (t/testing "Define a single key stylesheet."
    (t/is (= {:view {:border-radius 3}}
             (sut/styles
              :view
              [(view :radius 3)]))))

  (t/testing "Define a stylesheet with a vector of keys."
    (t/is (= {:container {:border-radius 3}
              :item      {}}
             (sut/styles
              [:container :item]
              [(view :radius 3) list!]))))

  (t/testing "Define a stylesheet with nested vectors of keys."
    (t/is (= {:container {:border-radius 5}
              :odd-item  {:color "blue"}
              :even-item {:color "red"}}
             (sut/styles
              [:container [:odd-item :even-item]]
              [(view :radius 5) list! (striped! :odd {:color "blue"}
                                                :even {:color "red"})]))))

  (t/testing "Define a stylesheet with multiple expressions."
    (t/is (= {:view      {:border-radius 3}
              :container {:width "200px", :height "100px", :border-radius 3}
              :item      {:color "white"}}
             (sut/styles
              :view
              [(view :radius 3)]
              [:container :item]
              [:view {:width "200px", :height "100px"}
               list! {:color "white"}])))))
              

