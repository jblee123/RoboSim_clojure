(ns robot.robot)

(use 'robot-position)
(use 'robot.robot-interfaces)
(use 'robot.controller)
(use 'robot.robot-comm)

(def robot-instance nil)

(defn create-robot [id host type x-pos y-pos theta color max-vel max-angular-vel radius]
    (let [interface (if (= type "simulation")
                            (create-sim-robot-interface)
                            (do
                                (println "Error: robot type '" type "' not currently supported")
                                (.exit System -1)))
          pos (create-robot-position (create-location x-pos y-pos) theta)
          communicator (create-robot-comm id host)
          _ (send-alive-confirmation communicator pos color max-vel max-angular-vel radius)
          interface (set-communicator interface communicator)
          controller (create-robot-controller)]
        ((:set-position interface) communicator pos)
        {:communicator communicator :controller controller :interface interface}))

;(defn set-robot-behaviors [robot behavior-map behavior-list]
;    (assoc robot {:controller (add-controller-behaviors (:controller robot) behavior-map behavior-list)}))

(defn run-robot [controller comm interface behavior-map behavior-list]
    (run-controller controller comm interface behavior-map behavior-list))
