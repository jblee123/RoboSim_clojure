(ns console.environment
    (:import java.awt.Color))

(use 'geom-utils)

(import java.awt.Color)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; environment members
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Obstacle [^double x ^double y ^double r]
    Object
    (toString [o] (format "[[%.02f, %.02f], %.02f]" (.x o) (.y o) (.r o))))

(defn intersect-obs-with-ray [^Obstacle obs ray]
    (intersect-ray-with-circle ray (.x obs) (.y obs) (.r obs)))

(defrecord Item [^double x ^double y ^double r ^Color color]
    Object
    (toString [item] (format "[[%.02f, %.02f], %.02f, %s]" (.x item) (.y item) (.r item) (.color item))))

(defrecord Wall [^double x1 ^double y1 ^double x2 ^double y2]
    Object
    (toString [w] (format "[[%.02f, %.02f], [%.02f, %.02f]]" (.x1 w) (.y1 w) (.x2 w) (.y2 w))))

(defn intersect-wall-with-ray [^Wall wall ray]
    (intersect-ray-with-segment ray (.x1 wall) (.y1 wall) (.x2 wall) (.y2 wall)))

(defrecord Label [^double x ^double y ^String text]
    Object
    (toString [label] (format "[[%.02f, %.02f], %s]" (.x label) (.y label) (.text label))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; environment scale stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-env-scale [screen-width screen-height env-width env-height]
    {:pix-per-meter (Math/min (float (/ screen-width env-width)) (float (/ screen-height env-height)))
     :env-width env-width :env-height env-height})

(defn meters-to-pixels [x y env-scale]
    [(* x (:pix-per-meter env-scale))
     (- (* (:pix-per-meter env-scale) (:env-height env-scale))
         (* y (:pix-per-meter env-scale)))])

(defn get-meter-dist-to-pixel-dist [meter-dist env-scale]
    (* meter-dist (:pix-per-meter env-scale)))

(defn get-env-size-in-pixels [env-scale]
    [(* (:pix-per-meter env-scale) (:env-width env-scale))
     (* (:pix-per-meter env-scale) (:env-height env-scale))])

(defn zoom-in-on-env [env-scale]
    (assoc env-scale :pix-per-meter (* (:pix-per-meter env-scale) 2)))

(defn zoom-out-on-env [env-scale]
    (assoc env-scale :pix-per-meter (/ (:pix-per-meter env-scale) 2)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; environment stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-environment [w h]
    {:width w
     :height h
     :obstacles []
     :walls []
     :labels []
     :items []})

(defn update-in-env [env to-add type]
    (assoc env type (conj (type env) to-add)))

(defn add-obstacle-to-env [env obstacle]
    (update-in-env env obstacle :obstacles))

(defn add-wall-to-env [env wall]
    (update-in-env env wall :walls))

(defn add-label-to-env [env label]
    (update-in-env env label :labels))

(defn add-item-to-env [env item]
    (update-in-env env item :items))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; environment test stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-env-for-test []
    (-> (create-environment 50 50)
        (add-obstacle-to-env (->Obstacle 10 10 2))
        (add-obstacle-to-env (->Obstacle 15 15 3))
        (add-wall-to-env (->Wall 15 35 25 35))
        (add-wall-to-env (->Wall 25 35 35 25))
        ;(add-wall-to-env (->Wall 35 25 35 15))
        (add-item-to-env (->Item 49 49  1 Color/RED))))

(defn get-ray-for-test [] (->Ray (->Vector 1 1 0) (->Vector 2 1 0)))
