(ns robot.behaviors)

(use 'geom-utils)

(defn print-behaviors [behavior-map]
    (println "printing behaviors:")
    (println "-------------------")
    (doseq [[beh-key beh-data] behavior-map]
        (println (:name beh-key) ":" (:output beh-data)))
    (println "-------------------"))

(defn beh-key [beh] (first beh))

(defn create-behavior [name output-func]
    (list {:name (if (empty? name) (str (gensym "AN_")) name)
           :output-func output-func}
          {:output nil}))

(defn get-output [data behaviors k] (:output (behaviors (k data))))

(defn to-behavior-map [behaviors] (reduce #(assoc %1 (first %2) (second %2)) {} behaviors))

(defn compile-behaviors [& behaviors] (list (to-behavior-map behaviors) (map first behaviors)))

(defn run-all-behaviors-reduce-fn [so-far behavior-key]
    ; (println "#################################")
    ; (println "getting output for" (:name behavior-key))
    ;(print-behaviors (first so-far))
    (let [[behavior-map interface comm] so-far
          output-func (:output-func behavior-key)
          behavior-data (behavior-map behavior-key)
          [behavior-data comm] (output-func behavior-data behavior-map interface comm)]
        ;(println "computed an output for" (:name behavior-key) "of" (:output behavior-data))
        [(assoc behavior-map behavior-key behavior-data) interface comm]))

(defn run-all-behaviors [behavior-list behavior-map interface comm]
    (reduce run-all-behaviors-reduce-fn [behavior-map interface comm] behavior-list))
    ;(reduce #(assoc %1 %2 ((:output-func %2) (%1 %2) %1)) behavior-map behavior-list))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; avoid obs stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-avoid-obs-output [data behaviors interface comm]
    (let [obs-list (get-output data behaviors :obs-list)
          safety-margin (get-output data behaviors :safety-margin)
          sphere-of-influence (get-output data behaviors :sphere-of-influence)]
        [(assoc data
            :output (->> obs-list
                        (keep #(if (< (get-vec-len %1) sphere-of-influence) %1))
                        (map #(if (< (get-vec-len %1) safety-margin) (mul-vec %1 -100000) (mul-vec %1 -1)))
                        (reduce add-vecs)))
         comm]))

(defn create-avoid-obs-behavior [name obs-list safety-margin sphere-of-influence]
    (let [[behavior data] (create-behavior name get-avoid-obs-output)]
        (list behavior
              (assoc data :obs-list (beh-key obs-list)
                          :safety-margin (beh-key safety-margin)
                          :sphere-of-influence (beh-key sphere-of-influence)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; global to egocentric stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-global-to-egocentric-output [data behaviors interface comm]
    (let [robot-pos (get-output data behaviors :robot-pos)
          global-pos (get-output data behaviors :global-pos)]
        [(assoc data :output (rotate-vec-z (sub-vecs global-pos (:location robot-pos))
                                          (* (:heading robot-pos) -1)))
         comm]))

(defn create-global-to-egocentric-behavior [name robot-pos global-pos]
    (let [[behavior data] (create-behavior name get-global-to-egocentric-output)]
        (list behavior
              (assoc data :robot-pos (beh-key robot-pos)
                          :global-pos (beh-key global-pos)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; literal stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-literal-output [data behaviors interface comm] [data comm])

(defn create-literal-behavior [name value]
    (let [[behavior data] (create-behavior name get-literal-output)]
        (list behavior (assoc data :output value))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; move to stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-move-to-output [data behaviors interface comm]
    [(assoc data :output (get-unit-vec (get-output data behaviors :target)))
     comm])

(defn create-move-to-behavior [name target]
    (let [[behavior data] (create-behavior name get-move-to-output)]
        (list behavior
              (assoc data :target (beh-key target)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; sum vectors stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-sum-vectors-output [data behaviors interface comm]
    (let [vectors (map #(:output (behaviors %1)) (:vectors data))
          weights (map #(:output (behaviors %1)) (:weights data))]
        [(assoc data :output (reduce add-vecs (map mul-vec vectors weights)))
         comm]))

(defn create-sum-vectors-behavior [name vectors weights]
    (let [[behavior data] (create-behavior name get-sum-vectors-output)]
        (list behavior
              (assoc data :vectors (map beh-key vectors)
                          :weights (map beh-key weights)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; wander stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-wander-output [data behaviors interface comm]
    [(if (or (nil? (:output data))
            (>= (:same-dir-count data) (get-output data behaviors :persistence)))
        (assoc data :same-dir-count 1
                    :output (rotate-vec-z (->Vector 1 0 0) (* (rand) 360)))
        (assoc data :same-dir-count (inc (:same-dir-count data))))
     comm])

(defn create-wander-behavior [name persistence]
    (let [[behavior data] (create-behavior name get-wander-output)]
        (list behavior
              (assoc data :persistence (beh-key persistence)
                          :same-dir-count 0))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; get-obs stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-get-obs-output [data behaviors interface comm]
    (let [[comm readings] ((:obs-readings interface) comm)]
        [(assoc data :output readings) comm]))

(defn create-get-obs-behavior [name]
    (create-behavior name get-get-obs-output))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; get-position stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-get-position-output [data behaviors interface comm]
    (let [[comm pos] ((:get-position interface) comm)]
        [(assoc data :output pos) comm]))

(defn create-get-position-behavior [name]
    (create-behavior name get-get-position-output))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; move-robot stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-move-robot-output [data behaviors interface comm]
    (let [movement (get-output data behaviors :movement)
          base-speed (get-output data behaviors :base-speed)
          max-speed (get-output data behaviors :max-speed)
          vec (mul-vec movement base-speed)
          vec (if (> (get-vec-len vec) max-speed)
                  (mul-vec (get-unit-vec vec) max-speed)
                  vec)]
        ((:move interface) comm vec)
        [data comm]))

(defn create-move-robot-behavior [name movement base-speed max-speed]
    (let [[behavior data] (create-behavior name get-move-robot-output)]
        (list behavior
              (assoc data :movement (beh-key movement)
                          :base-speed (beh-key base-speed)
                          :max-speed (beh-key max-speed)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(load "behaviors_test")