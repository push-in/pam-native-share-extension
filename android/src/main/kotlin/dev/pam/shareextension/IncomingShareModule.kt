package dev.pam.shareextension

import android.content.Context
import dev.pam.nativeapp.modules.*
import dev.pam.nativeapp.protocol.WireMap
import dev.pam.nativeapp.protocol.WireValue

class IncomingShareModule() : NativeModule, SurfaceRetirableModule {
    private var context: Context? = null

    /** The generated plugin registry builds every module and view factory with the app context. */
    constructor(context: Context) : this() {
        this.context = context.applicationContext ?: context
    }

    override fun invoke(method: String, payload: ByteArray, completion: ModuleCompletion) {
        runCatching {
            when (method) {
                "initial" -> {
                    PamIncomingShares.configure(WireMap.decode(payload))
                    context?.let(PamIncomingShares::sweepExpired)
                    PamIncomingShares.initial(completion)
                }
                "release" -> {
                    val eventId = (WireMap.decode(payload)["eventId"] as? WireValue.Text)?.value.orEmpty()
                    val released = context?.let { PamIncomingShares.release(it, eventId) } ?: false
                    completion.complete(
                        ModuleResultStatus.SUCCESS,
                        WireMap.encode(mapOf("released" to WireValue.Flag(released))),
                    )
                }
                "next" -> PamIncomingShares.next(completion)
                else -> error("Unknown incoming-share method $method")
            }
        }.onFailure { error ->
            completion.complete(
                ModuleResultStatus.FAILURE,
                (error.message ?: "Incoming share failed").toByteArray(),
            )
        }
    }

    // The queue and waiter belong to the retained PHP runtime, not this Activity.
    override fun retireSurface() = Unit

    override fun close() {
        PamIncomingShares.close("Incoming-share module closed")
    }
}
