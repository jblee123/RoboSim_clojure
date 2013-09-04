(ns robot.robot-interfaces)

(use 'robot.robot-comm)

(defn set-communicator [interface communicator]
    (assoc interface :communicator communicator))

(defn create-sim-robot-interface []
    {:get-position #(sim-get-position %1)
     :set-position #(send-position-update %1 %2)
     :move #(sim-move %1 %2)
     :obs-readings #(sim-get-obs %1)})
