/*******************************************************************************************************
 *
 * GamlUiModule.java, in gama.ui.shared.modeling, is part of the source code of the GAMA modeling and simulation
 * platform (v.1.9.0).
 *
 * (c) 2007-2022 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.editor;

import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.xtext.builder.builderState.IMarkerUpdater;
import org.eclipse.xtext.builder.resourceloader.IResourceLoader;
import org.eclipse.xtext.builder.resourceloader.ResourceLoaderProviders;
import org.eclipse.xtext.documentation.IEObjectDocumentationProvider;
import org.eclipse.xtext.ide.LexerIdeBindings;
import org.eclipse.xtext.ide.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.eclipse.xtext.parser.IEncodingProvider;
import org.eclipse.xtext.parser.antlr.ISyntaxErrorMessageProvider;
import org.eclipse.xtext.resource.clustering.DynamicResourceClusteringPolicy;
import org.eclipse.xtext.resource.clustering.IResourceClusteringPolicy;
import org.eclipse.xtext.resource.containers.IAllContainersState;
import org.eclipse.xtext.service.DispatchingProvider;
import org.eclipse.xtext.service.SingletonBinding;
import org.eclipse.xtext.ui.IImageHelper;
import org.eclipse.xtext.ui.IImageHelper.IImageDescriptorHelper;
import org.eclipse.xtext.ui.editor.IXtextEditorCallback;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;
import org.eclipse.xtext.ui.editor.XtextSourceViewerConfiguration;
import org.eclipse.xtext.ui.editor.actions.IActionContributor;
import org.eclipse.xtext.ui.editor.autoedit.AbstractEditStrategyProvider;
import org.eclipse.xtext.ui.editor.contentassist.ITemplateProposalProvider;
import org.eclipse.xtext.ui.editor.contentassist.XtextContentAssistProcessor;
import org.eclipse.xtext.ui.editor.contentassist.antlr.IContentAssistParser;
import org.eclipse.xtext.ui.editor.contentassist.antlr.internal.Lexer;
import org.eclipse.xtext.ui.editor.folding.IFoldingRegionProvider;
import org.eclipse.xtext.ui.editor.hover.IEObjectHoverProvider;
import org.eclipse.xtext.ui.editor.model.IResourceForEditorInputFactory;
import org.eclipse.xtext.ui.editor.model.ResourceForIEditorInputFactory;
import org.eclipse.xtext.ui.editor.outline.actions.IOutlineContribution;
import org.eclipse.xtext.ui.editor.preferences.IPreferenceStoreInitializer;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ITextAttributeProvider;
import org.eclipse.xtext.ui.resource.IResourceSetProvider;
import org.eclipse.xtext.ui.resource.SimpleResourceSetProvider;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.name.Names;

import gama.core.common.interfaces.IGamlLabelProvider;
import gama.dev.DEBUG;
import gama.ui.editor.contentassist.GamlTemplateProposalProvider;
import gama.ui.editor.decorators.GamlImageHelper;
import gama.ui.editor.decorators.GamlMarkerUpdater;
import gama.ui.editor.editor.GamaAutoEditStrategyProvider;
import gama.ui.editor.editor.GamaSourceViewerFactory;
import gama.ui.editor.editor.GamlEditor;
import gama.ui.editor.editor.GamlEditorTickUpdater;
import gama.ui.editor.editor.GamlHyperlinkDetector;
import gama.ui.editor.editor.GamlMarkOccurrenceActionContributor;
import gama.ui.editor.editor.GamlEditor.GamaSourceViewerConfiguration;
import gama.ui.editor.editor.folding.GamaFoldingActionContributor;
import gama.ui.editor.editor.folding.GamaFoldingRegionProvider;
import gama.ui.editor.highlight.GamlHighlightingConfiguration;
import gama.ui.editor.highlight.GamlReconciler;
import gama.ui.editor.highlight.GamlSemanticHighlightingCalculator;
import gama.ui.editor.highlight.GamlTextAttributeProvider;
import gama.ui.editor.hover.GamlDocumentationProvider;
import gama.ui.editor.hover.GamlHoverProvider;
import gama.ui.editor.hover.GamlHoverProvider.GamlDispatchingEObjectTextHover;
import gama.ui.editor.labeling.GamlLabelProvider;
import gama.ui.editor.outline.GamlLinkWithEditorOutlineContribution;
import gama.ui.editor.outline.GamlOutlinePage;
import gama.ui.editor.outline.GamlSortOutlineContribution;
import gama.ui.editor.templates.GamlTemplateStore;
import gama.ui.editor.utils.ModelRunner;
import gama.ui.shared.interfaces.IModelRunner;
import gaml.compiler.parsing.GamlSyntaxErrorMessageProvider;
import gaml.compiler.resource.GamlEncodingProvider;
import gaml.compiler.ide.contentassist.antlr.GamlParser;

/**
 * Use this class to register components to be used within the IDE.
 */
public class GamlUiModule extends gaml.compiler.ui.AbstractGamlUiModule {

	static {
		DEBUG.OFF();
	}

	/**
	 * Instantiates a new gaml ui module.
	 *
	 * @param plugin
	 *            the plugin
	 */
	public GamlUiModule(final AbstractUIPlugin plugin) {
		super(plugin);
	}

	@SuppressWarnings ("unchecked")
	@Override
	public void configure(final Binder binder) {
		DEBUG.OUT("Initialization of GAML XText UI module begins");
		super.configure(binder);
		binder.bind(String.class).annotatedWith(
				com.google.inject.name.Names.named(XtextContentAssistProcessor.COMPLETION_AUTO_ACTIVATION_CHARS))
				.toInstance(".");
		binder.bind(IContentAssistParser.class).to((Class<? extends IContentAssistParser>) GamlParser.class);
		binder.bind(Lexer.class).annotatedWith(Names.named(LexerIdeBindings.CONTENT_ASSIST))
				.to(InternalGamlLexer.class);
		binder.bind(IResourceLoader.class).toProvider(ResourceLoaderProviders.getParallelLoader());
		binder.bind(IResourceClusteringPolicy.class).to(DynamicResourceClusteringPolicy.class);
		binder.bind(IModelRunner.class).to(ModelRunner.class);
		// binder.bind(XtextDocumentProvider.class).to(XtextDocumentProvider.class);
		binder.bind(IMarkerUpdater.class).to(GamlMarkerUpdater.class);
		binder.bind(IGamlLabelProvider.class).to(GamlLabelProvider.class);
		// binder.bind(IHighlightingConfiguration.class).to(GamlHighlightingConfiguration.class).asEagerSingleton();
		DEBUG.OUT("Initialization of GAML XText UI module finished");
	}

	@Override
	public void configureUiEncodingProvider(final Binder binder) {
		binder.bind(IEncodingProvider.class).annotatedWith(DispatchingProvider.Ui.class).to(GamlEncodingProvider.class);
	}

	/**
	 * Bind parser based content assist context factory$ stateful factory.
	 *
	 * @return the class<? extends org.eclipse.xtext.ui.editor.contentassist.antlr. parser based content assist context
	 *         factory. stateful factory>
	 */
	public Class<? extends org.eclipse.xtext.ui.editor.contentassist.antlr.ParserBasedContentAssistContextFactory.StatefulFactory>
			bindParserBasedContentAssistContextFactory$StatefulFactory() {
		return gama.ui.editor.contentassist.ContentAssistContextFactory.class;
	}

	/**
	 * Bind source viewer factory.
	 *
	 * @return the class<? extends xtext source viewer. factory>
	 */
	public Class<? extends XtextSourceViewer.Factory> bindSourceViewerFactory() {
		return GamaSourceViewerFactory.class;
	}

	@Override
	@SingletonBinding (
			eager = true)
	public Class<? extends org.eclipse.jface.viewers.ILabelProvider> bindILabelProvider() {
		return gama.ui.editor.labeling.GamlLabelProvider.class;
	}

	@Override
	public Class<? extends ITemplateProposalProvider> bindITemplateProposalProvider() {
		return GamlTemplateProposalProvider.class;
	}

	/**
	 * Bind folding region provider.
	 *
	 * @return the class<? extends I folding region provider>
	 */
	public Class<? extends IFoldingRegionProvider> bindFoldingRegionProvider() {
		return GamaFoldingRegionProvider.class;
	}

	@Override
	public Class<? extends org.eclipse.jface.text.ITextHover> bindITextHover() {
		return GamlDispatchingEObjectTextHover.class;
	}

	// For performance issues on opening files : see
	// http://alexruiz.developerblogs.com/?p=2359
	@Override
	public Class<? extends IResourceSetProvider> bindIResourceSetProvider() {
		return SimpleResourceSetProvider.class;
	}

	@Override
	public void configureXtextEditorErrorTickUpdater(final com.google.inject.Binder binder) {
		binder.bind(IXtextEditorCallback.class).annotatedWith(Names.named("IXtextEditorCallBack")).to( //$NON-NLS-1$
				GamlEditorTickUpdater.class);
	}

	/**
	 * @author Pierrick
	 * @return GAMLSemanticHighlightingCalculator
	 */
	public Class<? extends ISemanticHighlightingCalculator> bindSemanticHighlightingCalculator() {
		return GamlSemanticHighlightingCalculator.class;
	}

	/**
	 * Bind I highlighting configuration.
	 *
	 * @return the class<? extends I highlighting configuration>
	 */
	@SingletonBinding (
			eager = false)
	public Class<? extends IHighlightingConfiguration> bindIHighlightingConfiguration() {
		return GamlHighlightingConfiguration.class;
	}

	/**
	 * Bind I text attribute provider.
	 *
	 * @return the class<? extends I text attribute provider>
	 */
	@SingletonBinding ()
	public Class<? extends ITextAttributeProvider> bindITextAttributeProvider() {
		return GamlTextAttributeProvider.class;
	}

	@Override
	public Class<? extends org.eclipse.xtext.ui.editor.IXtextEditorCallback> bindIXtextEditorCallback() {
		// TODO Verify this as it is only needed, normally, for languages that
		// do not use the builder infrastructure
		// (see http://www.eclipse.org/forums/index.php/mv/msg/167666/532239/)
		// not correct for 2.7: return GamlEditorCallback.class;
		return IXtextEditorCallback.NullImpl.class;
	}

	/**
	 * Bind I syntax error message provider.
	 *
	 * @return the class<? extends I syntax error message provider>
	 */
	public Class<? extends ISyntaxErrorMessageProvider> bindISyntaxErrorMessageProvider() {
		return GamlSyntaxErrorMessageProvider.class;
	}

	/**
	 * Bind IE object hover provider.
	 *
	 * @return the class<? extends IE object hover provider>
	 */
	public Class<? extends IEObjectHoverProvider> bindIEObjectHoverProvider() {
		return GamlHoverProvider.class;
	}

	/**
	 * Bind IE object documentation providerr.
	 *
	 * @return the class<? extends IE object documentation provider>
	 */
	public Class<? extends IEObjectDocumentationProvider> bindIEObjectDocumentationProviderr() {
		return GamlDocumentationProvider.class;
	}

	@Override
	public Provider<IAllContainersState> provideIAllContainersState() {
		return org.eclipse.xtext.ui.shared.Access.getWorkspaceProjectsState();
	}

	/**
	 * Bind xtext editor.
	 *
	 * @return the class<? extends xtext editor>
	 */
	public Class<? extends XtextEditor> bindXtextEditor() {
		return GamlEditor.class;
	}

	/**
	 * Bind xtext source viewer configuration.
	 *
	 * @return the class<? extends xtext source viewer configuration>
	 */
	public Class<? extends XtextSourceViewerConfiguration> bindXtextSourceViewerConfiguration() {
		return GamaSourceViewerConfiguration.class;
	}

	@Override
	public Class<? extends IHyperlinkDetector> bindIHyperlinkDetector() {
		return GamlHyperlinkDetector.class;
	}

	@Override
	public void configureBracketMatchingAction(final Binder binder) {
		// actually we want to override the first binding only...
		binder.bind(IActionContributor.class).annotatedWith(Names.named("foldingActionGroup")).to( //$NON-NLS-1$
				GamaFoldingActionContributor.class);
		binder.bind(IActionContributor.class).annotatedWith(Names.named("bracketMatcherAction")).to( //$NON-NLS-1$
				org.eclipse.xtext.ui.editor.bracketmatching.GoToMatchingBracketAction.class);
		binder.bind(IPreferenceStoreInitializer.class).annotatedWith(Names.named("bracketMatcherPrefernceInitializer")) //$NON-NLS-1$
				.to(org.eclipse.xtext.ui.editor.bracketmatching.BracketMatchingPreferencesInitializer.class);
		binder.bind(IActionContributor.class).annotatedWith(Names.named("selectionActionGroup")).to( //$NON-NLS-1$
				org.eclipse.xtext.ui.editor.selection.AstSelectionActionContributor.class);
	}

	@Override
	public void configureMarkOccurrencesAction(final Binder binder) {
		binder.bind(IActionContributor.class).annotatedWith(Names.named("markOccurrences"))
				.to(GamlMarkOccurrenceActionContributor.class);
		binder.bind(IPreferenceStoreInitializer.class).annotatedWith(Names.named("GamlMarkOccurrenceActionContributor")) //$NON-NLS-1$
				.to(GamlMarkOccurrenceActionContributor.class);
	}

	@Override
	public Class<? extends IResourceForEditorInputFactory> bindIResourceForEditorInputFactory() {
		return ResourceForIEditorInputFactory.class;
	}

	@Override
	public Class<? extends IContentOutlinePage> bindIContentOutlinePage() {
		return GamlOutlinePage.class;
	}

	@Override
	public Class<? extends IImageHelper> bindIImageHelper() {
		return GamlImageHelper.class;
	}

	@Override
	public Class<? extends IImageDescriptorHelper> bindIImageDescriptorHelper() {
		return GamlImageHelper.class;
	}

	@Override
	public void configureIOutlineContribution$Composite(final Binder binder) {
		binder.bind(IPreferenceStoreInitializer.class).annotatedWith(IOutlineContribution.All.class)
				.to(IOutlineContribution.Composite.class);
	}

	@Override
	public Class<? extends AbstractEditStrategyProvider> bindAbstractEditStrategyProvider() {
		return GamaAutoEditStrategyProvider.class;
	}

	@Override
	public void configureToggleSortingOutlineContribution(final Binder binder) {
		binder.bind(IOutlineContribution.class).annotatedWith(IOutlineContribution.Sort.class)
				.to(GamlSortOutlineContribution.class);
	}

	@Override
	public void configureToggleLinkWithEditorOutlineContribution(final Binder binder) {
		binder.bind(IOutlineContribution.class).annotatedWith(IOutlineContribution.LinkWithEditor.class)
				.to(GamlLinkWithEditorOutlineContribution.class);
	}

	@Override
	@SingletonBinding
	public Class<? extends TemplateStore> bindTemplateStore() {
		return GamlTemplateStore.class;
	}

	@Override
	public Class<? extends IReconciler> bindIReconciler() {
		return GamlReconciler.class;
	}

}
