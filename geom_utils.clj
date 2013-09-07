(ns geom-utils)

(def ^:const deg-per-circle 360.0)

(defn deg-to-rad [deg] (* deg (/ (* 2 Math/PI) deg-per-circle)))

(defn rad-to-deg [rad] (* rad (/ deg-per-circle (* 2 Math/PI))))

(defn normalize-angle [^double angle]
    (cond (< angle 0) (normalize-angle (+ angle deg-per-circle))
          (>= angle deg-per-circle) (normalize-angle (- angle deg-per-circle))
          :else angle))

(defn avg-angles [^double a1 ^double a2]
    (if (<= (Math/abs (- a1 a2)) (/ deg-per-circle 2))
        (/ (+ a1 a2) 2.0)
        (normalize-angle (/ (+ a1 a2 deg-per-circle) 2.0))))

(defn angle-diff [^double a1 ^double a2]
    (let [diff (Math/abs (- a1 a2))]
        (if (<= diff (/ deg-per-circle 2.0))
            diff
            (- deg-per-circle diff))))

;;; Vector stuff

(defrecord Vector [^double x ^double y ^double z]
    Object
    (toString [v] (format "[%.02f, %.02f, %.02f]" (.x v) (.y v) (.z v))))

(defn create-vec [& {:keys [x y z] :or {x 0 y 0 z 0}}]
    (Vector. x y z))

(defn add-vecs [^Vector v1 ^Vector v2]
    (Vector. (+ (.x v1) (.x v2))
             (+ (.y v1) (.y v2))
             (+ (.y v1) (.y v2))))

(defn add-vecs [& rest]
    (Vector. (apply + (map :x rest))
             (apply + (map :y rest))
             (apply + (map :z rest))))

(defn sub-vecs [^Vector v & rest]
    (Vector. (apply - (cons (.x v) (map :x rest)))
             (apply - (cons (.y v) (map :y rest)))
             (apply - (cons (.z v) (map :z rest)))))

(defn mul-vec [^Vector v scalar]
    (Vector. (* (.x v) scalar)
             (* (.y v) scalar)
             (* (.z v) scalar)))

(defn div-vec [^Vector v scalar]
    (Vector. (/ (.x v) scalar)
             (/ (.y v) scalar)
             (/ (.z v) scalar)))

(defn rotate-vec-z [^Vector v degs]
    (let [rads (deg-to-rad degs)
          c (Math/cos rads)
          s (Math/sin rads)
          x (.x v)
          y (.y v)]
        (Vector. (- (* x c) (* y s))
                 (+ (* x s) (* y c))
                 (.z v))))

(defn get-vec-angle [^Vector v]
    (normalize-angle (rad-to-deg (Math/atan2 (.y v) (.x v)))))

(defn get-vec-len [^Vector v]
    (Math/sqrt (+ (* (.x v) (.x v))
                  (* (.y v) (.y v))
                  (* (.z v) (.z v)))))

(defn get-unit-vec [v]
    (let [l (get-vec-len v)]
        (if (= l 0.0) (Vector. 1 0 0) (div-vec v l))))

;;; Ray stuff

(defrecord Ray [^Vector from-vec ^Vector to-vec])

(defn intersect-ray-with-circle [^Ray ray xc yc rad]
    (let [^Vector from-vec (.from-vec ray)
          ^Vector to-vec (.to-vec ray)
          x0 (.x from-vec)
          y0 (.y from-vec)
          dx (- (.x to-vec) x0)
          dy (- (.y to-vec) y0)
          a (+ (* dx dx) (* dy dy))
          b (* 2 (+ (* x0 dx)
                    (- (* xc dx))
                    (* y0 dy)
                    (- (* yc dy))))
          c (+ (* x0 x0)
               (* xc xc)
               (- (* 2 x0 xc))
               (* y0 y0)
               (* yc yc)
               (- (* 2 y0 yc))
               (- (* rad rad)))
          quotient (- (* b b) (* 4 a c))]
        (if (>= quotient 0)
            (let [t1 (/ (+ (- b) (Math/sqrt quotient)) (* 2 a))
                  t2 (/ (- (- b) (Math/sqrt quotient)) (* 2 a))
                  v1 (if (>= t1 0) (Vector. (+ x0 (* t1 dx)) (+ y0 (* t1 dy)) 0) nil)
                  v2 (if (>= t2 0) (Vector. (+ x0 (* t2 dx)) (+ y0 (* t2 dy)) 0) nil)]
                (if (and v1 v2)
                    (if (< (get-vec-len (sub-vecs v1 (.from-vec ray)))
                                       (get-vec-len (sub-vecs v2 (.from-vec ray)))) v1 v2)
                    (if v1 v1 v2))))))

(defn intersect-ray-with-segment [^Ray ray xs0 ys0 xs1 ys1]
    (let [^Vector from-vec (.from-vec ray)
          ^Vector to-vec (.to-vec ray)
          x0 (.x from-vec)
          y0 (.y from-vec)
          dxr (- (.x to-vec) x0)
          dyr (- (.y to-vec) y0)
          dxs (- xs1 xs0)
          dys (- ys1 ys0)]
          (if (and (not (zero? (- (* dxs dyr) (* dys dxr))))
                   (or (not (zero? dxr)) (not (zero? dyr))))
              (let [ts (+ (* dxr (- ys0 y0)) (* dyr (- x0 xs0)))
                    ts (/ ts (- (* dxs dyr) (* dys dxr)))
                    tr (if (not (zero? dxr))
                           (/ (+ xs0 (* ts dxs) (- x0)) dxr)
                           (/ (+ ys0 (* ts dys) (- y0)) dyr))]
                  (if (and (>= ts 0) (<= ts 1) (>= tr 0))
                      (Vector. (+ xs0 (* ts dxs)) (+ ys0 (* ts dys)) 0))))))
