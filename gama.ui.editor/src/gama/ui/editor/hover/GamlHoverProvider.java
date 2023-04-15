/*******************************************************************************************************
 *
 * GamlHoverProvider.java, in gama.ui.shared.modeling, is part of the source code of the GAMA modeling and simulation
 * platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.editor.hover;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.hover.DispatchingEObjectTextHover;
import org.eclipse.xtext.ui.editor.hover.html.DefaultEObjectHoverProvider;
import org.eclipse.xtext.ui.editor.hover.html.IXtextBrowserInformationControl;
import org.eclipse.xtext.ui.editor.hover.html.XtextBrowserInformationControl;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;

import com.google.inject.Inject;

import gama.annotations.common.interfaces.IGamlDescription;
import gaml.compiler.gaml.ActionDefinition;
import gaml.compiler.gaml.ActionRef;
import gaml.compiler.gaml.ArgumentPair;
import gaml.compiler.gaml.Array;
import gaml.compiler.gaml.ExpressionList;
import gaml.compiler.gaml.Facet;
import gaml.compiler.gaml.Function;
import gaml.compiler.gaml.Import;
import gaml.compiler.gaml.Parameter;
import gaml.compiler.gaml.S_Definition;
import gaml.compiler.gaml.S_Do;
import gaml.compiler.gaml.S_Global;
import gaml.compiler.gaml.Statement;
import gaml.compiler.gaml.TypeRef;
import gaml.compiler.gaml.UnitFakeDefinition;
import gaml.compiler.gaml.UnitName;
import gaml.compiler.gaml.VarDefinition;
import gaml.compiler.gaml.VariableRef;
import gama.ui.shared.utils.WorkbenchHelper;
import gaml.compiler.EGaml;
import gaml.compiler.resource.GamlResourceServices;
import gaml.core.compilation.GAML;
import gaml.core.compilation.kernel.GamaSkillRegistry;
import gaml.core.descriptions.FacetProto;
import gaml.core.descriptions.SkillDescription;
import gaml.core.descriptions.SymbolProto;
import gaml.core.expressions.units.UnitConstantExpression;
import gaml.core.factories.DescriptionFactory;
import gaml.core.statements.DoStatement;

/**
 * The Class GamlHoverProvider.
 */
public class GamlHoverProvider extends DefaultEObjectHoverProvider {

	/**
	 * The Class GamlDispatchingEObjectTextHover.
	 */
	public static class GamlDispatchingEObjectTextHover extends DispatchingEObjectTextHover {

		/** The e object at offset helper. */
		@Inject private EObjectAtOffsetHelper eObjectAtOffsetHelper;

		/** The location in file provider. */
		@Inject private ILocationInFileProvider locationInFileProvider;

		/** The correct. */
		EObject correct = null;

		@Override
		protected Pair<EObject, IRegion> getXtextElementAt(final XtextResource resource, final int offset) {
			// BUGFIX AD 2/4/13 : getXtextElementAt() is called twice, one to
			// compute the region
			// from the UI thread, one to compute the objects from the hover
			// thread. The offset in
			// the second call is always false (maybe we should file a bug in
			// XText). The following
			// code is a workaround.
			ITextRegion region = null;
			EObject o;
			if (correct == null) {
				correct = eObjectAtOffsetHelper.resolveContainedElementAt(resource, offset);
				o = correct;
			} else {
				o = correct;
				correct = null;
			}
			// /BUGFIX
			if (o != null) {
				if (o instanceof ActionRef) {
					final EObject container = o.eContainer();
					if (container instanceof Function) {
						o = container;
						region = locationInFileProvider.getFullTextRegion(o);
					}
				}
				if (region == null) { region = locationInFileProvider.getSignificantTextRegion(o); }
				final IRegion region2 = new Region(region.getOffset(), region.getLength());
				return Tuples.create(o, region2);
			}
			final ILeafNode node = NodeModelUtils.findLeafNodeAtOffset(resource.getParseResult().getRootNode(), offset);
			if (node != null && node.getGrammarElement() instanceof Keyword) {
				final IRegion region2 = new Region(node.getOffset(), node.getLength());
				return Tuples.create(node.getGrammarElement(), region2);
			}
			return null;
		}

		@Override
		public Object getHoverInfo(final EObject first, final ITextViewer textViewer, final IRegion hoverRegion) {
			return super.getHoverInfo(first, textViewer, hoverRegion);
		}

	}

	/**
	 * The Class GamlHoverControlCreator.
	 */
	public class GamlHoverControlCreator extends HoverControlCreator {

		/**
		 * @param informationPresenterControlCreator
		 */
		public GamlHoverControlCreator(final IInformationControlCreator informationPresenterControlCreator) {
			super(informationPresenterControlCreator);
		}

		/**
		 * The Class GamlInformationControl.
		 */
		public class GamlInformationControl extends XtextBrowserInformationControl {

			@Override
			public void setSize(final int width, final int height) {
				super.setSize(width, height);
				final org.eclipse.swt.graphics.Point p = WorkbenchHelper.getDisplay().getCursorLocation();
				p.x -= 5;
				p.y += 15;
				setLocation(p);
			}

			/**
			 * @param parent
			 * @param symbolicFontName
			 * @param statusFieldText
			 */
			public GamlInformationControl(final Shell parent, final String symbolicFontName,
					final String statusFieldText) {
				super(parent, symbolicFontName, statusFieldText);
			}

			/*
			 * @see org.eclipse.jface.text.IInformationControlExtension5# getInformationPresenterControlCreator()
			 */
			@Override
			public IInformationControlCreator getInformationPresenterControlCreator() {
				return GamlHoverProvider.this.getInformationPresenterControlCreator();
			}
		}

		@Override
		public IInformationControl doCreateInformationControl(final Shell parent) {

			final String tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
			if (BrowserInformationControl.isAvailable(parent)) {
				final String font = "org.eclipse.jdt.ui.javadocfont";
				final IXtextBrowserInformationControl iControl =
						new GamlInformationControl(parent, font, tooltipAffordanceString);
				addLinkListener(iControl);
				return iControl;
			}
			return new DefaultInformationControl(parent, tooltipAffordanceString);
		}
	}

	/** The creator. */
	private IInformationControlCreator creator;

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (creator == null) { creator = new GamlHoverControlCreator(getInformationPresenterControlCreator()); }
		return creator;
	}

	@Override
	protected boolean hasHover(final EObject o) {
		return true;
	}

	@Override
	protected String getFirstLine(final EObject o) {
		if (o instanceof Import) {
			String uri = ((Import) o).getImportURI();
			uri = uri.substring(uri.lastIndexOf('/') + 1);
			final String model = ((Import) o).getName() != null ? "micro-model" : "model";
			return "<b>Import of the " + model + " defined in <i>" + uri + "</i></b>";
		}
		if (o instanceof S_Global) return "<b>Global definitions of </b>" + getFirstLine(o.eContainer().eContainer());
		final Statement s = EGaml.getInstance().getStatement(o);
		if (o instanceof TypeRef && s instanceof S_Definition && ((S_Definition) s).getTkey() == o)
			return getFirstLine(s);

		// All the cases corresponding to #3495 -- arguments to actions and primitives
		// CASE do run_thread interval: 2#s;
		if (o instanceof Facet f && f.eContainer() instanceof S_Do sdo) {
			String key = EGaml.getInstance().getKeyOf(f);
			if (!DoStatement.DO_FACETS.contains(key)) {
				String result = "Argument " + key + " of action " + EGaml.getInstance().getNameOfRef(sdo.getExpr());
				return "<b>" + result + "</b>";
			}
		}

		// CASE do run_thread with: [interval::2#s];
		if (o instanceof ArgumentPair pair && pair.eContainer() instanceof ExpressionList el
				&& el.eContainer() instanceof Array array && array.eContainer() instanceof Facet facet) {
			if (facet.eContainer() instanceof S_Do sdo) {
				String key = pair.getOp();
				if (!DoStatement.DO_FACETS.contains(key)) {
					String result = "Argument " + key + " of action " + EGaml.getInstance().getNameOfRef(sdo.getExpr());
					return "<b>" + result + "</b>";
				}

			}
		}

		// CASE create xxx with: [var::yyy]
		if (o instanceof ArgumentPair pair && pair.eContainer() instanceof ExpressionList el
				&& el.eContainer() instanceof Array array && array.eContainer() instanceof Facet facet) {
			if (facet.eContainer() instanceof Statement sdo && "create".equals(sdo.getKey())) {
				String key = pair.getOp();
				IGamlDescription species =
						GamlResourceServices.getResourceDocumenter().getGamlDocumentation(sdo.getExpr());
				if (species != null) return "<b>" + "Attribute " + key + " defined in " + species.getTitle() + "</b>";

			}
		}

		// CASE do run_thread with: (interval::2#s);
		if (o instanceof VariableRef vr && vr.eContainer() instanceof Parameter pair
				&& pair.eContainer() instanceof ExpressionList el && el.eContainer() instanceof Facet facet
				&& facet.eContainer() instanceof S_Do sdo) {
			String key = EGaml.getInstance().getKeyOf(pair);
			if (!DoStatement.DO_FACETS.contains(key)) {
				String result = "Argument " + key + " of action " + EGaml.getInstance().getNameOfRef(sdo.getExpr());
				return "<b>" + result + "</b>";
			}

		}

		// CASE do run_thread (interval: 2#s); unknown aa <- self.run_thread (interval: 2#s); aa <- run_thread
		// (interval: 2#s);
		if (o instanceof VariableRef) {

			if (o.eContainer() instanceof Parameter param && param.eContainer() instanceof ExpressionList el
					&& el.eContainer() instanceof Function function) {
				final IGamlDescription description =
						GamlResourceServices.getResourceDocumenter().getGamlDocumentation(function);
				if (description != null) {
					VarDefinition vd = ((VariableRef) o).getRef();
					String result = "Argument " + vd.getName() + " of action "
							+ EGaml.getInstance().getNameOfRef(function.getLeft());
					return "<b>" + result + "</b>";
				}
			}

			// Case of species xxx skills: [skill]
			if (o.eContainer() instanceof ExpressionList el && el.eContainer() instanceof Array array
					&& array.eContainer() instanceof Facet facet && facet.getKey().startsWith("skills")) {
				VarDefinition vd = ((VariableRef) o).getRef();
				String name = vd.getName();
				SkillDescription skill = GamaSkillRegistry.INSTANCE.get(name);
				if (skill != null) return "<b>" + skill.getTitle() + "</b>";

			}

			VarDefinition vd = ((VariableRef) o).getRef();
			IGamlDescription description = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(vd);
			if (description != null) {
				String result = description.getTitle();
				if (result == null) return "";
				return "<b>" + result + "</b>";
			}
		}

		if (o instanceof Function) {
			final ActionRef ref = getActionFrom((Function) o);
			if (ref != null) {
				final ActionDefinition def = ref.getRef();
				if (def != null) {
					final String temp = getFirstLine(def);
					if (!temp.isEmpty()) return temp;
				}
			}
		} else if (o instanceof UnitName) {
			final UnitFakeDefinition fake = ((UnitName) o).getRef();
			if (fake == null) return "<b> Unknown unit or constant </b>";
			final UnitConstantExpression unit = GAML.UNITS.get(fake.getName());
			if (unit == null) return "<b> Unknown unit or constant </b>";
			return "<b>" + unit.getTitle() + "</b>";
		}

		final IGamlDescription description = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(o);
		if (description != null) {
			String result = description.getTitle();
			if (result == null || result.isEmpty()) return "";
			return "<b>" + result + "</b>";
		}
		if (o instanceof Facet) return "<b>" + getFirstLineOf((Facet) o) + "</b>";

		if (s != null && DescriptionFactory.isStatementProto(EGaml.getInstance().getKeyOf(o))) {
			if (s == o) return "";
			return getFirstLine(s);
		}
		if (o instanceof TypeRef) return "<b>Type " + EGaml.getInstance().getKeyOf(o) + "</b>";
		return "";
	}

	/**
	 * Gets the action from.
	 *
	 * @param f
	 *            the f
	 * @return the action from
	 */
	private ActionRef getActionFrom(final Function f) {
		if (f.getLeft() instanceof ActionRef) return (ActionRef) f.getLeft();
		return null;
	}

	/**
	 * @param o
	 * @return
	 */
	private String getFirstLineOf(final Facet o) {
		String facetName = o.getKey();
		if (facetName.endsWith(":")) { facetName = facetName.substring(0, facetName.length() - 1); }
		final EObject cont = o.eContainer();
		final String key = EGaml.getInstance().getKeyOf(cont);
		final SymbolProto p = DescriptionFactory.getProto(key, null);
		if (p != null) {
			final FacetProto f = p.getPossibleFacets().get(facetName);
			if (f != null) return f.getTitle();
		}
		return "Facet " + o.getKey();

	}
}