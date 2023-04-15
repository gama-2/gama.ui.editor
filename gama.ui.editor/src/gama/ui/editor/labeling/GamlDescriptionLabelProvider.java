/*******************************************************************************************************
 *
 * GamlDescriptionLabelProvider.java, in gama.ui.shared.modeling, is part of the source code of the
 * GAMA modeling and simulation platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 * 
 ********************************************************************************************************/
package gama.ui.editor.labeling;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.ui.label.DefaultDescriptionLabelProvider;

import com.google.inject.Inject;

/**
 * Provides labels for a IEObjectDescriptions and IResourceDescriptions.
 * 
 * see http://www.eclipse.org/Xtext/documentation/latest/xtext.html#labelProvider
 */
public class GamlDescriptionLabelProvider extends DefaultDescriptionLabelProvider {

	/** The delegate. */
	@Inject ILabelProvider delegate;

	@Override
	public Object text(final IEObjectDescription element) {
		final EObject o = element.getEObjectOrProxy();
		String name;
		if (o.eIsProxy())
			name = element.getQualifiedName().toString();
		else
			name = delegate.getText(o);
		return name + " [" + URI.decode(element.getEObjectURI().lastSegment()) + "]";
	}

	@Override
	public Image image(final IEObjectDescription element) {
		final Image i = delegate.getImage(element.getEObjectOrProxy());
		return i;
	}

	/*
	 * //Labels and icons can be computed like this:
	 * 
	 * String text(IEObjectDescription ele) { return "my "+ele.getName(); }
	 * 
	 * String image(IEObjectDescription ele) { return ele.getEClass().getName() + ".gif"; }
	 */

}
