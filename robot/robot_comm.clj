(ns robot.robot-comm)

(use 'io-utils)
(use 'comm-codes)
(use 'robot-position)
(use 'geom-utils)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; comms stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-robot-comm
    ([id host] (create-robot-comm id host console-port))
    ([id host port]
        {:id id
         :channel (create-client-datagram-channel host port)
         :queued-msgs clojure.lang.PersistentQueue/EMPTY }))

(defn clear-robot-comm-msg-queue [comm]
    (assoc comm :queued-msgs clojure.lang.PersistentQueue/EMPTY))

(defn add-msg-to-robot-comm-msg-queue [comm msg]
    (update-in comm [:queued-msgs] conj msg))

(defn send-to-console [comm data]
    (send-to-server (:channel comm) (marshall data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; msg handling infrastructure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def msg-types { START :start-robot
                 KILL  :kill
                 PAUSE :switch-paused })

(defn ensure-type [msg]
    (if-let [msg-type (get msg-types (first msg))]
        (assoc-in msg [0] msg-type)
        (println "Error: unregistered message from console number:" (first msg))))

(defn get-next-msg [comm]
    (if (> (count (:queued-msgs comm)) 0)
        [(update-in comm [:queued-msgs] pop)
         (first (:queued-msgs comm))]
        (let [msg (recv-from (:channel comm))
              msg (and msg (ensure-type (unmarshall (:data msg))))]
            [comm msg])))

(defn wait-for-msg [comm wait-for]
    (loop [comm comm]
        (if-let [msg (recv-from (:channel comm))]
            (let [msg (unmarshall (:data msg))]
                (if (= (first msg) wait-for)
                        [comm msg]
                        (recur (add-msg-to-robot-comm-msg-queue comm (ensure-type msg)))
                ))
            (recur comm))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; msgs we initiate to the console
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-alive-confirmation [comm pos color max-vel max-angular-vel radius]
    (let [{{x :x y :y z :z} :location t :heading} pos]
        (send-to-console comm
            [ALIVE (:id comm) x y z t color max-vel max-angular-vel radius])))

(defn send-position-update [comm pos]
    (let [{{x :x y :y z :z} :location t :heading} pos]
        (send-to-console comm [POSITION (:id comm) x y z t])))

(defn send-death-msg [comm]
    (send-to-console comm [ROBOT_DYING (:id comm)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; msgs we initiate to interact with the robot
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sim-get-position [comm]
    (send-to-console comm [REQUEST_POSITION (:id comm)])
    (let [[comm [type x y z t]] (wait-for-msg comm POSITION)]
        [comm (create-robot-position (create-location x y z) t)]))

(defn sim-get-obs [comm]
    (send-to-console comm [GET_OBSTACLES (:id comm)])
    (let [[comm msg] (wait-for-msg comm OBS_READINGS)
          pairs (partition 2 (rest msg))
          readings (map #(create-vec :x (first %1) :y (second %1)) pairs)]
        [comm readings]))

(defn sim-move [comm movement]
    (send-to-console comm [MOVE (:id comm) (:x movement) (:y movement)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; shutdown
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn perform-comm-shutdown [comm]
    (send-death-msg comm)
    (close-datagram-channel (:channel comm)))
