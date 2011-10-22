package org.wdssii.gui.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.gui.views.WorldWindView;

/** Called by name from WdssiiDynamic */
public class Snapshot3DWorldCommand extends WdssiiCommand {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog(Snapshot3DWorldCommand.class);

    @Override
    public boolean execute() {

        if (myWdssiiView != null) {
            if (myWdssiiView instanceof WorldWindView) {
                WorldWindView e = (WorldWindView) (myWdssiiView);
                e.takeDialogSnapshot();
            }
        }
        return false;
    }
}