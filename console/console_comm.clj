(ns console.console-comm)

(use 'robot-position)
(use 'comm-codes)
(use 'io-utils)

; (def handlers { POSITION          :update-robot-pos
;                 REQUEST_POSITION  :get-robot-pos
;                 GET_OBSTACLES     :get-obs-readings
;                 ROBOT_DYING       :robot-dying
;                 MOVE              :move-robot
;                 SPIN              :spin-robot })

(defn create-console-comm []
    {:channel nil
     :addresses {} })

(defn open-console-comm [comm]
    (assoc comm :channel (create-server-datagram-channel console-port)))

(defn register-robot-in-console-comm [comm id addr]
    (assoc-in comm [:addresses] {id addr}))

(defn unregister-robot-from-console-comm [comm id]
    (update-in comm [:addresses] dissoc id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; msg handling infrastructure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def msg-types { ALIVE            :robot-alive
                 POSITION         :update-robot-pos
                 REQUEST_POSITION :get-robot-pos
                 GET_OBSTACLES    :get-obs-readings
                 ROBOT_DYING      :robot-dying
                 MOVE             :move-robot
                 SPIN             :spin-robot })

(defn process-msg [msg]
    (let [msg (update-in msg [:data] unmarshall)]
        (if-let [msg-type (get msg-types (first (:data msg)))]
            (assoc-in msg [:data 0] msg-type)
            (println "Error: unregistered message number:" (first msg)))))

(defn get-next-msg [comm]
    (if-let [msg (recv-from (:channel comm))]
        (process-msg msg)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; msg sending stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-to-robot [comm id data]
    (send-to (:channel comm) (get (:addresses comm) id) (marshall data)))

(defn send-to-all-robots [comm data]
    ;(println "send-to-all-robots:" data)
    (doseq [id (keys (:addresses comm))] (send-to-robot comm id data)))

(defn send-start-msg [comm id]
    (send-to-robot comm id [START]))

(defn send-killall [comm]
    (send-to-all-robots comm [KILL]))

(defn send-switch-pause [comm]
    (send-to-all-robots comm [PAUSE]))

(defn send-robot-pos [comm id pos]
    (let [loc (:location pos)
          heading (:heading pos)]
        (send-to-robot comm id [POSITION (.x loc) (.y loc) (.z loc) heading])))

(defn send-obs-readings [comm id readings]
    (let [readings (mapcat (fn [r] [(.x r) (.y r)]) readings)]
        (send-to-robot comm id (cons OBS_READINGS readings))))
