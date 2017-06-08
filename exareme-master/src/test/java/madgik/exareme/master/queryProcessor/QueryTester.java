package madgik.exareme.master.queryProcessor;

import it.unibz.inf.ontop.io.ModelIOManager;
import it.unibz.inf.ontop.model.DatalogProgram;
import it.unibz.inf.ontop.model.OBDADataFactory;
import it.unibz.inf.ontop.model.OBDADataSource;
import it.unibz.inf.ontop.model.OBDAModel;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWL;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLConfiguration;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLConnection;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLFactory;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLResultSet;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLStatement;
import it.unibz.inf.ontop.r2rml.R2RMLReader;
import madgik.exareme.master.db.DBManager;
import madgik.exareme.master.queryProcessor.decomposer.dag.Node;
import madgik.exareme.master.queryProcessor.decomposer.dag.NodeHashValues;
import madgik.exareme.master.queryProcessor.estimator.NodeSelectivityEstimator;
import madgik.exareme.master.queryProcessor.sparql.DagCreator;
import madgik.exareme.master.queryProcessor.sparql.DagCreatorDatalog;
import madgik.exareme.master.queryProcessor.sparql.IdFetcher;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryTester {

	// Handy globals:
	private QuestOWL reasoner;
	private QuestOWLConnection conn;

	// Constructor args:
	private String owlfile;
	private String obdafile;
	private String constraints_file;
	private String tmap_conf_file;
	private String[] query_files;
	private int timeout; // Timeout for each query in seconds.
	private boolean run_sql;
	private String outputfolder;
	private IdFetcher fetcher;
	
	private StringBuffer obdaFile;
	private String dir="/media/dimitris/T/test2/";
	private int partitions=4;

	// For R2RML
	private boolean is_r2rml;
	private String db_creds_file;
	private String db_name;
	private String dbuser;
	private String dbpassword;
	private String jdbc_url;
	private String jdbc_driver;
	

	// Timing identifiers and strings.
	private final String TOTAL = "Total";
	private final String READQUERY = "Inputting";
	private final String UNFOLDING = "Unfolding";
	private final String RETRIEVING = "Retrieving";
	private final String OUTPUTTING = "Outputting";

	Logger log = LoggerFactory.getLogger(this.getClass());
	private String histograms;
	private DBManager m;

	public static void main(String[] args) throws IOException {

		System.out.println("GO!");

		if (args.length < 6) {
			System.out.println("Usage");
			System.out.println(
					" QueryTester output mode owlfile obdafile [db_creds_file] constraints tmap_conf_file queryfiles...\n");
			System.out.println(" output         :  path to folder for writing output");
			System.out.println(
					" sql|<timeout>  :  sql for only sql output, otherwise timeout in seconds. 0 for running sql without timeout");
			System.out.println(" owlfile        :  path to OWL file");
			System.out.println(" obdafile       :  path to OBDA or R2RML file");
			System.out.println("[ db_creds_file :  if R2RML, path to file with db credentials ]");
			System.out.println("  constraints   :  0 or path to file with external db constraints");
			System.out.println("  tmap_conf_file   :  0 or path to file with tmap configuration");
			System.out.println(" queryfiles     :  paths SPARQL query files\n");
			System.out.println(" histograms     :  paths to json histogram file\n");
			System.exit(0);
		}

		// args counter. NB! Keep the args in order, due to i++.
		int i = 0;

		String output = args[i++].trim();

		boolean run_sql;
		int timeout = 0;
		if (args[i].equals("sql"))
			run_sql = false;
		else {
			run_sql = true;
			try {
				timeout = Integer.parseInt(args[i]);
			} catch (NumberFormatException e) {
				System.err.println(
						"Error: argument no. " + i + ", '" + args[i] + "' must be the string 'sql' or an integer");
				System.exit(0);
			}
		}
		i++;

		String owlfile = args[i++].trim();
		String obdafile = args[i++].trim();
		boolean is_r2rml = obdafile.indexOf(".obda") == -1 ? true : false;
		String db_creds_file = null;
		if (is_r2rml)
			db_creds_file = args[i++].trim();
		String constraints_file = args[i++].trim();
		String tmap_conf_file = args[i++].trim();
		// String[] query_files = Arrays.copyOfRange(args, i, args.length);
		String[] query_files = readFilesFromDir(args[i++]);
		String histograms = args[i++].trim();

		// for(int test_no = 0; test_no < 5; test_no++){
		QueryTester tester;
		

		/*
		 * tester = new QueryTester (owlfile, obdafile, constraints_file,
		 * is_r2rml, query_files, run_sql, timeout, output + "/" + test_no +
		 * "KEYS_TMAP", db_creds_file, tmap_conf_file); tester.runQueries();
		 * Thread.sleep(7200000);
		 * 
		 * tester = new QueryTester (owlfile, obdafile, constraints_file,
		 * is_r2rml, query_files, run_sql, timeout, (output + "/" + test_no +
		 * "KEYS"), db_creds_file, null); tester.runQueries();
		 * Thread.sleep(7200000);
		 */
		tester = new QueryTester(owlfile, obdafile, constraints_file, is_r2rml, query_files, run_sql, timeout,
				output + "/" + "666" + "NOTUNING", db_creds_file, null, histograms);
		tester.runQueries();
		// Thread.sleep(7200000);

		// }

	}

	

	private static String[] readFilesFromDir(String string) throws IOException {
		File folder = new File(string);
		File[] listOfFiles = folder.listFiles();
		List<String> files = new ArrayList<String>();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() && listOfFiles[i].getCanonicalPath().endsWith("02.q")) {
				files.add(listOfFiles[i].getCanonicalPath());
			}
		}
		java.util.Collections.sort(files);
		return files.toArray(new String[files.size()]);
	}

	/**
	 * db_creds_file is needed for r2rml. db_creds_file must be a valid file
	 * name String if is_r2rml is true. for .obda the db_creds_file is
	 * disregarded and may be null
	 * 
	 * @param histograms
	 **/
	public QueryTester(String owlfile, String obdafile, String constraints_file, boolean is_r2rml, String[] query_files,
			boolean run_sql, int timeout, String output, String db_creds_file, String tmap_conf_file,
			String histograms) {
		this.owlfile = owlfile;
		this.obdafile = obdafile;
		this.is_r2rml = is_r2rml;
		this.query_files = query_files;
		this.run_sql = run_sql;
		this.timeout = timeout;
		this.outputfolder = output;
		this.db_creds_file = db_creds_file;
		this.constraints_file = constraints_file;
		this.tmap_conf_file = tmap_conf_file;
		this.histograms = histograms;
	}

	/**
	 * Returns everything after the last space, and except the end of line
	 * Assumes input is a single line from an obda db cred spec
	 **/
	private String get_stuff_after_space(String line) {
		return (line.split("\\s"))[1];
	}

	/**
	 * Reads database credentials in the .obda format used by ontop
	 **/
	private void read_db_credentials(String db_creds_file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(db_creds_file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf("connectionUrl") == 0)
					this.jdbc_url = this.get_stuff_after_space(line);
				else if (line.indexOf("username") == 0)
					this.dbuser = this.get_stuff_after_space(line);
				else if (line.indexOf("password") == 0)
					this.dbpassword = this.get_stuff_after_space(line);
				else if (line.indexOf("driverClass") == 0)
					this.jdbc_driver = this.get_stuff_after_space(line);
			}
		} catch (IOException e) {
			System.out.println("Error when reading db credentials file \"" + db_creds_file + "\"");
			e.printStackTrace();
		}
	}

	public void initQuest(String owlfile, String obdafile) throws Exception {
		// Loading the OWL ontology from the file as with normal OWLReasoners
		
		
		m = new DBManager();
		warmUpDBManager(partitions, dir, m);
		
		createObdaFile();
		
		
		
		myLog("Loading ontology");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));
		myDone();

		// Loading the OBDA model

		OBDAModel obdaModel;
		OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
		

		if (!this.is_r2rml) {
			myLog("Loading obda file");
			obdaModel = fac.getOBDAModel();
			ModelIOManager ioManager = new ModelIOManager(obdaModel);
			ioManager.load(new ByteArrayInputStream(obdaFile.toString().getBytes()));
			//ioManager.load(obdafile);
		} else {
			myLog("Loading r2rml file");
			R2RMLReader reader = new R2RMLReader(obdafile);
			String sourceUrl = obdafile;
			this.read_db_credentials(this.db_creds_file);
			OBDADataSource dataSource = fac.getJDBCDataSource(sourceUrl, jdbc_url, dbuser, dbpassword, jdbc_driver);
			obdaModel = reader.readModel(dataSource);
		}

		myDone();

		// Preparing the configuration for the new Quest instance, we need to
		// make sure it will be setup for "virtual ABox mode"
		myLog("Configuring Quest");
		QuestPreferences p = new QuestPreferences();
		p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
		p.setCurrentValueOf(QuestPreferences.OBTAIN_FULL_METADATA, QuestConstants.FALSE);
		p.setCurrentValueOf(QuestPreferences.SQL_GENERATE_REPLACE, QuestConstants.FALSE);
		p.setCurrentValueOf(QuestPreferences.REWRITE, QuestConstants.TRUE);
		//p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.PANTELIS);
		// p.setCurrentValueOf(QuestPreferences.DISTINCT_RESULTSET,
		// QuestConstants.TRUE);
		p.setCurrentValueOf(QuestPreferences.REFORMULATION_TECHNIQUE, QuestConstants.TW);
		myDone();

		// Creating the instance of the reasoner using the factory. Remember
		// that the RDBMS that contains the data must be already running and
		// accepting connections.
		QuestOWLConfiguration.Builder configBuilder = QuestOWLConfiguration.builder();
		configBuilder.obdaModel(obdaModel);
		configBuilder.preferences(p);
		QuestOWLConfiguration config = configBuilder.build();
		QuestOWLFactory factory = new QuestOWLFactory();
		// QuestOWLConfiguration config =
		// QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
		reasoner = factory.createReasoner(ontology, config);

		// Map<String, Set<Set<String>>> tMapSQL=reasoner.getTMappingsSQL();
		/*
		 * QuestOWLFactory factory = configBuilder.
		 * factory.setOBDAController(obdaModel); factory.setPreferenceHolder(p);
		 */
		if (!this.constraints_file.equals("0")) {
			myLog("Setting user constraints");
		}
		/*
		 * if(!this.tmap_conf_file.equals("0")){ myLog("Setting tmap config");
		 * TMappingExclusionConfig conf =
		 * TMappingExclusionConfig.parseFile(this.tmap_conf_file);
		 * factory.setExcludeFromTMappingsPredicates(conf); }
		 */
		// myLog("Creating reasoner");
		// reasoner = factory.createReasoner(ontology, config);
		// myDone();

		// Now we are ready to query. Querying is done as with JDBC. First we
		// get a connection, from the connection we get a Statement and using
		// the statement we query.
		myLog("Getting connection");
		conn = reasoner.getConnection();
		

		myDone();
	}

	private static void warmUpDBManager(int partitions, String database, DBManager m) throws SQLException {
		System.out.println("warming up DB manager...");
		long start=System.currentTimeMillis();
		List<Connection> cons=new ArrayList<Connection>(partitions+3);
		for(int i=0;i<cons.size();i++){
			cons.add(m.getConnection(database));
		}
		for(int i=0;i<cons.size();i++){
			cons.get(i).close();
		}
		System.out.println("finished warming up DBManager in "+(System.currentTimeMillis() - start+ " ms"));
		
	}


	private void createObdaFile() throws SQLException {
		obdaFile=new StringBuffer();
		obdaFile.append("[SourceDeclaration]");
		obdaFile.append("\n");
		obdaFile.append("sourceUri\tsparql");
		obdaFile.append("\n");
		obdaFile.append("connectionUrl\tjdbc:fedadp:"+dir);
		obdaFile.append("\n");
		obdaFile.append("username\ttest");
		obdaFile.append("\n");
		obdaFile.append("password\ttest");
		obdaFile.append("\n");
		obdaFile.append("driverClass\tmadgik.exareme.jdbc.embedded.AdpDriver");
		obdaFile.append("\n");
		
		obdaFile.append("\n");
		obdaFile.append("[MappingDeclaration] @collection [[");
		obdaFile.append("\n");
		
		Connection c=m.getConnection(dir);
		fetcher = new IdFetcher(c);
		fetcher.loadProperties();
		
		Statement st=c.createStatement();
		int mappingId=0;
		for(String property:fetcher.getProperties()){
			if(property.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")){
				String propNo="prop"+fetcher.getIdForProperty(property);
				StringBuffer getTypes=new StringBuffer();
				String del="";
				String distinct="";
				if(partitions==1){
					distinct=" distinct ";
				}
				for(int p=0;p<partitions-1;p++){
					getTypes.append(del);
					getTypes.append(" select ");
					getTypes.append(distinct);
					getTypes.append("o from ");
					getTypes.append(propNo);
					getTypes.append("_");
					getTypes.append(p);
					del= "\n UNION \n";
				}
				ResultSet rs2=st.executeQuery(getTypes.toString());
				while(rs2.next()){
					int o=rs2.getInt(1);
					obdaFile.append("mappingId\tmapp");
					obdaFile.append(mappingId);
					mappingId++;
					String type=fetcher.getUriForId(o);
					obdaFile.append("\n");
					obdaFile.append("target\t");
					obdaFile.append("<{s}> <");
					obdaFile.append("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
					obdaFile.append("> <");
					obdaFile.append(type);
					obdaFile.append("> .\n");
					obdaFile.append("source\t");
					obdaFile.append("select s from ");
					obdaFile.append(propNo);
					obdaFile.append(" where o='");
					obdaFile.append(type);
					obdaFile.append("'");
					
					obdaFile.append("\n");
					obdaFile.append("\n");	
				}
				continue;
			}
			obdaFile.append("mappingId\tmapp");
			obdaFile.append(mappingId);
			mappingId++;
			obdaFile.append("\n");
			obdaFile.append("target\t");
			obdaFile.append("<{s}> <");
			obdaFile.append(property);
			obdaFile.append("> <{o}> .\n");
			obdaFile.append("source\t");
			obdaFile.append("select s, o from prop");
			obdaFile.append(fetcher.getIdForProperty(property));
			obdaFile.append("\n");
			obdaFile.append("\n");	
			
		}
		
		obdaFile.append("]]");
		
		
	}



	public void runQueries() {
		try {
			initQuest(owlfile, obdafile);
			if (conn == null) {
				if (this.is_r2rml)
					System.err.println(
							"Could not load connection with " + this.db_creds_file + " to '" + this.jdbc_url + "'");
				else
					System.err.println("Could not load connection with obdafile " + obdafile);

				System.exit(0);
			}
			// Run all queries
			for (String queryfile : query_files) {
				runQuery(queryfile);
				Thread.sleep(10);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null)
					conn.close();
				if (reasoner != null)
					reasoner.dispose();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void runQuery(String queryfile) {
		try {
			Timer timer = new Timer();
			logIn(timer, TOTAL);
			System.out.println("query:" + queryfile);
			String query = readQuery(queryfile, timer);
			conn.setAutoCommit(false);
			QuestOWLStatement st = conn.createStatement();
			st.setFetchSize(1000);

			boolean mysql = reasoner.getQuestInstance().getMetaData().getDriverName().toLowerCase().contains("mysql");
			if (mysql) {
				st.setFetchSize(Integer.MIN_VALUE);
			}
			getPantelisDatalog(st, query, outputfolder + "/" + queryfile + ".sql", timer);
			//writeUnfoldedSQL(st, query, outputfolder + "/" + queryfile + ".sql", timer);

			if (this.run_sql) {
				setQueryTimeout(st, timeout);
				// Retrieving results:
				QuestOWLResultSet result = getResultSet(st, query, timer);
				if (result != null) {
					// Outputting and counting results:
					writeResultSet(result, outputfolder + "/" + queryfile + ".ans",
							outputfolder + "/" + queryfile + ".out", timer);
					result.close();
				}
			}

			st.close();
			logOut(timer, TOTAL);

			// Output time data
			String time = timer.printResults();
			writeFile(outputfolder + "/" + queryfile + ".time", time);

		} catch (Exception e) {
			System.err.println("Error executing query. Message: ");
			e.printStackTrace();
		}
	}

	////////////////////////////////////////
	// Query helper functions

	private String readQuery(String filename, Timer timer) {
		// Reading sparql query
		logIn(timer, READQUERY);
		String query = readFile(filename);
		logOut(timer, READQUERY);
		return query;
	}
	
	private void getPantelisDatalog(QuestOWLStatement st, String query, String file, Timer timer) {
		byte[] sql = null;
		logIn(timer, UNFOLDING);
		try {
			NodeSelectivityEstimator nse = new NodeSelectivityEstimator(
					dir + "histograms.json");
			NodeHashValues hashes = new NodeHashValues();
			hashes.setSelectivityEstimator(nse);
			DatalogProgram result = st.getUnfoldingForPantelis(query);
			DagCreatorDatalog creator = new DagCreatorDatalog(result, partitions, hashes, fetcher);

			Node root = creator.getRootNode();
			
		} catch (Exception e) {
			System.err.println("Error unfolding to SQL. ");
			e.printStackTrace();
		}
		
	}

	private void writeUnfoldedSQL(QuestOWLStatement st, String query, String file, Timer timer) {
		byte[] sql = null;
		logIn(timer, UNFOLDING);
		try {
			String result = st.getUnfolding(query);
			//String delimiter = "";
			StringBuffer q = new StringBuffer();
			//for (int i = 0; i < result.size(); i++) {
				//q.append(delimiter);
				q.append(result);
				//delimiter = "\n";
			//}
			sql = q.toString().getBytes();
		} catch (Exception e) {
			System.err.println("Error unfolding to SQL. ");
			e.printStackTrace();
		}
		logOut(timer, UNFOLDING);
		writeFile(file, sql);
	}

	private void setQueryTimeout(QuestOWLStatement st, int timeout) {
		try {
			st.setQueryTimeout(timeout);
		} catch (Exception e) {
			System.err.println("Error setting query timeout.");
			e.printStackTrace();
		}
	}

	private QuestOWLResultSet getResultSet(QuestOWLStatement st, String query, Timer timer) {
		QuestOWLResultSet result = null;
		String error_message = null;
		logIn(timer, RETRIEVING);
		try {
			result = st.executeTuple(query);
			logOut(timer, RETRIEVING);
		} catch (OWLException e) {
			if (e.getMessage().indexOf("ORA-01013") != -1) {
				error_message = "timeout (" + timeout + " secs)";
				logError(timer, RETRIEVING, error_message);
			} else {
				logError(timer, RETRIEVING, "error (see System.err for detailsr executing sql: " + e.getMessage());
			}
		}

		if (error_message == null && result == null) {
			error_message = "SQL query execution returns NULL";
		}
		if (error_message != null) {
			logError(timer, RETRIEVING, error_message);
			return null;
		}
		return result;
	}

	private void writeResultSet(QuestOWLResultSet result, String sumfile, String outputfile, Timer timer) {
		logIn(timer, OUTPUTTING);
		writeFile(sumfile, getResultString(result));
		logOut(timer, OUTPUTTING);
	}

	private String getResultString(QuestOWLResultSet result) {
		// String sep = "\t";
		// StringBuffer str = new StringBuffer(500);
		int count = -1;

		try {
			// header
			// int columns = result.getColumnCount();
			// for (int c = 0; c < columns; c++) {
			// String value = result.getSignature().get(c);
			// str.append(value);
			// if (c + 1 < columns)
			// str.append(sep);
			// }
			// str.append("\n");

			count = 0;

			// data
			if (result.nextRow()) {
				count++;
				System.out.println("...first");
			}
			while (result.nextRow()) {
				count += 1;
				// if(count == 10000)
				// return "\\geq 10000" + "\n"+ str.toString();
				// for (int c = 0; c < columns; c++) {
				// String value = result.getOWLObject(c+1).toString();
				// str.append(value);
				// if (c + 1 < columns)
				// str.append(sep);
				// }
				// str.append("\n");
			}

			// count number of results
			// result.last();
			// count = result.getRow();
			System.out.println("results:::" + count);

		} catch (Exception e) {
			System.err.println("Error writing results.");
			e.printStackTrace();
		}

		return count + "\n"; // + str.toString();
	}

	/////////////////////////////////////////////////////
	// File IO

	public static String readFile(String filename) {
		String file = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = null;
			while ((line = reader.readLine()) != null) {
				file += line + "\n";
			}
		} catch (Exception e) {
			System.err.println("Error reading file: " + filename);
			e.printStackTrace();
		}
		return file;
	}

	public static void writeFile(String filename, String string) {
		writeFile(filename, string.getBytes());
	}

	public static void writeFile(String filename, byte[] string) {
		try {
			File file = new File(filename);
			file.getParentFile().mkdirs();
			OutputStream out = new FileOutputStream(file);
			out.write(string);
			out.close();
		} catch (Exception e) {
			System.err.println("Error writing file: " + filename);
			e.printStackTrace();
		}
	}

	/////////////////////////////////////////////////////
	// Logging and timing

	private static void logIn(Timer t, String s) {
		t.newTest(s);
		myLog(s);
	}

	private static void logOut(Timer t, String s) {
		t.endTest(s);
		myDone();
	}

	private static void logError(Timer t, String s, String error) {
		t.setError(s, error);
		myFailed();
	}

	private static void myLog(String s) {
		System.out.print(s + "... ");
	}

	private static void myDone() {
		System.out.println("DONE!");
	}

	private static void myFailed() {
		System.out.println("FAILED!");
	}

}
