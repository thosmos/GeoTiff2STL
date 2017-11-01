(ns hellogeotiff
  (:gen-class)
  (:import
    [com.heightmap.stl StlObject]
    [org.gdal.gdalconst gdalconstConstants]
    [org.gdal.gdal gdal Dataset Driver Band])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn exit [status msg]
  (println msg)
  (println "EXITING" status)
  (System/exit status))

(defn reader-init []
  (try (gdal/AllRegister)
       (catch Exception ex (exit 1 (str "Is GDAL installed and working?\n" (.getMessage ex))))))

(defn ^String getDataType [v]
  (case v
    0 "Unknown"
    1 "Byte"
    2 "UInt16"
    3 "Int16"
    4 "UInt32"
    5 "Int32"
    6 "Float32"
    7 "Float64"
    8 "CInt16"
    9 "CInt32"
    10 "CFloat32"
    11 "CFloat64"
    12 "CFloat64"))

(def cli-options
  ;; An option with a required argument
  [["-m" "--multiplier MULT" "Height Multiplier"
    :id :multiplier
    :default 4.0
    :parse-fn #(Float/parseFloat %)
    :validate [#(< 0.0 % 20.0) "Must be a number between 0 and 20"]]
   ;; A non-idempotent option
   ["-z" "--zlift ZLIFT" "Z Lift/Reduce"
    :id :z-lift
    :default 0.0
    :parse-fn #(Float/parseFloat %)]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Welcome to GeoTIFF2STL. May it be of use to you."
        ""
        "Usage: java -jar GeoTIFF2STL.jar [options] path-to-geotiff-file"
        ""
        "Options:"
        options-summary]
    (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
    (string/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (or (< (count arguments) 1)
        (not (.exists (io/as-file (first arguments)))))
      {:exit-message (str (usage summary) \newline \newline "Where's the GeoTIFF file?")}
      (and (= 1 (count arguments))
        (.exists (io/as-file (first arguments))))
      {:filename (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

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

  (reader-init)

  (let [{:keys [filename options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [^String name (.getName (io/as-file filename))
            ^float multiplier (:multiplier options)
            ^float z-lift (:z-lift options)
            ^Dataset ds (gdal/Open filename gdalconstConstants/GA_ReadOnly)
            ^Driver drv (.GetDriver ds)
            x (.getRasterXSize ds)
            y (.getRasterYSize ds)
            hmap (make-array Float/TYPE x y)
            ^Band band (.GetRasterBand ds 1)
            ^doubles minMax (double-array 2)
            _ (.ComputeRasterMinMax band minMax)
            r-min (aget minMax 0)
            r-max (aget minMax 1)
            sum-start {:min Float/MAX_VALUE :max Float/MIN_VALUE}
            sum-fn (fn [sum next]
                     {:min (min-fn (:min sum) next) :max (max (:max sum) next)})
            no-data-arr (into-array (make-array Double/TYPE 1)) ;; a hack to make a [Ljava.lang.Double type
            _ (.GetNoDataValue band no-data-arr) ;; requires a [Ljava.lang.Double not a [D
            no-data-val (aget no-data-arr 0)]

        (println "GDAL Ver. " (gdal/VersionInfo))
        (println (str "Driver: " (.getShortName drv) "/" (.getLongName drv)))
        (println (str "Width: " x ", Height:" y "\nMin: " r-min ", Max: " r-max))
        (println (str "DataType: " (getDataType (.getDataType band)) "\n"
                   "NoData Value: " no-data-val))

        (println "Reading raster.")
        (doseq [^int i (range x)]
          (let [^floats arr (float-array y)
                xi (- x 1 i)] ; flip the order of columns (workaround for StlObject)
            (.ReadRaster band i 0 1 y arr)
            (aset hmap xi arr)))
        (println "Done reading raster.  \nParsing data.")
        (let [sum (reduce sum-fn sum-start
                    (for [i (range x)
                          j (range y)]
                      (let [v (float (+ z-lift (* (aget hmap i j) multiplier 0.01)))
                            v' (if (> v r-max) 0.0 v) ; fix for nodata values that are Float/MAX_VALUE
                            v' (float (max 0.0 v'))]
                        ;; use the zeroed value for the build
                        (aset hmap i j v')
                        (when (= (mod i 10) j 0)
                          (print ".") (flush))
                        v')))]
          (println "\nDone parsing: Modified Min: " (:min sum) ", Max: " (:max sum)))
        (println "Saving STL")
        (let [stl (StlObject/fromHeightmap name y x hmap)
              name (if (.contains name ".")
                     (.substring name 0 (.lastIndexOf name "."))
                     name)]
          (set! (. stl -path) (str name ".stl"))
          (.save stl StlObject/FILE_BINARY))))))
