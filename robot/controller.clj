(ns robot.controller)

(use 'robot.robot-comm)

;(require '[robot.behaviors :as behaviors])
(use 'robot.behaviors)

;(defn add-controller-behaviors [controller behavior-map behavior-list]
;    (assoc controller {:behavior-map behavior-map :behavior-list behavior-list}))

(defn get-controller-paused [controller]
    (:paused controller))

(defn set-controller-paused [controller paused]
    (assoc controller :paused paused))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; handlers of unsolicited msgs from the console
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start-robot [controller comm msg]
    (set-controller-paused controller false))

(defn kill [controller comm msg]
    (perform-comm-shutdown comm)
    (System/exit 0))

(defn switch-paused [controller comm msg]
    (set-controller-paused controller (not (get-controller-paused controller))))

(def console-handlers
    {:start-robot start-robot
     :kill kill
     :switch-paused switch-paused})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; running behaviors / main loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-msg [controller comm msg]
    ;(println "msg:" msg)
    ((get console-handlers (first msg)) controller comm msg))

(defn handle-msgs [controller comm]
    (let [[comm msg] (get-next-msg comm)]
        ;(println "  msg:" msg)
        (if msg
            (handle-msgs (handle-msg controller comm msg) comm)
            [controller comm])))

(defn run-controller [controller comm interface behavior-map behavior-list]
    (loop [controller controller
           interface interface
           comm comm
           behavior-map behavior-map]
        (let [[controller comm] (handle-msgs controller comm)]
            (if (get-controller-paused controller)
                (recur controller interface comm behavior-map)
                (let [[behavior-map interface comm] (run-all-behaviors behavior-list
                                                                       behavior-map
                                                                       interface
                                                                       comm)]
                    (recur controller interface comm behavior-map))))))




(defn create-robot-controller []
    {:behavior-list []
     :behavior-map {}
     :paused true})
     ; :get-controller-paused get-controller-paused
     ; :set-controller-paused set-controller-paused})
