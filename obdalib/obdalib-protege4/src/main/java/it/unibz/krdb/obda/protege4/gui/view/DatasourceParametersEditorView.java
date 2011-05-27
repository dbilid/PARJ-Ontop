package it.unibz.krdb.obda.protege4.gui.view;

import it.unibz.krdb.obda.gui.swing.panel.DatasourceParameterEditorPanel;
import it.unibz.krdb.obda.model.impl.OBDAModelImpl;
import it.unibz.krdb.obda.protege4.core.OBDAPluginController;

import java.awt.BorderLayout;

import org.apache.log4j.Logger;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

public class DatasourceParametersEditorView extends AbstractOWLViewComponent {
	/**
	 * 
	 */
	private static final long		serialVersionUID	= 1L;
	private static final Logger		log					= Logger.getLogger(DatasourceParametersEditorView.class);

	DatasourceParameterEditorPanel	panel				= null;

	OBDAPluginController apic = null;
	@Override
	protected void disposeOWLView() {
//		apic.removeListener(this);
	}

	@Override
	protected void initialiseOWLView() throws Exception {

		
		apic = getOWLEditorKit().get(OBDAModelImpl.class.getName());
//		apic.addListener(this);
		
		panel = new DatasourceParameterEditorPanel(apic.getOBDAManager().getDatasourcesController());

		add(panel, BorderLayout.CENTER);
		log.debug("Datasource parameter view Component initialized");

	}

//	@Override
//	public void obdaModelChanged(OBDAModel oldmodel, OBDAModel newmodel) {
//		OBDAPluginController apic = getOWLEditorKit().get(OBDAModel.class.getName());
//		panel.setDatasourcesController(newmodel.getDatasourcesController());
//	}
}