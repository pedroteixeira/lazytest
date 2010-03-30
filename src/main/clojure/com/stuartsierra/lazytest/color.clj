(ns com.stuartsierra.lazytest.color)

;; Mostly stolen from Stuart Halloway's circumspec

(defn colorize?
  "Colorize output, true if system property
  com.stuartsierra.lazytest.colorize is true (default)"
  [] (contains? #{"yes" "true"}
                (System/getProperty
                 "com.stuartsierra.lazytest.colorize"
                 "true")))

(defn set-colorize
  "Set the colorize? property to true or false."
  [bool]
  (assert (instance? Boolean bool))
  (System/setProperty "com.stuartsierra.lazytest.colorize"
                      (str bool)))

(def #^{:doc "ANSI color code table"}
     color-table
     {:reset "[0m"
      :bold-on "[1m"
      :italic-on "[3m"
      :underline-on "[4m"
      :inverse-on "[7m"
      :strikethrough-on "[9m"
      :bold-off "[22m"
      :italic-off "[23m"
      :underline-off "[24m"
      :inverse-off "[27m"
      :strikethrough-off "[29m"
      :fg-black "[30m"
      :fg-red "[31m"
      :fg-green "[32m"
      :fg-yellow "[33m"
      :fg-blue "[34m"
      :fg-magenta "[35m"
      :fg-cyan "[36m"
      :fg-white "[37m"
      :fg-default "[39m"
      :bg-black "[40m"
      :bg-red "[41m"
      :bg-green "[42m"
      :bg-yellow "[43m"
      :bg-blue "[44m"
      :bg-magenta "[45m"
      :bg-cyan "[46m"
      :bg-white "[47m"
      :bg-default "[49m"})

(defn ansi-color-str
  "Return ANSI color codes for the given sequence of colors, which are
  keywords in color-table."
  [& colors]
  (apply str (map (fn [c] (str (char 27) (color-table c))) colors)))

(defn colorize
  "Wrap string s in ANSI colors if colorize? is true."
  [s & colors]
  (if (and (colorize) (seq s))
    (str (ansi-color-string colors) (ansi-color-string :reset))
    s))