(ns io-utils
    (:import (java.io ObjectInputStream
                      ObjectOutputStream
                      BufferedOutputStream
                      ByteArrayInputStream
                      ByteArrayOutputStream)
            (java.net InetSocketAddress)
            (java.nio ByteBuffer)
            (java.nio.channels DatagramChannel)))

(use 'clojure.stacktrace)

(def ^:const console-port 50000)

(defn marshall [data]
    (let [baos (ByteArrayOutputStream.)
          os (ObjectOutputStream. (BufferedOutputStream. baos))]
          (.writeObject os (doall data))
          (.flush os)
          (.toByteArray baos)))

(defn unmarshall [byte-buffer]
    (let [bais (ByteArrayInputStream. byte-buffer)
          is (ObjectInputStream. bais)]
          (.readObject is)))

(def max-packet-size (* 64 1024))

(defn create-server-datagram-channel [port]
    (let [channel (DatagramChannel/open)
          byte-buf (ByteBuffer/allocate max-packet-size)]
        (.configureBlocking channel false)
        (.bind (.socket channel) (InetSocketAddress. port))
        {:channel channel
         :byte-buf byte-buf}))

(defn create-client-datagram-channel [host port]
    (let [channel (DatagramChannel/open)
          byte-buf (ByteBuffer/allocate max-packet-size)]
        (.configureBlocking channel false)
        {:host-sock-addr (InetSocketAddress. host port)
         :channel channel
         :byte-buf byte-buf}))

(defn close-datagram-channel [datagram-channel]
    (.close (:channel datagram-channel)))

(defn do-send-to [datagram-channel addr data]
    ;(println "send-to: data size:" (count data))
    ;(if (> (count data) 3000) (print-stack-trace (Exception. "asdf")))
    (let [byte-buf (:byte-buf datagram-channel)]
        (-> byte-buf
            (.clear)
            (.putInt (count data))
            (.put data)
            (.putLong (.getTime (new java.util.Date)))
            (.flip))
        (.send (:channel datagram-channel) byte-buf addr)))

(defn send-to [datagram-channel addr data]
    ;(println "send at time:" (mod (.getTime (new java.util.Date)) 100000))
    (let [start-time (.getTime (new java.util.Date))
          ret-val (do-send-to datagram-channel addr data)
          end-time (.getTime (new java.util.Date))]
        (println "send-to time  :" (- end-time start-time) start-time end-time)
        ret-val))

(defn send-to-server [datagram-channel data]
    (send-to datagram-channel (:host-sock-addr datagram-channel) data))

(defn convert-newly-read-byte-buffer-to-byte-array [byte-buf]
    (.flip byte-buf)
    (let [msg-size (.getInt byte-buf)
          ;_ (println "convert: msg-size:" msg-size)
          data (byte-array msg-size)]
        (.get byte-buf data)
        (let [send-time (.getLong byte-buf)
              transit-time (- (.getTime (new java.util.Date)) send-time)]
            (println "transit time:" transit-time))
        data))

(defn do-recv-from [datagram-channel]
    (let [byte-buf (:byte-buf datagram-channel)
          _ (.clear byte-buf)
          sock-addr (.receive (:channel datagram-channel) byte-buf)]
        (if sock-addr
            (do ;(println "recv at time:" (mod (.getTime (new java.util.Date)) 100000))
                {:addr sock-addr
                 :data (convert-newly-read-byte-buffer-to-byte-array byte-buf)}))))
        ; (if sock-addr {:addr sock-addr
        ;                :data (convert-newly-read-byte-buffer-to-byte-array byte-buf)})))

(defn recv-from [datagram-channel]
    ;(println "recv at time:" (mod (.getTime (new java.util.Date)) 100000))
    (let [start-time (.getTime (new java.util.Date))
          ret-val (do-recv-from datagram-channel)
          end-time (.getTime (new java.util.Date))]
        (if ret-val (println "recv-from time:" (- end-time start-time) start-time end-time))
        ret-val))
