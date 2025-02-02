(ns stencil.model.relations
  (:require [clojure.data.xml :as xml]
            [clojure.data.xml.pu-map :as pu]
            [clojure.walk :refer [postwalk]]
            [clojure.java.io :as io :refer [file]]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer :all]
            [stencil.model.common :refer :all]))

(def tag-relationships
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationships)

(def tag-relationship
  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fpackage%2F2006%2Frelationships/Relationship)

(def rel-type-hyperlink
  "Relationship type of hyperlinks in .rels files."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink")

(def rel-type-image
  "Relationship type of image files in .rels files."
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image")

(defn unlazy
  [coll]
  (let [unlazy-item (fn [item]
                      (cond
                        (sequential? item) (vec item)
                        (map? item) (into {} item)
                        :else item))
        result (postwalk unlazy-item coll)]
    result))

(defn parse [rel-file]
  (with-open [reader (io/input-stream (file rel-file))]
    (let [lazy-vals (xml/parse reader)
          parsed (unlazy lazy-vals)]
      (assert (= tag-relationships (:tag parsed))
              (str "Unexpected tag: " (:tag parsed)))
      (into (sorted-map)
            (for [d (:content parsed)
                  :when (map? d)
                  :when (= tag-relationship (:tag d))]
              [(:Id (:attrs d)) {:stencil.model/type   (doto (:Type (:attrs d)) assert)
                                 :stencil.model/target (doto (:Target (:attrs d)) assert)
                                 :stencil.model/mode   (:TargetMode (:attrs d))}])))))


(defn writer [relation-map]
  (assert (map? relation-map))
  (assert (every? string? (keys relation-map)) (str "Not all str: " (keys relation-map)))
  (->
   {:tag tag-relationships
    :content (for [[k v] relation-map]
               {:tag tag-relationship
                :attrs (cond-> {:Type (:stencil.model/type v), :Target (:stencil.model/target v), :Id k}
                         (:stencil.model/mode v) (assoc :TargetMode (:stencil.model/mode v)))})}
   ;; LibreOffice opens the generated document only when default xml namespace is the following:
   (with-meta {:clojure.data.xml/nss
               (pu/assoc pu/EMPTY "" "http://schemas.openxmlformats.org/package/2006/relationships")})
   (->xml-writer)))


(defn- map-rename-relation-ids [item id-rename]
  (-> item
      ;; Image relation ids are being renamed here.
      (update-some [:attrs ooxml/r-embed] id-rename)
      ;; Hyperlink relation ids are being renamed here
      (update-some [:attrs ooxml/r-id] id-rename)))


(defn xml-rename-relation-ids [id-rename xml-tree]
  (if (map? xml-tree)
    (-> xml-tree
        (map-rename-relation-ids id-rename)
        (update :content (partial map (partial xml-rename-relation-ids id-rename))))
    xml-tree))


;; generates a random relation id
(defn- ->relation-id [] (str (gensym "stencilRelId")))


(defn ids-rename [model fragment-name]
  (doall
   (for [[old-rel-id m] (-> model :main :relations :parsed (doto assert))
         :when (#{rel-type-image rel-type-hyperlink} (:stencil.model/type m))
         :let [new-id       (->relation-id)
               new-path     (if (= "External" (:stencil.model/mode m))
                              (:stencil.model/target m)
                              (str new-id "." (last (.split (str (:stencil.model/target m)) "\\."))))]]
     {:stencil.model/type       (:stencil.model/type m)
      :stencil.model/mode       (:stencil.model/mode m)
      :stencil.model/target     new-path
      :fragment-name fragment-name
      :new-id      new-id
      :old-id      old-rel-id
      :source-file (file (-> model :main :source-file file .getParentFile) (:stencil.model/target m))
      :stencil.model/path       new-path})))
