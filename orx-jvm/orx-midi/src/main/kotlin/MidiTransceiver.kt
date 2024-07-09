package org.openrndr.extra.midi

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.Program
import org.openrndr.events.Event
import javax.sound.midi.*

private val logger = KotlinLogging.logger {  }

@JvmRecord
data class MidiDeviceName(val name: String, val vendor: String)

class MidiDeviceCapabilities {
    var receive: Boolean = false
    var transmit: Boolean = false

    override fun toString(): String {
        return "MidiDeviceCapabilities(receive=$receive, transmit=$transmit)"
    }
}

@JvmRecord
data class MidiDeviceDescription(
    val name: String,
    val vendor: String,
    val receive: Boolean,
    val transmit: Boolean
) {
    companion object {
        fun list(): List<MidiDeviceDescription> {
            val caps = mutableMapOf<MidiDeviceName, MidiDeviceCapabilities>()

            val infos = MidiSystem.getMidiDeviceInfo()
            for (info in infos) {
                val device = MidiSystem.getMidiDevice(info)
                val name = MidiDeviceName(info.name, info.vendor)
                val deviceCaps =
                    caps.getOrPut(name) { MidiDeviceCapabilities() }

                if (device !is Sequencer && device !is Synthesizer) {
                    if (device.maxReceivers != 0 && device.maxTransmitters == 0) {
                        deviceCaps.receive = true
                    }
                    if (device.maxTransmitters != 0 && device.maxReceivers == 0) {
                        deviceCaps.transmit = true
                    }
                }
            }
            return caps.map {
                MidiDeviceDescription(
                    it.key.name,
                    it.key.vendor,
                    it.value.receive,
                    it.value.transmit
                )
            }
        }
    }

    fun open(program: Program): MidiTransceiver {
        require(receive && transmit) {
            "MIDI device should be a receiver and transmitter"
        }

        return MidiTransceiver.fromDeviceVendor(program, name, vendor)
    }
}

class MidiTransceiver(program: Program, val receiverDevice: MidiDevice?, val transmitterDevicer: MidiDevice?) {
    companion object {
        fun fromDeviceVendor(program: Program, name: String, vendor: String? = null): MidiTransceiver {
            val infos = MidiSystem.getMidiDeviceInfo()

            var receiverDevice: MidiDevice? = null
            var transmitterDevice: MidiDevice? = null

            for (info in infos) {
                try {
                    val device = MidiSystem.getMidiDevice(info)
                    if (device !is Sequencer && device !is Synthesizer) {
                        if ((vendor == null || info.vendor == vendor) && info.name == name) {
                            logger.info { "found matching MIDI device $name / $vendor" }
                            if (device.maxTransmitters != 0 && device.maxReceivers == 0) {
                                transmitterDevice = device
                                logger.debug {
                                    "found MIDI transmitter"
                                }
                            }
                            if (device.maxReceivers != 0 && device.maxTransmitters == 0) {
                                receiverDevice = device
                                logger.debug {
                                     "found MIDI receiver"
                                }
                            }
                        }
                    }
                } catch (e: MidiUnavailableException) {
                    error("no MIDI available")
                }
            }

            if (receiverDevice != null && transmitterDevice != null) {
                receiverDevice.open()
                transmitterDevice.open()
                return MidiTransceiver(program, receiverDevice, transmitterDevice)
            } else {
                error("MIDI device not found ${name}:${vendor} $receiverDevice $transmitterDevice")
            }
        }
    }

    private val receiver = receiverDevice?.receiver
    private val transmitter = transmitterDevicer?.transmitter

    private inner class Destroyer : Thread() {
        override fun run() {
            destroy()
        }
    }

    private fun trigger(message: MidiMessage) {
        val cmd = message.message
        val channel = (cmd[0].toInt() and 0xff) and 0x0f
        when (val eventType = message.eventType) {

            MidiEventType.NOTE_ON -> {
                val key = cmd[1].toInt() and 0xff
                val velocity = cmd[2].toInt() and 0xff
                if (velocity > 0) {
                    noteOn.trigger(MidiEvent.noteOn(channel, key, velocity))
                } else {
                    noteOff.trigger(MidiEvent.noteOff(channel, key, velocity))
                }
            }

            MidiEventType.NOTE_OFF -> noteOff.trigger(
                MidiEvent.noteOff(
                    channel,
                    cmd[1].toInt() and 0xff,
                    cmd[2].toInt() and 0xff
                )
            )

            MidiEventType.CONTROL_CHANGE -> controlChanged.trigger(
                MidiEvent.controlChange(
                    channel,
                    cmd[1].toInt() and 0xff,
                    cmd[2].toInt() and 0xff
                )
            )

            MidiEventType.PROGRAM_CHANGE -> programChanged.trigger(
                MidiEvent.programChange(
                    channel,
                    cmd[1].toInt() and 0xff
                )
            )

            MidiEventType.CHANNEL_PRESSURE -> channelPressure.trigger(
                MidiEvent.channelPressure(
                    channel,
                    cmd[1].toInt() and 0xff
                )
            )

            // https://sites.uci.edu/camp2014/2014/04/30/managing-midi-pitchbend-messages/
            // The next operation to combine two 7bit values
            // was verified to give the same results as the Linux
            // `midisnoop` program while using an `Alesis Vortex
            // Wireless 2` device. This MIDI device does not provide a
            // full range 14 bit pitch-bend resolution though, so
            // a different device is needed to confirm the pitch bend
            // values slide as expected from -8192 to +8191.
            MidiEventType.PITCH_BEND -> pitchBend.trigger(
                MidiEvent.pitchBend(
                    channel,
                    (cmd[2].toInt() shl 25 shr 18) + cmd[1].toInt()
                )
            )

            else -> {
                logger.trace { "Unsupported MIDI event type: $eventType" }
            }

        }
    }

    init {
        transmitter?.receiver = object : MidiDeviceReceiver {
            override fun getMidiDevice(): MidiDevice? {
                return null
            }
            override fun send(message: MidiMessage, timeStamp: Long) {
                trigger(message)
            }
            override fun close() {
            }
        }

        val destroyer = Destroyer()
        program.ended.listen {
            destroyer.start()
        }

    }

    val controlChanged = Event<MidiEvent>("midi-transceiver::controller-changed")
    val programChanged = Event<MidiEvent>("midi-transceiver::program-changed")
    val noteOn = Event<MidiEvent>("midi-transceiver::note-on")
    val noteOff = Event<MidiEvent>("midi-transceiver::note-off")
    val channelPressure = Event<MidiEvent>("midi-transceiver::channel-pressure")
    val pitchBend = Event<MidiEvent>("midi-transceiver::pitch-bend")

    fun controlChange(channel: Int, control: Int, value: Int) {
        send { ShortMessage(ShortMessage.CONTROL_CHANGE, channel, control, value) }
    }

    fun programChange(channel: Int, program: Int) {
        send { ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program) }
    }

    fun noteOn(channel: Int, key: Int, velocity: Int) {
        send { ShortMessage(ShortMessage.NOTE_ON, channel, key, velocity) }
    }

    fun noteOff(channel: Int, key: Int, velocity: Int) {
        send { ShortMessage(ShortMessage.NOTE_OFF, channel, key, velocity) }
    }

    fun channelPressure(channel: Int, value: Int) {
        send { ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, value) }
    }

    fun pitchBend(channel: Int, value: Int) {
        send { ShortMessage(ShortMessage.PITCH_BEND, channel, value) }
    }

    fun destroy() {
        receiverDevice?.close()
        transmitterDevicer?.close()
    }

    private fun send(block: () -> MidiMessage) {
        if (receiver != null && receiverDevice != null) {
            try {
                val msg = block()
                receiver.send(msg, receiverDevice.microsecondPosition)
            } catch (e: InvalidMidiDataException) {
                logger.warn { e.message }
            }
        }
    }

}

/**
 * List all available MIDI devices
 * @since 0.4.3
 */
fun listMidiDevices() = MidiDeviceDescription.list()

/**
 * Open a MIDI device by name
 * @param name the name of the MIDI device to open. Either the
 * exact name or the first characters of the name.
 * Throws an exception if the device name is not found.
 * @since 0.4.3
 */
fun Program.openMidiDevice(name: String) =
    openMidiDeviceOrNull(name) ?: error("MIDI device not found for query '$name'")

/**
 * Open a MIDI device by name
 *
 * @param name the name of the MIDI device to open. Either the
 * exact name or the first characters of the name.
 * Returns null if the device name is not found.
 * @since 0.4.3
 */
fun Program.openMidiDeviceOrNull(name: String): MidiTransceiver? {
    val devices = listMidiDevices()

    val matchingDevice = devices.firstOrNull {
        // Existing device name matches `name`
        it.name == name
    } ?: devices.firstOrNull {
        // Existing device name starts with `name`
        it.name.startsWith(name)
    }

    return if(matchingDevice != null)
        MidiTransceiver.fromDeviceVendor(this, matchingDevice.name)
    else
        null
}

/**
 * Open a dummy MIDI device
 *
 * Enables running programs that depend on a specific MIDI device
 * when that device is not available.
 * Usage: `val dev = openMidiDeviceOrNull("Twister") ?: dummyMidiDevice()`
 * @since 0.4.3
 */
fun Program.dummyMidiDevice() = MidiTransceiver(this, null, null)
