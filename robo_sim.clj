(ns robo-sim
    (:import
        (java.awt Color)
        (java.awt.event ActionListener)
        (javax.swing JPanel Timer)))

(set! *warn-on-reflection* true)

(use 'geom-utils)
(use 'console.environment)
(use 'console.simulator)
(use 'console.display)
(use 'console.console-comm)
(use 'robot-position)

(def the-state (atom nil))

(defn schedule-action
    ([f] (schedule-action f 0))
    ([f delay]
        (let [timer (Timer. delay nil)]
            (.addActionListener timer
                (proxy [ActionListener] []
                    (actionPerformed [e] (apply f []))))
            (.setRepeats timer false)
            (.start timer))))

(defn exit-robo-sim []
    (System/exit 0))

(defn init-shutdown []
    (reset! the-state (assoc @the-state :shutting-down true))
    (send-killall (:comm @the-state))
    (schedule-action (fn [] (exit-robo-sim)) 1000))

(defn pause-robo-sim []
    (send-switch-pause (:comm @the-state)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; display update funcs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn convert-readings-from-egocentric-to-global [readings robot-pos]
    (map #(egocentric-to-global robot-pos %1) readings))

(defn get-display-canvas [] (get-in @the-state [:console :canvas]))
(defn get-display-canvas-panel ^JPanel [] (:canvas (get-display-canvas)))
(defn set-display-robots-fn [] (:set-robots (get-display-canvas)))
(defn set-display-readings-fn [] (:set-readings (get-display-canvas)))

(defn repaint-display []
    (.repaint (get-display-canvas-panel)))

(defn set-display-robots []
    (let [robots (vals (:robots (:simulator @the-state)))]
        ((set-display-robots-fn) robots)
        (repaint-display)))

(defn set-display-readings [readings id]
    (let [robot-pos (get-in @the-state [:simulator :robots id :pos])
          readings (convert-readings-from-egocentric-to-global readings robot-pos)]
        ((set-display-readings-fn) readings id)
        (repaint-display)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; msg handlings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-robot-position [x y z t]
    (create-robot-position (create-vec :x x :y y :z z) t))

(defn handle-robot-alive [the-state msg addr]
    (let [[_ id x y z t color max-vel max-angular-vel radius] msg
          pos (build-robot-position x y z t)
          comm (register-robot-in-console-comm (:comm the-state) id addr)
          sim (register-robot-in-simulator (:simulator the-state) comm
                                           id pos color max-vel max-angular-vel radius)]
        (assoc the-state :comm comm :simulator sim)))

(defn handle-update-robot-pos [the-state msg]
    (let [[_ id x y z t] msg
          pos (build-robot-position x y z t)]
        (assoc-in the-state [:simulator :robots id :pos] pos)))

(defn handle-get-robot-pos [the-state msg]
    (let [[_ id] msg
          pos (get-robot-pos (:simulator the-state) id)]
         (send-robot-pos (:comm the-state) id pos)
         the-state))

(defn handle-get-obs-readings [the-state msg]
    (let [[_ id] msg
          readings (get-obs-readings (:simulator the-state) (:environment the-state) id)]
         (send-obs-readings (:comm the-state) id readings)
         (set-display-readings readings id)
         the-state))

(defn handle-robot-dying [the-state msg]
    (let [[_ id] msg
          comm (unregister-robot-from-console-comm (:comm the-state) id)
          simulator (kill-robot (:simulator the-state) id)]
        (if (and (:shutting-down the-state) (= (count (get-in the-state [:simulator :robots])) 0))
            (exit-robo-sim))
        (assoc the-state :comm comm :simulator simulator)))

(defn handle-move-robot [the-state msg]
    (let [[_ id x y] msg
          sim (move-robot (:simulator the-state) (:environment the-state) id x y)]
        (assoc the-state :simulator sim)))

(defn handle-spin-robot [the-state msg] the-state
    (let [[_ id theta] msg
          sim (spin-robot (:simulator the-state) id theta)]
        (assoc the-state :simulator sim)))

(def msg-handlers {:robot-alive      handle-robot-alive
                   :update-robot-pos handle-update-robot-pos
                   :get-robot-pos    handle-get-robot-pos
                   :get-obs-readings handle-get-obs-readings
                   :robot-dying      handle-robot-dying
                   :move-robot       handle-move-robot
                   :spin-robot       handle-spin-robot })

(defn handle-comm-msg []
    (if-let [msg (get-next-msg (:comm @the-state))]
        (let [{addr :addr msg :data} msg
               handler (get msg-handlers (first msg))
               new-state (if (= handler handle-robot-alive)
                             (handler @the-state msg addr)
                             (handler @the-state msg))]
            (reset! the-state new-state)
            (set-display-robots)
            msg)))

(defn schedule-handle-comm-msg []
    (schedule-action (fn []
        (handle-comm-msg)
        (schedule-handle-comm-msg))))

(defn schedule-robo-sim-init [environment]
    (schedule-action (fn []
        (reset! the-state {:simulator (create-simulator)
                           :environment environment
                           :console (create-main-frame environment init-shutdown pause-robo-sim)
                           :comm (open-console-comm (create-console-comm))
                           :shutting-down false})
        (schedule-handle-comm-msg))))

(defn init-robo-sim [environment]
    (schedule-robo-sim-init environment))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; run the program
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-env-for-test-goto []
    (-> (create-environment 50 50)
        (add-obstacle-to-env (->Obstacle 10 10 2))
        (add-obstacle-to-env (->Obstacle 15 15 3))
        (add-wall-to-env (->Wall 15 35 25 35))
        (add-wall-to-env (->Wall 25 35 35 25))
        ;(add-wall-to-env (->Wall 35 25 35 15))
        (add-item-to-env (->Item 49 49  1 Color/RED))))

(init-robo-sim (get-env-for-test-goto))


(.exec (Runtime/getRuntime) "clojure -cp . -port 44444 robot/goto_test_robot.clj")


(loop []
    (Thread/sleep 250)
    (recur))
