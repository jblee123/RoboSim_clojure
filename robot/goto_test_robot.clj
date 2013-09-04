(ns robot.goto-test-robot)

(use 'geom-utils)
(use 'robot.behaviors)
(use 'robot.robot)

(println "starting test robot")

(defn create-goto-test []
    (let [get-pos (create-get-position-behavior "get-pos")
          target (create-literal-behavior "target" (->Vector 49 49 0))
          global-to-egocentric (create-global-to-egocentric-behavior "gte-for-target" get-pos target)
          move-to (create-move-to-behavior "move-to" global-to-egocentric)

          get-obs (create-get-obs-behavior "get-obs")
          safety-margin (create-literal-behavior "safety-margin" 1.5)
          sphere-of-influence (create-literal-behavior "sphere-of-influence" 5)
          avoid-obs (create-avoid-obs-behavior "avoid-obs" get-obs safety-margin sphere-of-influence)

          wander-persistence (create-literal-behavior "wander-persistence" 10)
          wander (create-wander-behavior "wander" wander-persistence)

          move-to-weight (create-literal-behavior nil 1.0)
          avoid-obs-weight (create-literal-behavior nil 1.0)
          wander-weight (create-literal-behavior nil 0.3)

          sum-vecs (create-sum-vectors-behavior "sum-vecs"
                        [move-to avoid-obs wander]
                        [move-to-weight avoid-obs-weight wander-weight])
          base-speed (create-literal-behavior nil 1.0)
          max-speed (create-literal-behavior nil 1.0)

          move-robot (create-move-robot-behavior "move-robot" sum-vecs base-speed max-speed)]

        (compile-behaviors get-pos
                           target
                           global-to-egocentric
                           move-to
                           get-obs
                           safety-margin
                           sphere-of-influence
                           avoid-obs
                           wander-persistence
                           wander
                           move-to-weight
                           avoid-obs-weight
                           wander-weight
                           sum-vecs
                           base-speed
                           max-speed
                           move-robot)))

(let [robot (create-robot 1 "localhost" "simulation" 1 1 0 "blue" 1 20 0.5)
      [behavior-map behavior-list] (create-goto-test)]
    (run-robot (:controller robot) (:communicator robot) (:interface robot) behavior-map behavior-list))
