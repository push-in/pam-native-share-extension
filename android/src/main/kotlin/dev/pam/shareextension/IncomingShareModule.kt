package dev.pam.shareextension

import android.content.Context
import dev.pam.nativeapp.modules.*
import dev.pam.nativeapp.protocol.WireMap

class IncomingShareModule() : NativeModule, SurfaceRetirableModule {
    /** The generated plugin registry builds every module and view factory with the app context. */
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    override fun invoke(method: String, payload: ByteArray, completion: ModuleCompletion) {
        runCatching {
            when (method) {
                "initial" -> {
                    PamIncomingShares.configure(WireMap.decode(payload))
                    PamIncomingShares.initial(completion)
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
