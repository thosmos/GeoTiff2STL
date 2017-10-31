(ns stl-gen)


;(defn points-loop [width height hmap]
;  (loop [y 0 pts []]
;    (loop [x 0 pts pts]
;      (let [x1 (+ x 1)
;            y1 (+ y 1)
;            xy (aget hmap x y)
;            x1y (aget hmap x1 y)
;            xy1 (aget hmap x y1)
;            x1y1 (aget hmap x1 y1)
;            pts (cond-> pts
;                        ;;lower left triangles
;                        (or (> xy 0.0) (> x1y 0.0) (> xy1 0.0))
;                        (->
;                          ;; top
;                          (conj x y xy, x1 y x1y, x y1 xy1)
;                          ;; bottom
;                          (conj x y 0.0, x1 y 0.0, x y1 0.0)
;                          ;; sides
;                          (cond->
;                            ;; left
;                            (= x 0)
;                            (conj 0.0 y xy, 0.0 y1 xy1, 0.0 y 0.0
;                                  0.0 y1 xy1, 0.0 y1 0.0, 0.0 y 0.0)
;                            ;; near
;                            (= y 0)
;                            (conj x 0.0 xy, x 0.0 0.0, x1 0.0 x1y
;                                  x1 0.0 x1y, x 0.0 0.0, x1 0.0 0.0)))
;
;                        ;; upper right triangles
;                        (or (> x1y 0.0) (> x1y1 0.0) (> xy1 0.0))
;                        (->
;                          ;; top
;                          (conj x1 y x1y, x1 y1 x1y1, x y1 xy1)
;                          ;; bottom
;                          (conj x1 y 0.0, x1 y1 0.0, x y1 0.0)
;                          ;; sides
;                          (cond->
;                            ;; right
;                            (= x (- width 2))
;                            (conj x1 y x1y, x1 y 0.0, x1 y1 x1y1
;                                  x1 y1 x1y1, x1 y 0.0, x1 y1 0.0)
;                            ;; far
;                            (= y (- height 2))
;                            (conj x y1 xy1, x1 y1 x1y1, x y1 0.0
;                                  x y1 0.0, x1 y1 x1y1, x1 y1 0.0))))]
;        (if (< x (- width 2))
;          (recur (inc x) pts)
;          (if (< y (- height 2))
;            (recur (inc y) pts)
;            pts))))))
;
;(defn points-concat [width height hmap]
;  (concat
;    (for [y (range (- height 1))
;          x (range (- width 1))
;          :let [x1 (+ x 1)
;                y1 (+ y 1)
;                xy (aget hmap x y)
;                x1y (aget hmap x1 y)
;                xy1 (aget hmap x y1)
;                x1y1 (aget hmap x1 y1)
;                pts (cond-> []
;
;                            ;;lower left triangles
;                            (or (> xy 0.0) (> x1y 0.0) (> xy1 0.0))
;                            (->
;                              ;; top
;                              (conj x y xy, x1 y x1y, x y1 xy1)
;                              ;; bottom
;                              (conj x y 0.0, x1 y 0.0, x y1 0.0)
;                              ;; sides
;                              (cond->
;                                ;; left
;                                (= x 0)
;                                (conj 0.0 y xy, 0.0 y1 xy1, 0.0 y 0.0
;                                      0.0 y1 xy1, 0.0 y1 0.0, 0.0 y 0.0)
;                                ;; near
;                                (= y 0)
;                                (conj x 0.0 xy, x 0.0 0.0, x1 0.0 x1y
;                                      x1 0.0 x1y, x 0.0 0.0, x1 0.0 0.0)))
;
;                            ;; upper right triangles
;                            (or (> x1y 0.0) (> x1y1 0.0) (> xy1 0.0))
;                            (->
;                              ;; top
;                              (conj x1 y x1y, x1 y1 x1y1, x y1 xy1)
;                              ;; bottom
;                              (conj x1 y 0.0, x1 y1 0.0, x y1 0.0)
;                              ;; sides
;                              (cond->
;                                ;; right
;                                (= x (- width 2))
;                                (conj x1 y x1y, x1 y 0.0, x1 y1 x1y1
;                                      x1 y1 x1y1, x1 y 0.0, x1 y1 0.0)
;                                ;; far
;                                (= y (- height 2))
;                                (conj x y1 xy1, x1 y1 x1y1, x y1 0.0
;                                      x y1 0.0, x1 y1 x1y1, x1 y1 0.0))))]]
;      pts)))
;
;(defn points-array [width height hmap]
;  (concat
;    (for [y (range (- height 1))
;         x (range (- width 1))
;         :let [x1 (+ x 1)
;               y1 (+ y 1)
;               xy (aget hmap x y)
;               x1y (aget hmap x1 y)
;               xy1 (aget hmap x y1)
;               x1y1 (aget hmap x1 y1)]]
;     (persistent!
;       (cond->
;         (transient [])
;
;         ;;lower left triangles
;         (or (> xy 0.0) (> x1y 0.0) (> xy1 0.0))
;         (->
;           ;; top
;           (conj! x)
;           (conj! y)
;           (conj! xy)
;           (conj! x1)
;           (conj! y)
;           (conj! x1y)
;           (conj! x)
;           (conj! y1)
;           (conj! xy1)
;
;           ;; bottom
;           (conj! x)
;           (conj! y)
;           (conj! 0.0)
;           (conj! x1)
;           (conj! y)
;           (conj! 0.0)
;           (conj! x)
;           (conj! y1)
;           (conj! 0.0)
;
;           ;; sides
;           (cond->
;             ;; left
;             (= x 0)
;             (->
;               (conj! 0.0)
;               (conj! y)
;               (conj! xy)
;               (conj! 0.0)
;               (conj! y1)
;               (conj! xy1)
;               (conj! 0.0)
;               (conj! y)
;               (conj! 0.0)
;               (conj! 0.0)
;               (conj! y1)
;               (conj! xy1)
;               (conj! 0.0)
;               (conj! y1)
;               (conj! 0.0)
;               (conj! 0.0)
;               (conj! y)
;               (conj! 0.0))
;
;             ;; near
;             (= y 0)
;             (->
;               (conj! x)
;               (conj! 0.0)
;               (conj! xy)
;               (conj! x)
;               (conj! 0.0)
;               (conj! 0.0)
;               (conj! x1)
;               (conj! 0.0)
;               (conj! x1y)
;               (conj! x1)
;               (conj! 0.0)
;               (conj! x1y)
;               (conj! x)
;               (conj! 0.0)
;               (conj! 0.0)
;               (conj! x1)
;               (conj! 0.0)
;               (conj! 0.0))))
;
;         ;; upper right triangles
;         (or (> x1y 0.0) (> x1y1 0.0) (> xy1 0.0))
;         (->
;           ;; top
;           (conj! x1)
;           (conj! y)
;           (conj! x1y)
;           (conj! x1)
;           (conj! y1)
;           (conj! x1y1)
;           (conj! x)
;           (conj! y1)
;           (conj! xy1)
;
;           ;; bottom
;           (conj! x1)
;           (conj! y)
;           (conj! 0.0)
;           (conj! x1)
;           (conj! y1)
;           (conj! 0.0)
;           (conj! x)
;           (conj! y1)
;           (conj! 0.0)
;
;           ;; sides
;           (cond->
;             ;; right
;             (= x (- width 2))
;             (->
;               (conj! x1)
;               (conj! y)
;               (conj! x1y)
;               (conj! x1)
;               (conj! y)
;               (conj! 0.0)
;               (conj! x1)
;               (conj! y1)
;               (conj! x1y1)
;               (conj! x1)
;               (conj! y1)
;               (conj! x1y1)
;               (conj! x1)
;               (conj! y)
;               (conj! 0.0)
;               (conj! x1)
;               (conj! y1)
;               (conj! 0.0))
;
;             ;; far
;             (= y (- height 2))
;             (->
;               (conj! x)
;               (conj! y1)
;               (conj! xy1)
;               (conj! x1)
;               (conj! y1)
;               (conj! x1y1)
;               (conj! x)
;               (conj! y1)
;               (conj! 0.0)
;               (conj! x)
;               (conj! y1)
;               (conj! 0.0)
;               (conj! x1)
;               (conj! y1)
;               (conj! x1y1)
;               (conj! x1)
;               (conj! y1)
;               (conj! 0.0)))))))))