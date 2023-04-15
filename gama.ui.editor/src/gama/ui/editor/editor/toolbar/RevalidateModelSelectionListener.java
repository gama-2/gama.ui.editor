/*******************************************************************************************************
 *
 * RevalidateModelSelectionListener.java, in gama.ui.shared.modeling, is part of the source code of the
 * GAMA modeling and simulation platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 * 
 ********************************************************************************************************/
package gama.ui.editor.editor.toolbar;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.CancelableUnitOfWork;

import gama.ui.editor.editor.GamlEditor;
import gama.ui.shared.views.toolbar.Selector;
import gaml.compiler.validation.GamlModelBuilder;

/**
 * The class CreateExperimentSelectionListener.
 *
 * @author drogoul
 * @since 27 août 2016
 *
 */
public class RevalidateModelSelectionListener implements Selector {

	/** The editor. */
	GamlEditor editor;

	/**
	 *
	 */
	public RevalidateModelSelectionListener(final GamlEditor editor) {
		this.editor = editor;
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(final SelectionEvent e) {

		editor.getDocument().readOnly(new CancelableUnitOfWork<Object, XtextResource>() {

			@Override
			public Object exec(final XtextResource state, final CancelIndicator c) throws Exception {
				return GamlModelBuilder.getDefaultInstance().compile(state.getURI(), null);
			}
		});

	}

}
