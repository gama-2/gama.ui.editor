/*******************************************************************************************************
 *
 * OpenExperimentSelectionListener.java, in gama.ui.shared.modeling, is part of the source code of the GAMA modeling
 * and simulation platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.editor.editor.toolbar;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.events.SelectionEvent;

import gama.core.common.preferences.GamaPreferences;
import gama.core.runtime.GAMA;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.ui.editor.editor.GamlEditor;
import gama.ui.editor.editor.GamlEditorState;
import gama.ui.shared.interfaces.IModelRunner;
import gama.ui.shared.utils.WorkbenchHelper;
import gama.ui.shared.views.toolbar.Selector;

/**
 * The class CreateExperimentSelectionListener.
 *
 * @author drogoul
 * @since 27 août 2016
 *
 */
public class OpenExperimentSelectionListener implements Selector {

	/** The editor. */
	GamlEditor editor;

	/** The state. */
	GamlEditorState state;

	/** The runner. */
	final IModelRunner runner;

	/**
	 *
	 */
	public OpenExperimentSelectionListener(final GamlEditor editor, final GamlEditorState state,
			final IModelRunner runner) {
		this.editor = editor;
		this.state = state;
		this.runner = runner;
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(final SelectionEvent e) {

		// final IGui gui = GAMA.getRegularGui();
		// We refuse to run if there is no XtextGui available.
		editor.doSave(null);
		if (GamaPreferences.Modeling.EDITOR_SAVE.getValue()) {
			WorkbenchHelper.getPage().saveAllEditors(GamaPreferences.Modeling.EDITOR_SAVE_ASK.getValue());
		}
		String name = (String) e.widget.getData("exp");
		final int i = state.abbreviations.indexOf(name);
		if (i == -1) return;
		name = state.experiments.get(i);
		runner.runModel(editor.getDocument(), name);

	}

	/**
	 * Goto editor.
	 *
	 * @param exception
	 *            the exception
	 */
	void gotoEditor(final GamaRuntimeException exception) {
		final EObject o = exception.getEditorContext();
		if (o != null) { WorkbenchHelper.asyncRun(() -> GAMA.getGui().editModel(null, o)); }

	}

}
