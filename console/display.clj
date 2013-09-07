(ns console.display
    (:require (console.environment))
    (:import
        (java.awt Color
                  Container
                  Dimension
                  Graphics
                  Graphics2D
                  GridBagLayout
                  GridBagConstraints
                  Insets
                  Polygon
                  RenderingHints
                  Toolkit)
        (java.awt.event KeyAdapter WindowAdapter KeyEvent)
        (javax.swing JPanel JFrame)
        (console.environment Obstacle Item Wall Label)))

(use 'geom-utils)
(use 'console.environment)

(defn draw-obstacle [^Graphics g ^Obstacle obs scale]
    (let [[x y] (meters-to-pixels (.x obs) (.y obs) scale)
          [r _] (meters-to-pixels (.r obs) 0 scale)]
        (.setColor g Color/BLACK)
        (.fillOval g (- x r) (- y r) (* r 2) (* r 2))))

(defn draw-wall [^Graphics g ^Wall wall scale]
    (let [[x1 y1] (meters-to-pixels (.x1 wall) (.y1 wall) scale)
          [x2 y2] (meters-to-pixels (.x2 wall) (.y2 wall) scale)]
        (.setColor g Color/BLACK)
        (.drawLine g x1 y1 x2 y2)))

; not implemented yet
(defn draw-label [^Graphics g ^Label label scale] nil)

(defn draw-item [^Graphics g ^Item obj scale]
    (let [[x y] (meters-to-pixels (.x obj) (.y obj) scale)
          [r _] (meters-to-pixels (.r obj) 0 scale)]
        (.setColor g (.color obj))
        (.fillOval g (- x r) (- y r) (* r 2) (* r 2))))

(defn get-color-from-string [color-name]
    (.get (.getField (Class/forName "java.awt.Color") color-name) nil))

(defn draw-robot [^Graphics g robot scale]
    (let [heading (:heading (:pos robot))
          loc (:location (:pos robot))
          points [[-0.5  0.5]
                  [ 0.5  0.5]
                  [ 1.0  0.0]
                  [ 0.5 -0.5]
                  [-0.5 -0.5]]
          points (map #(create-vec :x (first %1) :y (second %1)) points)
          points (map #(rotate-vec-z %1 heading) points)
          points (map #(add-vecs %1 loc) points)
          points (map #(meters-to-pixels (:x %1) (:y %1) scale) points)
          robot-poly (new Polygon)]
        (doseq [p points] (.addPoint robot-poly (first p) (second p)))
        (.setColor g (get-color-from-string (:color robot)))
        (.fillPolygon g robot-poly)))

(defn draw-robots [g robots scale]
    (doseq [robot robots]
        (draw-robot g robot scale)))

(defn draw-readings-for-robot [^Graphics g readings scale]
    (.setColor g Color/RED)
    (doseq [r readings]
        (let [[x y] (meters-to-pixels (:x r) (:y r) scale)]
            (.drawOval g (- x 2) (- y 2) (* 2 2) (* 2 2)))))

(defn draw-readings [g readings scale]
    (doseq [robot-readings (vals readings)]
        (draw-readings-for-robot g robot-readings scale)))

(defn draw-world [^Graphics2D g ^JPanel canvas env scale robots readings]
    (let [draw-entity (fn [draw-func entity-type]
                        (doseq [entity (entity-type env)] (draw-func g entity scale)))]
        (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
        (.setColor g Color/WHITE)
        (.fillRect g 0 0 (.getWidth canvas) (.getHeight canvas))
        (.setColor g Color/BLACK)
        (draw-entity draw-obstacle :obstacles)
        (draw-entity draw-wall :walls)
        (draw-entity draw-label :labels)
        (draw-entity draw-item :items)
        (draw-robots g robots scale)
        (draw-readings g readings scale)))

(defn create-world-canvas [env scale]
    (let [robots (atom {})
          readings (atom {})
          set-robots (fn [new-robots] (reset! robots new-robots))
          set-readings (fn [new-readings id] (reset! readings (assoc @readings id new-readings)))
          panel (proxy [JPanel] []
                    (paint [g] (draw-world g this env scale @robots @readings)))]
        {:set-robots set-robots
         :set-readings set-readings
         :canvas panel}))

(defn create-frame-window-listener [init-exit-fn]
    (proxy [WindowAdapter] []
        (windowClosing [e] (init-exit-fn))))

(defn create-frame-key-listener [pause-fn]
    (proxy [KeyAdapter] []
        (keyPressed [^KeyEvent e]
            (if (and (= (.getKeyCode e) KeyEvent/VK_P) (not (.isShiftDown e)))
                (pause-fn)))))

(defn create-main-frame [env init-exit-fn pause-fn]
    (let [screen-size (.getScreenSize (Toolkit/getDefaultToolkit))
          w (- (.getWidth screen-size) 100)
          h (- (.getHeight screen-size) 100)
          scale (get-env-scale w h (:width env) (:height env))
          canvas (create-world-canvas env scale)
          ^JPanel canvas_panel (:canvas canvas)
          frame (new JFrame "Robo Sim")
          ^Container content_pane (.getContentPane frame)]
        (.setSize frame w h)
        (.setLayout (.getContentPane frame) (new GridBagLayout))
        (.add content_pane
            canvas_panel
            (new GridBagConstraints 0 0 1 1 1.0 1.0
                                    GridBagConstraints/CENTER
                                    GridBagConstraints/BOTH
                                    (new Insets 0 0 0 0 )
                                    0 0 ))
        (.addWindowListener frame (create-frame-window-listener init-exit-fn))
        (.addKeyListener frame (create-frame-key-listener pause-fn))
        ;(.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
        (.setLocationRelativeTo frame nil)
        (.setVisible frame true)
        {:frame frame
         :canvas canvas}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Test stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; (defn get-env-for-test []
;     (-> (create-environment 50 50)
;         (add-obstacle-to-env (->Obstacle 10 10 2))
;         (add-obstacle-to-env (->Obstacle 15 15 3))
;         (add-wall-to-env (->Wall 15 35 25 35))
;         (add-wall-to-env (->Wall 25 35 35 25))
;         ;(add-wall-to-env (->Wall 35 25 35 15))
;         (add-item-to-env (->Item 49 49  1 Color/RED))))

; (create-main-frame {:environment (get-env-for-test)})
