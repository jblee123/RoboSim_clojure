(ns console.simulator
    (:import java.awt.Color))

(use 'geom-utils)
(use 'robot-position)
(use 'console.console-comm)
(use 'console.environment)

(defn create-robot-info
    ([] (create-robot-info (create-robot-position (create-location) 0)))
    ([pos] (create-robot-info pos Color/BLUE))
    ([pos color] (create-robot-info pos color -1))
    ([pos color max-vel] (create-robot-info pos color max-vel -1))
    ([pos color max-vel max-angular-vel] (create-robot-info pos color max-vel max-angular-vel 0.5))
    ([pos color max-vel max-angular-vel radius]
        {:pos pos :color color :max-vel max-vel :max-angular-vel max-angular-vel :radius radius}))

(defn get-robot-location [robot] (:location (:pos robot)))

(defn get-robot-heading [robot] (:heading (:pos robot)))

(def ^:const num-of-sim-rays 16)

(defn create-simulator
    ([] (create-simulator 0.2))
    ([time-step] {:time-step time-step :robots {}}))

(defn register-robot-in-simulator [simulator comm id pos color max-vel max-angular-vel radius]
    (let [simulator (assoc-in simulator [:robots id]
                        (create-robot-info pos color max-vel max-angular-vel radius))]
        (send-start-msg comm id)
        simulator))

(defn update-robot-pos [sim id pos]
    (assoc-in sim [:robots id :pos] pos))

(defn get-robot-pos [sim id]
    (get-in sim [:robots id :pos]))

(defn register-robot [sim comm id pos color max-vel max-angular-vel radius]
    (send-start-msg comm id)
    (assoc-in sim [:robots] id (create-robot-info pos color max-vel max-angular-vel radius)))

(defn global-to-egocentric [robot-pos to-convert]
    (rotate-vec-z (sub-vecs to-convert (:location robot-pos))
                  (* (:heading robot-pos) -1)))

(defn egocentric-to-global [robot-pos to-convert]
    (add-vecs (rotate-vec-z to-convert (:heading robot-pos))
              (:location robot-pos)))

(defn compare-dist-for-closeness [value so-far]
    (let [dist (get-vec-len value)]
        (if (< dist (first so-far))
            [dist value]
            so-far)))

(defn get-closest-reading [sim env robot ray-num]
    (let [ray-angle (* ray-num (/ 360.0 num-of-sim-rays))
          robot-heading (get-robot-heading robot)
          robot-loc (get-robot-location robot)
          v (add-vecs (rotate-vec-z (create-vec :x 1 :y 0 :z 0) (+ robot-heading ray-angle)) robot-loc)
          ray (->Ray robot-loc v)
          obs-readings (map #(intersect-obs-with-ray %1 ray) (:obstacles env))
          wall-readings (map #(intersect-wall-with-ray %1 ray) (:walls env))
          readings (remove #(nil? %1) (concat obs-readings wall-readings))
          readings (map #(global-to-egocentric (:pos robot) %1) readings)
          comparison-fn #(compare-dist-for-closeness %2 %1)]
        (second (reduce comparison-fn [1000000 nil] readings))))

(defn get-obs-readings [sim env id]
    (if-let [robot (get (:robots sim) id)]
        (remove #(nil? %1) (map #(get-closest-reading sim env robot %1) (range num-of-sim-rays)))
        []))

(defn kill-robot [sim id]
    (update-in sim [:robots] dissoc id))

(defn constrain-by-robot [sim requested robot]
    (let [time-step (:time-step sim)
          max-turn (* time-step (:max-angular-vel robot))
          angle (get-vec-angle requested)
          angle (if (< angle 180)
                    (min (* angle time-step) max-turn)
                    (max (* (- angle 360) time-step) (- max-turn)))
          dist (max (* (:max-vel robot) time-step)
                    (* (get-vec-len requested) time-step))]
        (rotate-vec-z (create-vec :x dist :y 0 :z 0) angle)))

(defn compare-intersection-for-closeness [value from-vec max-collision-dist so-far]
    (let [dist (get-vec-len (sub-vecs value from-vec))]
        (if (and (< dist max-collision-dist) (< dist (first so-far)))
            [dist value]
            so-far)))

(defn constrain-by-env [from-vec to-vec robot-radius env]
    (let [ray (->Ray from-vec to-vec)
          delta (sub-vecs to-vec from-vec)
          ray-len (get-vec-len delta)
          max-collision-dist (+ ray-len robot-radius)
          obs-intersections (map #(intersect-obs-with-ray %1 ray) (:obstacles env))
          wall-intersections (map #(intersect-wall-with-ray %1 ray) (:walls env))
          intersections (remove #(nil? %1) (concat obs-intersections wall-intersections))
          comparison-fn #(compare-intersection-for-closeness %2 from-vec max-collision-dist %1)
          closest (reduce comparison-fn [1000000 nil] intersections)
          closest-dist (first closest)
          closest-intersection (second closest)]
        (if closest-intersection
            (mul-vec (get-unit-vec delta) (- closest-dist robot-radius))
            delta)))

(defn move-robot [sim env id x y]
    (if-let [robot (get (:robots sim) id)]
        (let [requested (create-vec :x x :y y)
              movement (constrain-by-robot sim requested robot)
              movement (rotate-vec-z movement (:heading (:pos robot)))
              movement (constrain-by-env (get-robot-location robot)
                                         (add-vecs (get-robot-location robot) movement)
                                         (:radius robot)
                                         env)
              new-heading (get-vec-angle movement)
              new-loc (add-vecs (get-robot-location robot) movement)]
            (assoc-in sim [:robots id :pos] (create-robot-position new-loc new-heading)))
        sim))

; not implemented -- do nothing
(defn spin-robot [sim id theta] sim)
