import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder
import simulations.AccountLoadTest

object Engine {
    @JvmStatic
    fun main(args: Array<String>) {
        val props =
            GatlingPropertiesBuilder()
                .resourcesDirectory(IDEPathHelper.gradleResourcesDirectory.toString())
                .resultsDirectory(IDEPathHelper.resultsDirectory.toString())
                .binariesDirectory(IDEPathHelper.gradleBinariesDirectory.toString())
                .simulationClass(AccountLoadTest::class.java.name)
        Gatling.fromMap(props.build())
    }
}
