;; nbb テストランナー（第一経路。JVM 互換は clojure -M:test）
;;   nbb --classpath src:test test/run.cljs
(ns run
  (:require [clojure.test :as t]
            [kagaku.scenario-test]
            [kagaku.simcase-test]
            [kagaku.factcheck-test]
            [kagaku.rom-test]
            [kagaku.script-test]
            [kagaku.derived-test]
            [kagaku.voice-test]
            [kagaku.scene-test]
            [kagaku.compose-test]
            [kagaku.audit-test]
            [kagaku.preview-test]
            [kagaku.audio-test]))

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (when-not (t/successful? m)
    (set! (.-exitCode js/process) 1)))

(t/run-tests 'kagaku.scenario-test 'kagaku.simcase-test 'kagaku.factcheck-test 'kagaku.rom-test 'kagaku.script-test 'kagaku.derived-test 'kagaku.voice-test 'kagaku.scene-test 'kagaku.compose-test 'kagaku.audit-test 'kagaku.preview-test 'kagaku.audio-test)
