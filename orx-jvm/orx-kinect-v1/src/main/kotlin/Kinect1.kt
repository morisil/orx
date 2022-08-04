package org.openrndr.extra.kinect.v1

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.libfreenect.*
import org.bytedeco.libfreenect.global.freenect.*
import org.bytedeco.libfreenect.presets.freenect
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.kinect.*
import org.openrndr.launch
import org.openrndr.math.IntVector2
import org.openrndr.resourceUrl
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class Kinect1Exception(msg: String) : KinectException(msg)

class Kinect1 : Kinect, Extension {

    override var enabled: Boolean = true

    /**
     * Defaults to 100 ms. Delay seems to be necessary due to
     * either my misunderstanding or some weird freenect bug.
     *
     * Without the delay between starting depth camera and
     * registering depth callback, no frames are transferred
     * at all. However this problem happens only on the first
     * try with freshly connected kinect.
     * Subsequent runs of the same program don't require
     * this delay at all.
     */
    // TODO is it still needed?
    var depthCameraInitializationDelay: Long = 100

    class DeviceInfo(
        override val serialNumber: String,
    ) : Kinect.Device.Info {
        override fun toString(): String {
            return "Kinect1[serial=$serialNumber]"
        }
    }

    /**
     * Sub-devices to open when [openDevice] is called.
     */
    enum class SubDevice(val code: Int) {

        MOTOR(FREENECT_DEVICE_MOTOR),
        CAMERA(FREENECT_DEVICE_CAMERA),
        AUDIO(FREENECT_DEVICE_AUDIO);

        companion object {
            val allSubDevices = setOf(MOTOR, CAMERA, AUDIO)
        }

    }

    /**
     * Log level for native freenect logging.
     *
     * @param code the code of corresponding freenect log level.
     */
    enum class LogLevel(val code: Int) {

        /** Crashing/non-recoverable errors. */
        FATAL(FREENECT_LOG_FATAL),

        /** Major errors. */
        ERROR(FREENECT_LOG_ERROR),

        /** Warning messages. */
        WARNING(FREENECT_LOG_WARNING),

        /** Important messages. */
        NOTICE(FREENECT_LOG_NOTICE),

        /** Log for normal messages. */
        INFO(FREENECT_LOG_INFO),

        /** Log for useful development messages. */
        DEBUG(FREENECT_LOG_DEBUG),

        /** Log for slightly less useful messages. */
        SPEW(FREENECT_LOG_SPEW),

        /** Log EVERYTHING. May slow performance. */
        FLOOD(FREENECT_LOG_FLOOD);

    }

    var logLevel: LogLevel
        get() = freenect.logLevel
        set(value) { freenect.logLevel = value }

    private val logger = KotlinLogging.logger {}

    private lateinit var program: Program
    private lateinit var depthToRawNormalizedMapper: DepthToRawNormalizedMapper
    private lateinit var depthToMetersMapper: Filter

    private lateinit var freenect: Freenect


    override fun setup(program: Program) {
        if (!enabled) { return }
        logger.info("Starting kinect1 support")
        this.program = program
        depthToRawNormalizedMapper = DepthToRawNormalizedMapper()
        depthToRawNormalizedMapper.maxDepthValue = 2047.0
        depthToMetersMapper = Filter(
            filterShaderFromUrl(
                resourceUrl(
                    "kinect1-depth-to-meters-mapper.frag",
                    Kinect1::class
                )
            )
        )
        freenect = Freenect(initialLogLevel = LogLevel.INFO)
    }

    override fun listDevices(): List<DeviceInfo> = freenect.callBlocking("listDevices") { _, _ ->
        freenect.listDevices()
    }

    override fun openDevice(index: Int): Kinect1.V1Device =
        openDevice(index, SubDevice.allSubDevices)

    /**
     * Starts kinect device of a given index with all the specified [SubDevice]s.
     *
     * Note: [subDevices] defaults to empty set, which will open all
     * the [SubDevice]s.
     *
     * Freenect docs: In particular, this allows libfreenect
     * to grab only a subset of the devices in the Kinect,
     * so you could (for instance) use libfreenect to handle audio
     * and motor support while letting OpenNI have access to the
     * cameras. If a device is not supported on a particular
     * platform, its flag will be ignored.
     *
     * @param index the kinect device index (starts with 0). If no value specified,
     *          it will default to 0.
     * @param subDevices the set of sub devices to open as well.
     * @throws Kinect1Exception if device of such an index does not exist,
     *          or it was already started.
     * @see listDevices
     */
    fun openDevice(index: Int, subDevices: Set<SubDevice>): V1Device {
        val result = freenect.callBlocking("openDeviceByIndex") { ctx, _ ->
            val devices = freenect.listDevices()
            if (devices.isEmpty()) {
                throw KinectException("No kinect devices detected, cannot open any")
            } else if (index >= devices.size) {
                throw KinectException("Invalid device index, number of kinect1 devices: ${devices.size}")
            }
            Pair(
                openFreenectDevice(
                    ctx,
                    devices[index].serialNumber,
                    subDevices
                ),
                devices[index]
            )
        }
        val device = V1Device(result.first, result.second, subDevices)
        mutableActiveDevices.add(device)
        return device
    }

    override fun openDevice(serialNumber: String): V1Device =
        openDevice(serialNumber, SubDevice.allSubDevices)

    fun openDevice(serialNumber: String, subDevices: Set<SubDevice>): V1Device {
        val dev = freenect.callBlocking("openDeviceBySerial") { ctx, _ ->
            openFreenectDevice(ctx, serialNumber, subDevices)
        }
        val device = V1Device(dev, DeviceInfo(serialNumber), subDevices)
        mutableActiveDevices.add(device)
        return device
    }

    private val mutableActiveDevices = LinkedList<V1Device>()

    override val activeDevices: List<Kinect.Device>
        get() = mutableActiveDevices

    private fun openFreenectDevice(
        ctx: freenect_context,
        serialNumber: String,
        subDevices: Set<SubDevice>
    ): freenect_device {
        val dev = freenect_device()
        // TODO fix subdevices
        //freenect_select_subdevices(ctx, subDevices.toFreenectExpression())
        freenect.checkReturn(
            freenect_open_device_by_camera_serial(ctx, dev, serialNumber)
        )
        return dev
    }

    override fun shutdown(program: Program) {
        if (!enabled) { return }
        logger.info { "Shutting down Kinect1 support" }
        logger.debug("Closing active devices, count: ${mutableActiveDevices.size}")
        mutableActiveDevices.forEach {
            it.close()
        }
        mutableActiveDevices.clear()
        freenect.close()
    }

    fun executeInFreenectContext(
        name: String,
        block: (ctx: freenect_context, usbCtx: freenect_usb_context) -> Unit
    ) {
        freenect.call(name) { ctx, usbCtx ->
            block(ctx, usbCtx)
        }
    }

    fun <T> executeInFreenectContextBlocking(
        name: String,
        block: (ctx: freenect_context, usbCtx: freenect_usb_context) -> T
    ): T = freenect.callBlocking(name) { ctx, usbCtx ->
        block(ctx, usbCtx)
    }

    inner class V1Device(
        private val dev: freenect_device,
        override val info: DeviceInfo,
        val activeSubDevices: Set<SubDevice>
    ) : Kinect.Device {

        private lateinit var depthMapper: Filter

        inner class V1DepthCamera(
            override val resolution: IntVector2,
        ) : KinectDepthCamera {

            private val enabledState = AtomicBoolean(false)

            private var bytesIn = kinectRawDepthByteBuffer(resolution)
            private var bytesOut = kinectRawDepthByteBuffer(resolution)
            private val bytesFlow = MutableStateFlow(bytesOut)

            private val rawBuffer = colorBuffer(
                resolution.x,
                resolution.y,
                format = ColorFormat.R,
                type = ColorType.UINT16_INT
            ).also {
                it.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
            }

            private val processedFrameBuffer = colorBuffer(
                resolution.x,
                resolution.y,
                format = ColorFormat.R,
                type = ColorType.FLOAT16 // in the future we might want to choose the precision here
            )

            private var mutableCurrentFrame = processedFrameBuffer

            private var depthMapper = depthToRawNormalizedMapper

            // TODO add cancelation
            private lateinit var frameFlowChannel: SendChannel<ColorBuffer>

            private val mutableFrameFlow = MutableSharedFlow<ColorBuffer>()

            override val currentFrame get() = mutableCurrentFrame

            override val frameFlow: Flow<ColorBuffer> = mutableFrameFlow

            init {
                program.launch {
                    bytesFlow.collect { bytes ->
                        rawBuffer.write(bytes)
                        if (depthMeasurement != DepthMeasurement.RAW) {
                            depthMapper.apply(rawBuffer, processedFrameBuffer)
                        }
                        mutableFrameFlow.emit(mutableCurrentFrame)
                    }
                }
            }

            private val freenectDepthCallback = object : freenect_depth_cb() {
                override fun call(
                    dev: freenect_device,
                    depth: Pointer,
                    timestamp: Int
                ) {
                    bytesFlow.tryEmit(bytesOut)
                    val bytesTmp = bytesOut
                    bytesOut = bytesIn
                    bytesIn = bytesTmp
                    freenect.checkReturn(
                        freenect_set_depth_buffer(dev, Pointer(bytesIn))
                    )
                }
            }

            override var enabled: Boolean
                get() = enabledState.get()
                set(value) {
                    freenect.callBlocking("depthCameraEnable[$value]") { _, _ ->
                        freenect.expectingEvents = value
                        if (enabledState.get() != value) {
                            if (value) {
                                start()
                            } else {
                                stop()
                            }
                            enabledState.set(value)
                        }
                    }
                }

            private var flipHState: Boolean = false

            override var depthMeasurement: DepthMeasurement =
                DepthMeasurement.RAW_NORMALIZED

            private fun Boolean.toFreenectFlagValue(): Int =
                if (this) FREENECT_ON
                else FREENECT_OFF

            override var flipH: Boolean
                get() = flipHState
                set(value) {
                    freenect.call("setMirrorDepth") { _, _ ->
                        freenect_set_flag(dev, FREENECT_MIRROR_DEPTH, value.toFreenectFlagValue())
                        flipHState = value
                    }
                }

            override var flipV: Boolean
                get() = rawBuffer.flipV
                set(value) {
                    rawBuffer.flipV = value
                }

            private fun start() {
                logger.info { "Starting depth camera, device: $info" }
                freenect.checkReturn(freenect_set_depth_mode(
                    dev, freenect_find_depth_mode(FREENECT_RESOLUTION_MEDIUM, FREENECT_DEPTH_11BIT))
                )
                freenect.checkReturn(freenect_set_depth_buffer(dev, Pointer(bytesIn)))
                freenect.checkReturn(freenect_start_depth(dev))
                Thread.sleep(depthCameraInitializationDelay) // here is the hack
                freenect_set_depth_callback(dev, freenectDepthCallback)
                if (flipHState) {
                    freenect_set_flag(dev, FREENECT_MIRROR_DEPTH, FREENECT_ON)
                }
            }

            private fun stop() {
                logger.info { "Stopping depth camera, device: $info" }
                freenect.checkReturn(freenect_stop_depth(dev))
            }

            internal fun shutdown() {
                if (!enabled) {
                    return
                }
                frameFlowChannel.close()
            }

            private fun verifyOnShutdown(ret: Int) {
                if (ret != 0) {
                    logger.error { "Unexpected return value while shutting down Kinect1 support: $ret" }
                }
            }

        }

        override val depthCamera: V1DepthCamera = V1DepthCamera(
            resolution = IntVector2(640, 480)
        )

        fun executeInFreenectDeviceContext(
            name: String,
            block: (ctx: freenect_context, usbCtx: freenect_usb_context, dev: freenect_device) -> Unit
        ) {
            freenect.call(name) { ctx, usbCtx ->
                block(ctx, usbCtx, dev)
            }
        }

        fun <T> executeInFreenectDeviceContextBlocking(
            name: String,
            block: (ctx: freenect_context, usbCtx: freenect_usb_context, dev: freenect_device) -> T
        ): T = freenect.callBlocking(name) { ctx, usbCtx ->
            block(ctx, usbCtx, dev)
        }

        override fun close() {
            logger.info { "Closing device: $info" }
            depthCamera.enabled = false
            freenect.callBlocking("closeDevice") { _, _ ->
                // TODO fix it
                //frameFlowChannel.close()
                //depthCamera.stop()
                freenect.checkReturn(freenect_stop_depth(dev))
                freenect.checkReturn(freenect_close_device(dev))
            }
        }

    }

}

/**
 * This class provides a low level API for accessing a kinect1 device.
 * All the operations are executed in a single thread responsible for calling
 * freenect API.
 *
 * @param initialLogLevel the log level to use when freenect is initialized.
 */
class Freenect(initialLogLevel: Kinect1.LogLevel) {

    private var currentLogLevel = initialLogLevel

    private val logger = KotlinLogging.logger {}

    private val nativeLogger = KotlinLogging.logger(logger.name + ".native")

    val logAdapters = arrayOf<(message: String) -> Unit>(
        { message -> nativeLogger.error(message) },
        { message -> nativeLogger.warn(message) },
        { message -> nativeLogger.info("NOTICE: $message") },
        { message -> nativeLogger.info(message) },
        { message -> nativeLogger.debug(message) },
        { message -> nativeLogger.debug("SPEW: $message") },
        { message -> nativeLogger.trace(message) }
    )

    private fun setUpLogging() {
        freenect_set_log_callback(
            ctx,
            object : freenect_log_cb() {
                override fun call(dev: freenect_context, level: Int, msg: BytePointer) {
                    logAdapters[level].invoke(msg.string)
                }
            }
        )
        freenect_set_log_level(ctx, currentLogLevel.code)
    }

    var logLevel: Kinect1.LogLevel
        get() = currentLogLevel
        set(value) {
            call("logLevel[$value]") { ctx, _ ->
                freenect_set_log_level(ctx, value.code)
            }
            currentLogLevel = value
        }

    var expectingEvents: Boolean = false

    private val ctx = freenect_context()

    private val usbCtx = freenect_usb_context()

    private var running: Boolean = true

    private val runner = thread(name = "kinect1", start = true, isDaemon = true) {
        logger.info("Starting Kinect1 thread")
        checkReturn(freenect_init(ctx, usbCtx))
        //setUpLogging()
        val num = checkReturn(freenect_num_devices(ctx))
        if (num == 0) {
            logger.warn { "Could not find any Kinect1 devices, calling openDevice() will throw exception" }
        } else {
            val devices = listDevices()
            logger.info { "Kinect1 detected, device count: ${devices.size}" }
            devices.forEachIndexed { index, info ->
                logger.info { "  |-[$index]: serialNumber: ${info.serialNumber}" }
            }
        }

        val timeout = freenect.timeval()
        timeout.tv_sec(1)
        while (running) {
            if (expectingEvents) {
                val ret = freenect_process_events(ctx)
                if (ret != 0) {
                    logger.error { "freenect_process_events returned non-zero value: $ret" }
                }
                val tasks = freenectCallQueue.iterator()
                for (task in tasks) {
                    tasks.remove()
                    task.run()
                }
            } else {
                freenectCallQueue.pollFirst()?.run()
            }
        }

        checkReturn(freenect_shutdown(ctx))
    }

    private val freenectCallQueue = LinkedBlockingDeque<FutureTask<*>>()

    fun call(
        name: String,
        block: (
            ctx: freenect_context,
            usbCtx: freenect_usb_context
        ) -> Unit
    ) {
        logger.debug { "call '$name' requested (non-blocking)" }
        val task = FutureTask {
            logger.trace { "call '$name': started" }
            try {
                block(ctx, usbCtx)
                logger.trace { "call '$name': ended" }
            } catch (e: Exception) {
                logger.error("call '$name': failed", e)
            }
        }
        freenectCallQueue.add(task)
    }

    fun <T> callBlocking(
        name: String,
        block: (
            ctx: freenect_context,
            usbCtx: freenect_usb_context
        ) -> T
    ): T {
        logger.debug { "call '$name' requested (blocking)" }
        val task = FutureTask {
            logger.trace { "call '$name': started" }
            try {
                val result = block(ctx, usbCtx)
                logger.trace { "call '$name': ended" }
                Result.success(result)
            } catch (e: Exception) {
                logger.error("call '$name': failed", e)
                Result.failure(e)
            }
        }
        freenectCallQueue.add(task)
        val result = task.get()
        logger.debug { "call '$name': returned result" }
        return result.getOrThrow()
    }

    fun listDevices() : List<Kinect1.DeviceInfo> {
        val attributes = freenect_device_attributes()
        freenect_list_device_attributes(ctx, attributes)
        try {
            val devices = buildList {
                var item: freenect_device_attributes? =
                    if (attributes.isNull) null
                    else attributes
                while (item != null) {
                    val serialNumber = item.camera_serial().string
                    add(Kinect1.DeviceInfo(serialNumber))
                    item = item.next()
                }
            }
            return devices
        } finally {
            if (!attributes.isNull) {
                freenect_free_device_attributes(attributes)
            }
        }
    }

    fun close() {
        logger.debug("Closing kinect1 runner")
        running = false
        logger.debug("Waiting for runner thread to finish")
        runner.join()
    }

    // TODO how to make it work?
    private fun Set<Kinect1.SubDevice>.toFreenectExpression(): Int {
        var value: Int = 0
        this.forEach {
            value = value or it.code
        }
        return value
    }

    fun checkReturn(ret: Int): Int =
        if (ret >= 0) ret
        else {
            throw Kinect1Exception("Freenect error: ret=$ret")
        }

}
