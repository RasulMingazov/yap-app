package app.yap.server.feature.scenario.model

sealed class ScenarioFailure : RuntimeException() {

    class MalformedInput : ScenarioFailure()

    class Unauthorized : ScenarioFailure()

    class NotFound : ScenarioFailure()

    class AccessRequired : ScenarioFailure()

    class SlotLimitReached : ScenarioFailure()

    class NotRepeatable : ScenarioFailure()
}
