package dev.pam.shareextension
import dev.pam.nativeapp.modules.*
import org.junit.Test
import org.junit.Assert.*
class ReloadListenersTest {
    @Test
    fun incomingShareWaiterCanBeRearmedAfterReload() {
        PamIncomingShares.next(ModuleCompletion { _, _ -> })

        PamIncomingShares.prepareReload()
        PamIncomingShares.next(ModuleCompletion { _, _ -> })

        PamIncomingShares.prepareReload()
    }
}
