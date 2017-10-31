(ns hellogeotiff
  (:gen-class)
  (:import [org.geotools.data CachingFeatureSource FeatureSource FileDataStore FileDataStoreFinder])
  (:import [java.io File]
           [java.awt.image BufferedImage]
           [java.awt.image Raster]
           [javax.imageio ImageIO ImageReader]
           [com.heightmap.stl StlObject ModelObject]
           )
  (:require [clojure.pprint :refer [pprint]]))

(defn dostuff []
  (doseq [suffix (ImageIO/getReaderFileSuffixes)]
    (println "suffix:" suffix))

  (let [readers (ImageIO/getImageReadersByFormatName "TIFF")]
    (while (. readers hasNext)
      (println "reader: " (. readers next)))))

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

(defn min-fn
  ([x y]
   (if (> y 0)
     (min x y)
     (if (> x 0)
       x
       Float/MAX_VALUE)))
  ([x y & more]
   (reduce min-fn (min-fn x y) more)))

(defn -main
  "The Main."
  [& args]
  (println "GeoTIFF2STL!" args)
  ;(dostuff)
  (let [^String filename (first args)
        multiplier (Float/parseFloat (get (vec args) 1))
        z-lift (Float/parseFloat (get (vec args) 2))
        ^File file (File. filename)
        name (.getName file) ;
        ^BufferedImage image (try
                               (ImageIO/read file)
                               (catch Exception ex (print ex)))
        width (.getWidth image)
        _ (println "width" width)
        height (.getHeight image)
        _ (println "height" height)
        ^Raster raster (.getData image)
        hmap (make-array Float/TYPE width height)
        ;samples (for [w (range width) h (range height)]
        ;          (.getSample raster w h 0))
        ;points (make-array Float/TYPE (* (+ (* height width) width height) 6 3 2))
        ;stl (StlObject. name 0)
        ]

    (println "Calculating samples and min/max (z-lift: " z-lift ", mult: " multiplier ")")
    (let [keys (vec (for [w (range width) h (range height)]
                      [w h]))
          samples (vec (for [[w h] keys]
                         (let [s (.getSample raster w h 0)
                               s2 (.getRGB image w h)
                               s' (if (> s 0)
                                    (+ z-lift (* multiplier (/ s 100.0)))
                                    0)]
                           (println s2 s s')
                           s')))
          min (reduce min-fn samples)
          max (reduce max samples)]
      (println "min: " min "max: " max)
      (when (> min 10)
        (println "min is over 10, consider a lower z-lift value"))
      (println "Converting to java array")
      (doseq [i (range (count samples))]
        (let [[w h] (get keys i)
              sample (get samples i)]
          (aset-float hmap (- (- width 1) w) h
            sample))))

    ;(doseq [w (range width) h (range height)]
    ;  (let [s (.getSample raster w h 0)]
    ;    (aset-float minmax 0 (min-fn (aget minmax 0) s))
    ;    (aset-float minmax 1 (max (aget minmax 1) s))))

    ;(println "Loading elevation data")
    ;(doseq [w (range width) h (range height)]
    ;  (let [sample (.getSample raster w h 0)]
    ;    (aset-float hmap (- (- width 1) w) h
    ;                (if (> sample 0)
    ;                  (+ 10 (* 4 (/ sample 90)))
    ;                  0))))



    ;(println "Calculating min/max")
    ;(let [vecs (map #(into [] %) (into [] hmap))
    ;      min-max (reduce (fn [in val] {:min (min-fn (:min in) (apply min-fn val))
    ;                                    :max (max (:max in) (apply max val))})
    ;                      {:min Float/MAX_VALUE :max Float/MIN_VALUE} vecs)
    ;      ]
    ;  (println min-max))

    (println "Saving STL")
    (let [stl (StlObject/fromHeightmap name height width hmap)
          name (if (.contains name ".")
                 (.substring name 0 (.lastIndexOf name "."))
                 name)]
      (set! (. stl -path) (str name ".stl"))
      (.save stl StlObject/FILE_BINARY))

    ;(println "count loop-recur" (time (count (points-loop width height hmap))))
    ;(println "count for-concat" (time (count (points-concat width height hmap))))
    ;(println "count for-array" (time (count (points-array width height hmap))))

    ))
