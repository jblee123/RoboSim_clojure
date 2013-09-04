(ns robot.behaviors)

(use 'geom-utils)

;;; attempt 1: way too OO

; (defn create-anon-name-generator []
;     (let [prev-id (atom 0)]
;         (fn [] (str "AN_" (swap! prev-id inc)))))

; (defn create-behavior-system-context []
;     (let [cycle-num (atom 0)]
;         {:get-next-anon-name (create-anon-name-generator)
;          :get-cycle (fn [] @cycle-num)
;          :inc-cycle (fn [] (swap! cycle-num inc))}))

;;; attempt 2: got a little further before deciding *still* too OO...
;;; I want to do this w/out mutable stuff!

; (defn create-behavior-system-context []
;     {:prev-anon-name-id (atom 0)
;      :cycle-num (atom 0)})

; (defn get-next-anon-name [bs-context]
;     (str "AN_" (swap! (:prev-anon-name-id bs-context) inc)))

; (defn get-cycle [bs-context] @(:cycle-num bs-context))

; (defn inc-cycle [bs-context] (swap! (:cycle-num bs-context) inc))

; (defn create-behavior [bs-context name]
;     {:name (if (empty? name) (get-next-anon-name bs-context) name)
;      :output nil
;      :last-cycle -1
;      :system bs-context})

; (defn get-behavior-output [context compute-output]
;     (if (< (:last-cycle context) (get-cycle (:system context)))


; ; wander stuff

; (defn create-wander-behavior [bs-context name persistence]
;     (assoc (create-behavior bs-context name) :persistence persistence))

; (defn get-wander-output [context]
