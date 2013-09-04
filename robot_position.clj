(ns robot-position)

(defn create-location
    ([] (create-location 0 0 0))
    ([x y] (create-location x y 0))
    ([x y z] {:x x :y y :z z}))

(defn create-robot-position [location heading]
    {:location location :heading heading})
