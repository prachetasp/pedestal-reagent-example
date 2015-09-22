(ns reagent-example.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.log :as log]
            [reagent-example.layout :as layout]
            [ring.util.response :as ring-resp]))

(defn about-page [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn home-page [request]
  (ring-resp/response "Sup Bro!"))

(defn render-layout [path]
  (layout/render-file
    (->> (str path ".html") seq rest (apply str))))

(def wrap-ring-resp
  (interceptor
    {:name ::wrap-ring-resp
     :leave
     (fn [context]
       (merge context {:response (ring-resp/response (:response context))
                       :content-type (if (= (:request-method context) :post)
                                       "application/edn; charset=UTF-8"
                                       "text/html; charset=UTF-8")}))}))

(defn app-form [context]
  (render-layout (:uri context)))

(defn save-app-form [context]
  (log/info :msg (:edn-params context))
  {:status "ok"})

(defroutes routes
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[["/" {:get home-page}
     ^:interceptors [(body-params/body-params) bootstrap/html-body]
     #_["/about" {:get about-page}]
     ["/app" ^:interceptors [wrap-ring-resp]
      {:get app-form
       :post save-app-form}]]]])

;; Consumed by reagent-example.server/create-server
;; See bootstrap/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :jetty
              ;;::bootstrap/host "localhost"
              ::bootstrap/port 8080})
