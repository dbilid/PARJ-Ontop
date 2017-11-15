package madgik.exareme.master.queryProcessor.sparql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import it.unibz.inf.ontop.model.CQIE;
import it.unibz.inf.ontop.model.DatalogProgram;
import it.unibz.inf.ontop.model.Function;
import it.unibz.inf.ontop.model.Predicate;
import it.unibz.inf.ontop.model.Term;
import it.unibz.inf.ontop.model.Variable;
import it.unibz.inf.ontop.model.impl.OBDAVocabulary;
import madgik.exareme.master.queryProcessor.decomposer.dag.Node;
import madgik.exareme.master.queryProcessor.decomposer.dag.NodeHashValues;
import madgik.exareme.master.queryProcessor.decomposer.query.Column;
import madgik.exareme.master.queryProcessor.decomposer.query.Constant;
import madgik.exareme.master.queryProcessor.decomposer.query.NonUnaryWhereCondition;
import madgik.exareme.master.queryProcessor.decomposer.query.Output;
import madgik.exareme.master.queryProcessor.decomposer.query.Selection;
import madgik.exareme.master.queryProcessor.decomposer.query.Table;

public class DagCreatorDatalog {

	private DatalogProgram pq;
	private NodeHashValues hashes;
	private int alias;
	private IdFetcher fetcher;

	public DagCreatorDatalog(DatalogProgram q, int partitions, NodeHashValues hashes, IdFetcher fetcher) {
		super();
		this.pq = q;
		this.hashes = hashes;
		this.fetcher = fetcher;
	}

	public Node getRootNode() throws SQLException {
		CQIE first=pq.getRules().get(0);
		System.out.println(first);
		Node projection = new Node(Node.AND, Node.PROJECT);
		alias = 1;
			Map<JoinClassMap, Node> eqClassesToNodes = new HashMap<JoinClassMap, Node>();

			getNodeFromExpression(eqClassesToNodes, first);
			List<JoinClassMap> keys = new ArrayList<JoinClassMap>(eqClassesToNodes.keySet());
			JoinClassMap current = keys.remove(0);
			Node top = eqClassesToNodes.get(current);

			while (!keys.isEmpty()) {
				boolean compatibleFound = false;
				for (int i = 0; i < keys.size(); i++) {
					JoinClassMap next = keys.get(i);
					List<NonUnaryWhereCondition> joins = current.merge(next);
					if (!joins.isEmpty()) {
						keys.remove(i);
						Node joinNode = new Node(Node.AND, Node.JOIN);
						joinNode.setObject(joins.get(0));
						joinNode.addChild(top);
						joinNode.addChild(eqClassesToNodes.get(next));
						joinNode.addAllDescendantBaseTables(top.getDescendantBaseTables());
						joinNode.addAllDescendantBaseTables(eqClassesToNodes.get(next).getDescendantBaseTables());
						Node newTop = new Node(Node.OR);
						newTop.addChild(joinNode);
						newTop.addAllDescendantBaseTables(joinNode.getDescendantBaseTables());
						top = newTop;
						compatibleFound = true;
						hashes.put(joinNode.computeHashIDExpand(), joinNode);
						hashes.put(newTop.computeHashIDExpand(), newTop);
						for (int k = 1; k < joins.size(); k++) {
							Node joinNode2 = new Node(Node.AND, Node.JOIN);
							joinNode2.setObject(joins.get(k));
							joinNode2.addChild(top);
							joinNode2.addAllDescendantBaseTables(top.getDescendantBaseTables());
							Node newTop2 = new Node(Node.OR);
							newTop2.addChild(joinNode2);
							newTop2.addAllDescendantBaseTables(joinNode2.getDescendantBaseTables());
							top = newTop2;
							hashes.put(joinNode2.computeHashIDExpand(), joinNode2);
							hashes.put(newTop2.computeHashIDExpand(), newTop2);
						}
						break;
					}
				}
				if (!compatibleFound) {
					throw new SQLException("Input query contains cartesian product. Currently not suppoted");
				}

			}
			projection.addChild(top);
			// Set<Column> projected=new HashSet<Column>();

			madgik.exareme.master.queryProcessor.decomposer.query.Projection prj = new madgik.exareme.master.queryProcessor.decomposer.query.Projection();
			projection.setObject(prj);
			for(Term t:first.getHead().getTerms()){
				if(t instanceof Variable){
					Variable varT=(Variable) t;
				Column proj = current.getFirstColumn(varT.getName());
				// Column proj= new
				// Column(current.getFirstColumn(pe.getSourceName()).getAlias(),
				// pe.getSourceName());
				// projected.add(proj);
				prj.addOperand(new Output(varT.getName(), proj));
				}
				else{
					System.out.println("what9??? "+t);
				}
			}
			// System.out.println(projection.dotPrint(new HashSet<Node>()));
			Node root = new Node(Node.OR);
			root.addChild(projection);
			return root;
			// Map<String, Set<Column>> eqClasses=new HashMap<String,
			// Set<Column>>();

		

	}

	private JoinClassMap getNodeForTriplePattern(Function atom, Node top) throws SQLException {
		String predString;
		boolean selection = false;
		String aliasString = null;
		// Node baseTable=new Node(Node.OR);
		Table predTable = null;
		Node selNode = new Node(Node.AND, Node.SELECT);
		Selection s = new Selection();
		selNode.setObject(s);
		JoinClassMap result = new JoinClassMap();
		//Var predicate = atom.getPredicateVar();
		Term subject = atom.getTerm(0);
		Term object = atom.getTerm(1);
		
			predString = atom.getFunctionSymbol().getName().replace("adp.", "");
			aliasString = "alias" + alias;
			predTable = new Table(predString, aliasString);

			// baseTable.setObject(predTable);
			alias++;
		
		if (subject instanceof Variable){
			Variable subVar=(Variable) subject;
			String varString = subVar.getName();
			// if(eqClasses.containsKey(varString)){
			// Set<Column> tablesForVar=eqClasses.get(varString);
			// joinCondition.setLeftOp(tablesForVar.iterator().next());
			Column newCol = new Column(aliasString, "s");
			result.add(varString, newCol);
			// joinCondition.setRightOp(newCol);
			// tablesForVar.add(newCol);
			// }
			// else{
			// Set<Column> tablesForVar=new HashSet<Column>();
			// tablesForVar.add(new Column(aliasString, "s"));
			// eqClasses.put(varString, tablesForVar);
			// }
		} else if(subject instanceof it.unibz.inf.ontop.model.Constant){
			createSelection(selNode, selection, subject, aliasString, "s");
			selection = true;
		}
		else{
			System.out.println("what???8 "+subject);
		}
		if (object instanceof Variable) {
			Variable objVar=(Variable) object;
			String varString = objVar.getName();

			if (result.containsVar(varString)) {
				throw new SQLException("same var in subject and object not supported yet");
			}

			Column newCol = new Column(aliasString, "o");
			// joinCondition.setRightOp(newCol);
			result.add(varString, newCol);

		} else if(object instanceof it.unibz.inf.ontop.model.Constant){
			createSelection(selNode, selection, object, aliasString, "o");
			selection = true;
		}
		else{
			System.out.println("what???9 "+object);
		}
		if (selection) {
			Node baseNode = new Node(Node.OR);
			baseNode.setObject(predTable);
			hashes.put(baseNode.computeHashIDExpand(), baseNode);
			selNode.addChild(baseNode);
			hashes.put(selNode.computeHashIDExpand(), selNode);
			top.addChild(selNode);
		} else {
			top.setObject(predTable);

		}
		hashes.put(top.computeHashIDExpand(), top);
		top.addDescendantBaseTable(aliasString);
		return result;
	}

	private void createSelection(Node selNode, boolean selection, Term subject, String aliasString, String sOrO)
			throws SQLException {
		it.unibz.inf.ontop.model.Constant subjConst=(it.unibz.inf.ontop.model.Constant) subject;
		// Selection s=null;
		// if(selection){
		Selection s = (Selection) selNode.getObject();
		// }
		// else{
		// selNode=new Node(Node.AND, Node.SELECT);
		// s=new Selection();
		// selNode.setObject(s);
		// }
		NonUnaryWhereCondition nuwc = new NonUnaryWhereCondition();
		nuwc.setOperator("=");
		nuwc.setLeftOp(new Column(aliasString, sOrO));
		nuwc.setRightOp(new Constant(fetcher.getIdForUri(subjConst.getValue())));
		s.addOperand(nuwc);

	}

	private void getNodeFromExpression(Map<JoinClassMap, Node> eqClassesToNodes, CQIE first) throws SQLException {
		for(Function atom:first.getBody()){
			Predicate pred=atom.getFunctionSymbol();
			if(pred == OBDAVocabulary.DISJUNCTION){
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
							getNodeForTriplePattern(conjAtom, top);
							String subOrObj=projectedVars.get(conjAtom.getTerm(0));
							top.setSubjectIsFirst(subOrObj!=null && subOrObj.equals("s"));
							
							union.addChild(top);
							
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
				String aliasString="alias"+alias;
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
				hashes.put(unionOr.computeHashIDExpand(), unionOr);
			} else if(pred.getName().startsWith("adp.prop")){
				Node top = new Node(Node.OR);
				eqClassesToNodes.put(getNodeForTriplePattern(atom, top), top);
			}
			else{
				System.out.println("what7??? "+pred);
			}
		}

	}

}