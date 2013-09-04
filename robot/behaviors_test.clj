(in-ns 'robot.behaviors)

(defn get-result-from-run-all [behavior-list behavior-map]
    ; (print behavior-list)
    ; (print behavior-map)
    (:output ((first (run-all-behaviors behavior-list behavior-map nil nil)) (last behavior-list))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; avoid obs stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-avoid-obs-behavior-test [obs]
    (let [obs-list (create-literal-behavior nil obs)
          safety-margin (create-literal-behavior nil 0.75)
          sphere-of-influence (create-literal-behavior nil 10)
          avoid-obs-behavior (create-avoid-obs-behavior "avoid-obs" (first obs-list)
                                                                    (first safety-margin)
                                                                    (first sphere-of-influence))
          [behavior-map behavior-list] (compile-behaviors obs-list safety-margin sphere-of-influence avoid-obs-behavior)
          data (second avoid-obs-behavior)]
        (println (get-avoid-obs-output data behavior-map nil nil))
        (println (:output (get-avoid-obs-output data behavior-map nil nil)))
        (println (get-result-from-run-all behavior-list behavior-map))))

(defn create-avoid-obs-behavior-test1 [] (create-avoid-obs-behavior-test []))
(defn create-avoid-obs-behavior-test1 [] (create-avoid-obs-behavior-test [(->Vector 0 1 0)
                                                                          (->Vector 1 0 0)]))
(defn create-avoid-obs-behavior-test2 [] (create-avoid-obs-behavior-test [(->Vector 0 1 0)
                                                                          (->Vector 1 0 0)
                                                                          (->Vector 20 0 0)]))
(defn create-avoid-obs-behavior-test3 [] (create-avoid-obs-behavior-test [(->Vector 1 0 0)
                                                                          (->Vector -0.5 0 0)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; global to egocentric stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-global-to-egocentric-behavior-test []
    (let [robot-pos (create-literal-behavior nil {:location (->Vector 1 1 0) :heading 90})
          global-pos (create-literal-behavior nil (->Vector 0 1 0))
          gte-behavior (create-global-to-egocentric-behavior "gte" (first robot-pos) (first global-pos))
          [behavior-map behavior-list] (compile-behaviors robot-pos global-pos gte-behavior)
          data (second gte-behavior)]
        (println (get-global-to-egocentric-output data behavior-map nil nil))
        (println (:output (get-global-to-egocentric-output data behavior-map nil nil)))
        (println (get-result-from-run-all behavior-list behavior-map))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; literal stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-literal-behavior-test []
    (let [lit5 (create-literal-behavior nil 5)
          [behavior-map behavior-list] (compile-behaviors lit5)
          data (second lit5)]
        (println (get-literal-output data behavior-map nil nil))
        (println (:output (get-literal-output data behavior-map nil nil)))
        (println (get-result-from-run-all behavior-list behavior-map))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; move to stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-move-to-behavior-test []
    (let [target (create-literal-behavior nil (->Vector 0 5 5))
          move-to-behavior (create-move-to-behavior "move-to" (first target))
          [behavior-map behavior-list] (compile-behaviors target move-to-behavior)
          data (second move-to-behavior)]
        (println (get-move-to-output data behavior-map nil nil))
        (println (:output (get-move-to-output data behavior-map nil nil)))
        (println (get-result-from-run-all behavior-list behavior-map))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; sum vectors stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-sum-vectors-behavior-test []
    (let [v1 (create-literal-behavior nil (->Vector 0 2 0))
          v2 (create-literal-behavior nil (->Vector 3 0 0))
          w1 (create-literal-behavior nil 0.5)
          w2 (create-literal-behavior nil 1/3)
          sum-vectors-behavior (create-sum-vectors-behavior "sum-vectors" (map first [v1 v2]) (map first [w1 w2]))
          [behavior-map behavior-list] (compile-behaviors v1 v2 w1 w2 sum-vectors-behavior)
          data (second sum-vectors-behavior)]
        (println (get-sum-vectors-output data behavior-map nil nil))
        (println (:output (get-sum-vectors-output data behavior-map nil nil)))
        (println (get-result-from-run-all behavior-list behavior-map))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; wander stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-wander-behavior-test []
    (let [lit5 (create-literal-behavior nil 5)
          wander1 (create-wander-behavior "wander1" (first lit5))
          [behavior-map behavior-list] (compile-behaviors lit5 wander1)
          data (second wander1)]
        (println (get-wander-output data behavior-map nil nil))
        (println (:output (get-wander-output data behavior-map nil nil)))
        (println (get-result-from-run-all behavior-list behavior-map))))
