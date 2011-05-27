package inf.unibz.it.obda.gui.swing.treemodel;

import inf.unibz.it.obda.model.OBDAMappingAxiom;
import inf.unibz.it.obda.model.impl.RDBMSSQLQuery;

/*
 * @author
 * This filter receives a string in the constructor and returns true if any mapping contains the string in the body.
 *
 */
public class MappingSQLStringTreeModelFilter implements
		TreeModelFilter<OBDAMappingAxiom> {

	private final String srtSQLStringTreeModelFilter;

	/**
	 * @param srtSQLStringTreeModelFilter
	 * Constructor of the filter
	 */
	public MappingSQLStringTreeModelFilter(String srtSQLStringTreeModelFilter) {
		this.srtSQLStringTreeModelFilter=srtSQLStringTreeModelFilter;
	}


	/* (non-Javadoc)
	 * @see inf.unibz.it.obda.gui.swing.treemodel.filter.TreeModelFilter#match(java.lang.Object)
	 */
	@Override
	public boolean match(OBDAMappingAxiom object) {
		// TODO Auto-generated method stub
		boolean filterValue = false;
		OBDAMappingAxiom mapping = object;
		RDBMSSQLQuery bodyquery = (RDBMSSQLQuery) mapping.getSourceQuery();
		if (bodyquery.toString().indexOf(srtSQLStringTreeModelFilter) != -1)
			filterValue = true;
		return filterValue;
	}
}