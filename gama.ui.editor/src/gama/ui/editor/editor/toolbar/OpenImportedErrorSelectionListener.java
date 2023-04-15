/*******************************************************************************************************
 *
 * OpenImportedErrorSelectionListener.java, in gama.ui.shared.modeling, is part of the source code of the
 * GAMA modeling and simulation platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 * 
 ********************************************************************************************************/
package gama.ui.editor.editor.toolbar;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;

import gama.core.runtime.GAMA;
import gama.ui.editor.editor.GamlEditor;
import gama.ui.editor.editor.GamlEditorState;
import gama.ui.shared.menus.GamaMenu;
import gama.ui.shared.views.toolbar.Selector;

/**
 * The class CreateExperimentSelectionListener.
 *
 * @author drogoul
 * @since 27 août 2016
 *
 */
public class OpenImportedErrorSelectionListener implements Selector {

	/** The editor. */
	GamlEditor editor;
	
	/** The state. */
	GamlEditorState state;

	/**
	 * 
	 */
	public OpenImportedErrorSelectionListener(final GamlEditor editor, final GamlEditorState state,
			final Control toolbar) {
		this.editor = editor;
		this.state = state;
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(final SelectionEvent e) {
		final Map<String, URI> msgs = state.getImportedErrors();
		if (!msgs.isEmpty()) {
			final GamaMenu menu = new GamaMenu() {

				@Override
				protected void fillMenu() {

					for (final String s : msgs.keySet()) {
						action(s, new SelectionAdapter() {

							@Override
							public void widgetSelected(final SelectionEvent e1) {
								GAMA.getGui().editModel(null, msgs.get(s));
							}

						}, null);
					}

				}
			};
			menu.open((Control) e.widget, e, 32);
		}
	}

}
