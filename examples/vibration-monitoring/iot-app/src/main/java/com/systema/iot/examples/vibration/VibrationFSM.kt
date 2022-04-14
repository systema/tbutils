import com.systema.iot.examples.vibration.VibrationDeviceStateModel
import io.jumpco.open.kfsm.async.asyncStateMachine


enum class ToolState { Productive, UnscheduledDown, Maintenance, Restarting }
enum class ToolEvent { MaxVibrationExceeded, UnscheduledTooldown, MaintenanceStarted, MaintenanceComplete }


val fsmDefinition = asyncStateMachine(
    ToolState.values().toSet(),
    ToolEvent.values().toSet(),
    VibrationDeviceStateModel::class
) {

    // TODO implement better state initialization
    initialState {
        ToolState.Productive
    }

    default {
        onEntry { fromState, targetState, arg ->
            logStatus()
        }
    }

    // define state-machine
    whenState(ToolState.Productive) {
        onEvent(ToolEvent.UnscheduledTooldown to ToolState.UnscheduledDown) {
            logger.info("Failed to do preventive maintenance. Tool went into unscheduled down")
        }
        onEvent(ToolEvent.MaxVibrationExceeded to ToolState.Maintenance) {
            logger.info("Max vibration exceeded. Starting maintenance.")
        }
    }

    whenState(ToolState.UnscheduledDown) {
        // apply additional timeout penalty
        timeout(ToolState.Maintenance, 6000) {
            logger.info { "UnscheduledDown, waiting for tool engineer..." }
        }
    }

    whenState(ToolState.Maintenance) {
        timeout(ToolState.Restarting, 5000) {
            repairCompleted()
        }

//        onExit{fromState, targetState, arg ->
//        }
        onEvent(ToolEvent.MaxVibrationExceeded to ToolState.Maintenance) {
            logger.info("Max vibration exceeded while performing maintenance.")

        }

        onEvent(ToolEvent.MaintenanceComplete to ToolState.Restarting) {
            logger.info("Finished maintenance")
        }
    }


    whenState(ToolState.Restarting) {
        timeout(ToolState.Productive, 3000) {
            logger.info{ "Tool coming back online after maintenance."}
        }
    }

}.build()


// TODO visualize state-machine
//fun main() {
//    val visualization = visualize(fsmDefinition)
//    File("generated", "turnstile.plantuml").writeText(plantUml(visualization))
//}
