(ns stencil.model.numbering
  (:import [java.io File])
  (:require [clojure.data.xml :as xml]
            [clojure.walk :refer [postwalk]]
            [clojure.java.io :as io]
            [stencil.ooxml :as ooxml]
            [stencil.util :refer :all]
            [stencil.model.common :refer :all]))


(def ^:private rel-type-numbering
  "http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering")


(def ^:dynamic *numbering* nil)


(defn unlazy
  [coll]
  (let [unlazy-item (fn [item]
                      (cond
                        (sequential? item) (vec item)
                        (map? item) (into {} item)
                        :else item))
        result (postwalk unlazy-item coll)]
    result))


(defn- find-node [tree predicate]
  (when (map? tree)
    (if (predicate tree)
      tree
      (some #(find-node % predicate) (:content tree)))))


(defn- find-lvl [tree level]
  (find-node tree
             (fn [node]
               (and (map? node)
                    (= (:tag node) ooxml/tag-lvl)
                    (= (str level) (-> node :attrs ooxml/attr-ilvl str))))))


(defn- get-id-style-xml [tree id level]
  (assert (integer? level))
  (assert (string? id))
  (assert (map? tree) (str "Not a map: " (pr-str (type tree))))
  (let [def1 (find-node tree
                       (fn [node]
                         (and (map? node)
                              (= (:tag node) ooxml/tag-num)
                              (= id (-> node :attrs ooxml/attr-numId)))))]
    (or (find-lvl def1 level) ;; find in override

        ;; find abstract definition
        (let [abstract-id (-> (find-node def1 (comp #{ooxml/xml-abstract-num-id} :tag)) :attrs ooxml/val)
              abstract (find-node tree
                                  (fn [node]
                                    (and (= (:tag node) ooxml/tag-abstract-num)
                                         (= abstract-id (-> node :attrs ooxml/xml-abstract-num-id)))))]
          (find-lvl abstract level)))))


(defn- xml-lvl-parse [tree]
  (assert (= ooxml/tag-lvl (:tag tree)))
  (letfn [(node-attr [tag] (-> tree (find-node (comp #{tag} name :tag)) :attrs ooxml/val))]
    {:lvl-text (node-attr "lvlText")
     :num-fmt  (node-attr "numFmt")
     :start    (->int (node-attr "start"))}))


(defn prepare-numbering-xml [xml-tree] xml-tree)


(defn- parse [numbering-file]
  (assert numbering-file)
  (with-open [r (io/input-stream (io/file numbering-file))]
    (let [tree (xml/parse r)
          eager-vals (unlazy tree)]
      (prepare-numbering-xml eager-vals))))


(defn main-numbering [dir main-document main-document-rels]
  (when-let [main-numbering-path
             (some #(when (= rel-type-numbering (:stencil.model/type %))
                      (unix-path (io/file (.getParentFile (io/file main-document))
                                       (:stencil.model/target %))))
                   (vals (:parsed main-document-rels)))]
    {:stencil.model/path       main-numbering-path
     :source-file              (io/file dir main-numbering-path)
     :parsed                   (parse (io/file dir main-numbering-path))}))


(defn style-def-for [id lvl]
  (assert (string? id))
  (assert (integer? lvl))
  (some-> (:parsed *numbering*)
          (get-id-style-xml id lvl)
          (xml-lvl-parse)))
