(ns authenticated-compojure-api.auth-resources.token-auth-backend
  (:require [environ.core :refer [env]]
            [buddy.auth.backends.token :refer [jws-backend]]))

;; ============================================================================
;  Tokens are valid for fifteen minutes after creation. If token is valid the
;  decoded contents of the token will be added to the request with the keyword
;  of :identity
;; ============================================================================
(def token-backend (jws-backend {:secret (env :auth-key) :options {:alg :hs512}}))
