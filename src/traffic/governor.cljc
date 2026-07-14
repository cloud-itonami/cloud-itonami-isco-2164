(ns traffic.governor
  "TrafficGovernor — the independent safety/traceability layer for
  the ISCO-08 2164 town and traffic planning actor. Wired as its own
  `:govern` node in `traffic.actor`'s StateGraph, downstream of
  `:advise` — the Advisor has no notion of site provenance, planning
  authority, or the planner's professional/legal responsibility, so this
  MUST be a separate system able to reject a proposal (itonami actor
  pattern, per ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. site provenance   — the request's site must be registered.
    2. no-actuation      — proposal :effect must be :propose.
    3. no-binding-authority — any attempt to issue a binding zoning change,
       set traffic regulations, approve a development, or otherwise bind
       the planning authority to a decision is a permanent block (those
       remain the human planner's/planning authority's exclusive responsibility).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: safety-critical proposals and public consultation
  always require human planner sign-off):
    4. :op :flag-public-safety-concern (always escalates).
    5. proposals affecting safety-critical systems (pedestrian crossings,
       emergency routes, intersections with high incident history).
    6. low confidence (< `confidence-floor`)."
  (:require [traffic.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:flag-public-safety-concern})
(def ^:private safety-critical-keywords #{:pedestrian-safety :emergency-route :intersection :traffic-signal :crossing})

(defn- hard-violations [{:keys [proposal]} site-record]
  (cond-> []
    (nil? site-record)
    (conj {:rule :no-site :detail "未登録 site"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

    (or (= :issue-zoning-change (:op proposal))
        (= :set-traffic-regulation (:op proposal))
        (= :approve-development (:op proposal)))
    (conj {:rule :no-binding-authority :detail "binding zoning changes, traffic regulations, and development approvals are the planning authority's exclusive responsibility"})))

(defn- safety-critical-system? [{:keys [scope tags]}]
  (and scope tags (some safety-critical-keywords tags)))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `traffic.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [site-record (store/site store (:site-id request))
        hard (hard-violations {:proposal proposal} site-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        escalating-op? (contains? escalating-ops (:op proposal))
        safety-critical? (and (not hard?) (safety-critical-system? proposal))]
    {:ok? (and (not hard?) (not low?) (not escalating-op?) (not safety-critical?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? escalating-op? safety-critical?))}))
