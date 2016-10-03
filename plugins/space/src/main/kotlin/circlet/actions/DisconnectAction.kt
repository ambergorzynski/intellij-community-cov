package circlet.actions

import circlet.components.*
import circlet.utils.*
import com.intellij.openapi.actionSystem.*

class DisconnectAction : AnAction() {

    override fun update(e: AnActionEvent?) {
        e ?: return
        val project = e.project
        project ?: return

        val connectionState = project.component<IdePluginClient>().connectionState
        // can disconnect in any connected state...
        e.presentation.isEnabled = connectionState != null
    }

    override fun actionPerformed(e: AnActionEvent?) {
        e ?: return
        val project = e.project
        project ?: return

        project.component<IdePluginClient>().disconnect()
    }
}
