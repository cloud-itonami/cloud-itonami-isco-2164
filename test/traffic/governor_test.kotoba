(ns traffic.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [traffic.store :as store]
            [traffic.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-site! st {:site-id "site-1" :location "Main & 5th" :jurisdiction "Downtown"})
    st))

(deftest ok-on-clean-scenario-draft
  (let [st (fresh-store)
        proposal {:op :draft-planning-scenario :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-site
  (let [st (fresh-store)
        proposal {:op :draft-planning-scenario :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:site-id "no-such-site"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-site (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :draft-planning-scenario :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-attempt-to-issue-zoning-change
  (let [st (fresh-store)
        proposal {:op :issue-zoning-change :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-binding-authority (:rule %)) (:violations v)))))

(deftest hard-on-attempt-to-set-traffic-regulation
  (let [st (fresh-store)
        proposal {:op :set-traffic-regulation :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-binding-authority (:rule %)) (:violations v)))))

(deftest hard-on-attempt-to-approve-development
  (let [st (fresh-store)
        proposal {:op :approve-development :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-binding-authority (:rule %)) (:violations v)))))

(deftest escalates-on-safety-concern-flag
  (let [st (fresh-store)
        proposal {:op :flag-public-safety-concern :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-safety-critical-system
  (let [st (fresh-store)
        proposal {:op :analyze-traffic-data :effect :propose :confidence 0.9 :stake :medium
                  :scope :intersection :tags #{:pedestrian-safety :traffic-signal}}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :analyze-traffic-data :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:site-id "site-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:site-id "site-1" :op :analyze-traffic-data})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "site-1"))))
    (is (= 1 (count (store/ledger st))))))
