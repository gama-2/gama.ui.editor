/*******************************************************************************************************
 *
 * DelegateForAllElements.java, in gama.ui.shared.modeling, is part of the source code of the
 * GAMA modeling and simulation platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 * 
 ********************************************************************************************************/
package gama.ui.editor.editor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import gama.ui.shared.utils.WorkbenchHelper;

/**
 * The Class DelegateForAllElements.
 */
public class DelegateForAllElements implements IWorkbenchWindowActionDelegate {

	@Override
	public void run(final IAction action) {
		try {
			WorkbenchHelper.runCommand("org.eclipse.xtext.ui.shared.OpenXtextElementCommand");
		} catch (final ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {}

	@Override
	public void dispose() {}

	@Override
	public void init(final IWorkbenchWindow window) {}

}
