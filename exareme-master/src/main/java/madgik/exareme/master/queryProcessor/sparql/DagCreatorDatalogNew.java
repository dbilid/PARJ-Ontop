package madgik.exareme.master.queryProcessor.sparql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

import it.unibz.inf.ontop.model.CQIE;
import it.unibz.inf.ontop.model.DatalogProgram;
import it.unibz.inf.ontop.model.Function;
import it.unibz.inf.ontop.model.Predicate;
import it.unibz.inf.ontop.model.Term;
import it.unibz.inf.ontop.model.Variable;
import it.unibz.inf.ontop.model.impl.OBDAVocabulary;
import madgik.exareme.master.queryProcessor.decomposer.dag.Node;
import madgik.exareme.master.queryProcessor.decomposer.dag.NodeHashValues;
import madgik.exareme.master.queryProcessor.decomposer.dp.DPSubLinear;
import madgik.exareme.master.queryProcessor.decomposer.dp.EquivalentColumnClass;
import madgik.exareme.master.queryProcessor.decomposer.dp.EquivalentColumnClasses;
import madgik.exareme.master.queryProcessor.decomposer.query.Column;
import madgik.exareme.master.queryProcessor.decomposer.query.Constant;
import madgik.exareme.master.queryProcessor.decomposer.query.NonUnaryWhereCondition;
import madgik.exareme.master.queryProcessor.decomposer.query.Output;
import madgik.exareme.master.queryProcessor.decomposer.query.SQLQuery;
import madgik.exareme.master.queryProcessor.decomposer.query.Selection;
import madgik.exareme.master.queryProcessor.decomposer.query.Table;

public class DagCreatorDatalogNew {

	private DatalogProgram pq;
	private NodeHashValues hashes;
	private int alias;
	private int unionalias;
	private IdFetcher fetcher;
	private List<Node> tableNodes;
	private List<Table> tables;
	JoinClassMap classes;
	SQLQuery query;
	private List<Integer> filters;
	// list to track filter on base tables, 0 no filter, 1 filter on first, 2
	// filter on second

	public DagCreatorDatalogNew(DatalogProgram q, int partitions, NodeHashValues hashes, IdFetcher fetcher) {
		super();
		this.pq = q;
		this.hashes = hashes;
		this.fetcher = fetcher;
		classes = new JoinClassMap();
		tableNodes = new ArrayList<Node>();
		tables = new ArrayList<Table>();
		query = new SQLQuery();
		filters = new ArrayList<Integer>();
	}

	public SQLQuery getRootNode() throws SQLException {
		CQIE first = pq.getRules().get(0);
		Node projection = new Node(Node.AND, Node.PROJECT);
		alias = 0;
		unionalias=1024;
		// if (pq.getTupleExpr() instanceof Projection) {
		// Projection p = (Projection) pq.getTupleExpr();

		getNodeFromExpression(first);
		EquivalentColumnClasses eqClasses = new EquivalentColumnClasses();
		for (JoinClass jc : classes.getClasses()) {
			if (jc.getColumns().size() > 1) {
				EquivalentColumnClass eqClass = new EquivalentColumnClass(jc.getColumns());
				eqClasses.add(eqClass);
			}
		}
		DPSubLinear dp = new DPSubLinear(tableNodes, eqClasses);
		dp.setNse(hashes.getNse());
		int tableOrder[] = dp.getPlan();
		eqClasses.renew();
		int inserted = 0;
		for (int i = 0; i < tableOrder.length; i++) {
			query.addInputTable(tables.get(tableOrder[i]));
			eqClasses.renew();
			boolean hasJoinOnlyInSecond = true;
			for (int j = 0; j < inserted; j++) {
				Set<NonUnaryWhereCondition> joins = eqClasses.getJoin(tableOrder[i] + 1, tableOrder[j] + 1);
				if (joins != null) {
					for (NonUnaryWhereCondition join : joins) {
						query.addBinaryWhereCondition(join);
						if (hasJoinOnlyInSecond && filters.get(tableOrder[i]) == 0) {
							for (Column c : join.getAllColumnRefs()) {
								if (c.getAlias() == tables.get(tableOrder[i]).getAlias() && c.getColumnName()) {
									hasJoinOnlyInSecond = false;
								}
							}
						}
					}
				}
			}
			if (i > 0) {
				if (!hasJoinOnlyInSecond) {
					tables.get(tableOrder[i]).setInverse(false);
				} else {
					if (filters.get(tableOrder[i]) != 1) {
						tables.get(tableOrder[i]).setInverse(true);
					}
				}
			} else {
				if (filters.get(tableOrder[i]) == 2) {
					tables.get(tableOrder[i]).setInverse(true);
				}
			}
			inserted++;
		}

		// projection.addChild(top);
		// Set<Column> projected=new HashSet<Column>();

		madgik.exareme.master.queryProcessor.decomposer.query.Projection prj = new madgik.exareme.master.queryProcessor.decomposer.query.Projection();
		projection.setObject(prj);
		for (Term t : first.getHead().getTerms()) {
			if (t instanceof Variable) {
				Variable varT = (Variable) t;
				Column proj = classes.getFirstColumn(varT.getName());
				// Column proj= new
				// Column(current.getFirstColumn(pe.getSourceName()).getAlias(),
				// pe.getSourceName());
				// projected.add(proj);
				query.getOutputs().add(new Output(varT.getName(), proj));
				// prj.addOperand(new Output(varT.getName(), proj));
			} else {
				System.out.println("what9??? " + t);
			}
		}

		// System.out.println(projection.dotPrint(new HashSet<Node>()));
		Node root = new Node(Node.OR);
		root.addChild(projection);
		// System.out.println(query.toSQL());

		return query;
		// Map<String, Set<Column>> eqClasses=new HashMap<String,
		// Set<Column>>();

	}

	private void getNodeForTriplePattern(Function atom, Node top, boolean addToTables) throws SQLException {
		int pred;
		boolean selection = false;
		// Node baseTable=new Node(Node.OR);
		Table predTable = null;
		filters.add(0);
		Node selNode = new Node(Node.AND, Node.SELECT);
		Selection s = new Selection();
		selNode.setObject(s);
		String subVar = "";
		// JoinClassMap result = new JoinClassMap();
		Term subject = atom.getTerm(0);
		Term object = atom.getTerm(1);
		String predString = atom.getFunctionSymbol().getName().replace("adp.prop", "");
		pred = Integer.parseInt(predString);
		//pred = (int) fetcher.getIdForProperty(predString);
		
		predTable = new Table(pred, alias);

		// baseTable.setObject(predTable);

		if (subject instanceof Variable) {
			Variable subVarbl = (Variable) subject;
			String varString = subVarbl.getName();

			// joinCondition.setLeftOp(tablesForVar.iterator().next());
			Column newCol = new Column(alias, true);
			classes.add(varString, newCol);
			subVar = varString;
			// joinCondition.setRightOp(newCol);
			// tablesForVar.add(newCol);
			// }
			// else{
			// Set<Column> tablesForVar=new HashSet<Column>();
			// tablesForVar.add(new Column(aliasString, "s"));
			// eqClasses.put(varString, tablesForVar);
			// }
		} else if (subject instanceof it.unibz.inf.ontop.model.Constant) {
			createSelection(selNode, selection, subject, alias, true, addToTables);
			selection = true;
		} else {
			System.out.println("what???8 " + subject);
		}
		if (object instanceof Variable) {
			Variable objVar = (Variable) object;
			String varString = objVar.getName();

			if (subVar.equals(varString)) {
				throw new SQLException("same var in subject and object not supported yet");
			}

			Column newCol = new Column(alias, false);
			// joinCondition.setRightOp(newCol);
			classes.add(varString, newCol);

		} else if (object instanceof it.unibz.inf.ontop.model.Constant) {
			createSelection(selNode, selection, object, alias, false, addToTables);
			selection = true;
		} else {
			System.out.println("what???38 " + object);
		}
		if (selection) {
			Node baseNode = new Node(Node.OR);
			baseNode.setObject(predTable);
			hashes.getNse().makeEstimationForNode(baseNode);
			// hashes.put(baseNode.computeHashIDExpand(), baseNode);
			selNode.addChild(baseNode);
			// hashes.put(selNode.computeHashIDExpand(), selNode);
			top.addChild(selNode);
		} else {
			top.setObject(predTable);

		}
		// hashes.put(top.computeHashIDExpand(), top);
		hashes.getNse().makeEstimationForNode(top);
		top.addDescendantBaseTable("alias" + alias);
		if(addToTables){
			tableNodes.add(top);
			tables.add(predTable);
		}
		// return result;
	}

	private void createSelection(Node selNode, boolean selection, Term sbjOrObj, int aliasString, boolean sOrO, boolean addToQuery)
			throws SQLException {
		// Selection s=null;
		// if(selection){
		it.unibz.inf.ontop.model.Constant con=(it.unibz.inf.ontop.model.Constant) sbjOrObj;
		System.out.println(con.getValue());
		Selection s = (Selection) selNode.getObject();
		// }
		// else{
		// selNode=new Node(Node.AND, Node.SELECT);
		// s=new Selection();
		// selNode.setObject(s);
		// }
		if (sOrO) {
			filters.set(filters.size() - 1, 1);
		} else {
			filters.set(filters.size() - 1, 2);
		}
		
		NonUnaryWhereCondition nuwc = new NonUnaryWhereCondition();
		nuwc.setOperator("=");
		nuwc.setLeftOp(new Column(aliasString, sOrO));
		nuwc.setRightOp(new Constant(fetcher.getIdForUri(con.getValue())));
		s.addOperand(nuwc);
		if(addToQuery){
		query.addBinaryWhereCondition(nuwc);}

	}

	private void getNodeFromExpression(CQIE expr) throws SQLException {
		for (Function atom : expr.getBody()) {
			Predicate pred = atom.getFunctionSymbol();
			if (pred == OBDAVocabulary.DISJUNCTION) {
				alias++;
				unionalias++;
				Map<Variable, String> projectedVars=new HashMap<Variable, String>(2);
				String position="s";
				Node union=new Node(Node.AND, Node.UNIONALL);
				for(Term conj:atom.getTerms()){
					Node top=new Node(Node.OR);
					if(conj instanceof Function){
						Function conjFunct=(Function) conj;
						if(conjFunct.getTerms().size()>1){
							System.out.println("what11???? "+conjFunct);
							}
							Function conjAtom=(Function) conjFunct.getTerm(0);
							getNodeForTriplePattern(conjAtom, top, false);
							String subOrObj=projectedVars.get(conjAtom.getTerm(0));
							top.setSubjectIsFirst(subOrObj!=null && subOrObj.equals("s"));
							
							union.addChild(top);
							//break;
							
					}
					else if(conj instanceof Variable){
						projectedVars.put((Variable)conj, position);
						position="o";
					}
				    else if(conj instanceof it.unibz.inf.ontop.model.Constant){
				    	position="o";
				    }
					else{
						System.out.println("what9???? "+conj);
					}
				}
				Node unionOr=new Node(Node.OR);
				unionOr.addChild(union);
				hashes.getNse().makeEstimationForNode(unionOr);
				
				Table predTable = new Table(unionalias, alias);
				unionOr.addDescendantBaseTable("alias" + alias);
				tableNodes.add(unionOr);
				System.out.println("tables for union alias: "+unionalias);
				System.out.println("projectedVars:"+ projectedVars);
				for(Node u:union.getChildren()){
					if(u.getObject() instanceof Table){
					Table t=(Table)u.getObject();
					if(u.isSubjectFirst()){
						System.out.println(t.getName()+" ");
					}
					else{
						System.out.println("inv"+t.getName()+" ");
					}
					}
					else{
						Selection s=(Selection)u.getChildAt(0).getObject();
						Node table=u.getChildAt(0).getChildAt(0);
						Table t2=(Table)table.getObject();
						if(u.isSubjectFirst()){
							System.out.println(s.toString()+" from "+t2.getName()+" ");
						}
						else{
							System.out.println(s.toString()+" from "+"inv"+t2.getName()+" ");
						}
					}
					
				}
				tables.add(predTable);
				/*String aliasString="alias"+alias;
				alias++;
				Table t=new Table(aliasString, aliasString);
				unionOr.setObject(t);
				unionOr.getDescendantBaseTables().add(aliasString);
				JoinClassMap result = new JoinClassMap();
				for(Variable var:projectedVars.keySet()){
					Column newCol = new Column(aliasString, projectedVars.get(var));
					result.add(var.getName(), newCol);
				}
				
				eqClassesToNodes.put(result, unionOr);
				hashes.put(union.computeHashIDExpand(), union);
				hashes.put(unionOr.computeHashIDExpand(), unionOr);*/

			} else if (pred.getName().startsWith("adp.prop")) {
				Node top = new Node(Node.OR);
				alias++;
				getNodeForTriplePattern(atom, top, true);
			} else {
				System.out.println("what7??? " + pred);
			}

		}

	}

}