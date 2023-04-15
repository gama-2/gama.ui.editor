/*******************************************************************************************************
 *
 * GamlDocumentationProvider.java, in gama.ui.shared.modeling, is part of the source code of the GAMA modeling and
 * simulation platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.editor.hover;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.documentation.impl.MultiLineCommentDocumentationProvider;

import com.google.inject.Inject;

import gama.annotations.common.interfaces.IGamlDescription;
import gama.core.common.interfaces.IDocManager;
import gama.core.common.util.FileUtils;
import gama.core.runtime.GAMA;
import gama.core.util.file.IGamaFileMetaData;
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
import gaml.compiler.gaml.StringLiteral;
import gaml.compiler.gaml.TypeRef;
import gaml.compiler.gaml.UnitName;
import gaml.compiler.gaml.VarDefinition;
import gaml.compiler.gaml.VariableRef;
import gama.ui.editor.editor.GamlHyperlinkDetector;
import gaml.compiler.EGaml;
import gaml.compiler.resource.GamlResourceServices;
import gaml.core.compilation.GAML;
import gaml.core.compilation.kernel.GamaSkillRegistry;
import gaml.core.descriptions.FacetProto;
import gaml.core.descriptions.SkillDescription;
import gaml.core.descriptions.SymbolProto;
import gaml.core.expressions.units.UnitConstantExpression;
import gaml.core.factories.DescriptionFactory;
import gaml.core.operators.Strings;
import gaml.core.statements.DoStatement;

/**
 * The Class GamlDocumentationProvider.
 */
public class GamlDocumentationProvider extends MultiLineCommentDocumentationProvider {

	/** The detector. */
	@Inject protected GamlHyperlinkDetector detector;

	/**
	 * Gets the only comment.
	 *
	 * @param o
	 *            the o
	 * @return the only comment
	 */
	public String getOnlyComment(final EObject o) {
		return super.getDocumentation(o);
	}

	@Override
	public String getDocumentation(final EObject o) {
		if (o instanceof Import) return "ctrl-click or cmd-click on the path to open this model in a new editor";
		if (o instanceof S_Global) return getDocumentation(o.eContainer().eContainer());
		if (o instanceof StringLiteral) {
			final URI iu = detector.getURI((StringLiteral) o);
			if (iu != null) {
				if (FileUtils.isFileExistingInWorkspace(iu)) {
					final IFile file = FileUtils.getWorkspaceFile(iu);
					final IGamaFileMetaData data = GAMA.getGui().getMetaDataProvider().getMetaData(file, false, true);
					if (data == null) {
						final String ext = file.getFileExtension();
						return "This workspace " + ext + " file has no metadata associated with it";
					}
					String s = data.getDocumentation();
					if (s != null) return s.replace(Strings.LN, "<br/>");
				} else { // absolute file
					final IFile file =
							FileUtils.createLinkToExternalFile(((StringLiteral) o).getOp(), o.eResource().getURI());
					if (file == null) return "This file is outside the workspace and cannot be found.";
					final IGamaFileMetaData data = GAMA.getGui().getMetaDataProvider().getMetaData(file, false, true);
					if (data == null) {
						final String ext = file.getFileExtension();
						return "This external " + ext + " file has no metadata associated with it";
					}
					String s = data.getDocumentation();
					if (s != null) return s.replace(Strings.LN, "<br/>");
				}
			}
		}

		String comment = super.getDocumentation(o);
		if (o instanceof VariableRef) {
			comment = super.getDocumentation(((VariableRef) o).getRef());
		} else if (o instanceof ActionRef) { comment = super.getDocumentation(((ActionRef) o).getRef()); }
		if (comment == null) {
			comment = "";
		} else {
			comment += "<br/>";
		}
		if (o instanceof TypeRef) {
			final Statement s = EGaml.getInstance().getStatement(o);
			if (s instanceof S_Definition && ((S_Definition) s).getTkey() == o) {
				final IDocManager dm = GamlResourceServices.getResourceDocumenter();
				final IGamlDescription gd = dm.getGamlDocumentation(s);
				if (gd != null) return gd.getDocumentation().get();
			}
		} else if (o instanceof Function f) {
			if (f.getLeft() instanceof ActionRef) {
				final ActionRef ref = (ActionRef) f.getLeft();
				final String temp = getDocumentation(ref.getRef());
				if (temp != null && !temp.contains("No documentation")) return temp;
			}
		} else if (o instanceof UnitName) {
			final String name = ((UnitName) o).getRef().getName();
			final UnitConstantExpression exp = GAML.UNITS.get(name);
			if (exp != null) return exp.getDocumentation().get();
		}

		// ============================================================================
		// All the cases corresponding to #3495 -- arguments to actions and primitives
		// CASE do run_thread interval: 2#s;
		if (o instanceof Facet f && f.eContainer() instanceof S_Do sdo && sdo.getExpr() instanceof VariableRef vr) {
			String key = EGaml.getInstance().getKeyOf(f);

			if (!DoStatement.DO_FACETS.contains(key)) {
				IGamlDescription action = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(vr);
				return action == null ? "" : action.getDocumentation().get(key).get();
			}
		}

		// CASE do run_thread with: [interval::2#s];
		if (o instanceof ArgumentPair pair && pair.eContainer() instanceof ExpressionList el
				&& el.eContainer() instanceof Array array && array.eContainer() instanceof Facet facet) {
			if (facet.eContainer() instanceof S_Do sdo && sdo.getExpr() instanceof VariableRef vr) {
				String key = pair.getOp();
				if (!DoStatement.DO_FACETS.contains(key)) {
					IGamlDescription action = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(vr);
					return action == null ? "" : action.getDocumentation().get(key).get();
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
				return species == null ? "" : species.getDocumentation().get(key).get();

			}
		}

		// CASE do run_thread with: (interval::2#s);
		if (o instanceof VariableRef vr && vr.eContainer() instanceof Parameter pair
				&& pair.eContainer() instanceof ExpressionList el && el.eContainer() instanceof Facet facet
				&& facet.eContainer() instanceof S_Do sdo && sdo.getExpr() instanceof VariableRef v) {
			String key = EGaml.getInstance().getKeyOf(pair);
			if (!DoStatement.DO_FACETS.contains(key)) {
				IGamlDescription action = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(v);
				return action == null ? "" : action.getDocumentation().get(key).get();
			}

		}

		// CASE do run_thread (interval: 2#s); unknown aa <- self.run_thread (interval: 2#s); aa <- run_thread
		// (interval: 2#s);
		if (o instanceof VariableRef && o.eContainer() instanceof Parameter param
				&& param.eContainer() instanceof ExpressionList el && el.eContainer() instanceof Function function
				&& function.getLeft() instanceof ActionRef ar) {
			final IGamlDescription action = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(function);
			String key = ((VariableRef) o).getRef().getName();
			return action == null ? "" : action.getDocumentation().get(key).get();
		}

		// Case of species xxx skills: [skill]
		if (o instanceof VariableRef && o.eContainer() instanceof ExpressionList el
				&& el.eContainer() instanceof Array array && array.eContainer() instanceof Facet facet
				&& facet.getKey().startsWith("skills")) {
			VarDefinition vd = ((VariableRef) o).getRef();
			String name = vd.getName();
			SkillDescription skill = GamaSkillRegistry.INSTANCE.get(name);
			if (skill != null) return skill.getDocumentation().get();

		}

		// ============================================================================

		IGamlDescription description = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(o);

		if (description == null) {
			// In case we have a reference to a variable which is not documented itself (like in create xxx with: [var:
			// yyy])
			if (o instanceof VariableRef) {
				VarDefinition vd = ((VariableRef) o).getRef();
				description = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(vd);
				if (description != null) {
					String result = description.getDocumentation().get();
					if (result == null) return "";
					return result;
				}
			}
			if (o instanceof Facet) {

				String facetName = ((Facet) o).getKey();
				if (facetName.endsWith(":")) { facetName = facetName.substring(0, facetName.length() - 1); }
				final EObject cont = o.eContainer();
				final String key = EGaml.getInstance().getKeyOf(cont);
				final SymbolProto p = DescriptionFactory.getProto(key, null);
				if (p != null) {
					final FacetProto f = p.getPossibleFacets().get(facetName);
					if (f != null) return comment + Strings.LN + f.getDocumentation();
				}
				return comment;
			}
			if (comment.isEmpty()) return null;
			return comment + Strings.LN + "No documentation.";
		}

		return comment + description.getDocumentation();
	}

}