package it.unibz.inf.ontop.sesame.repository.test;

/*
 * #%L
 * ontop-test
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.inf.ontop.io.QueryIOManager;
import it.unibz.inf.ontop.model.OBDADataFactory;
import it.unibz.inf.ontop.model.OBDADataSource;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.model.impl.RDBMSourceParameterConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import it.unibz.inf.ontop.owlrefplatform.owlapi.*;
import it.unibz.inf.ontop.querymanager.QueryController;
import it.unibz.inf.ontop.querymanager.QueryControllerEntity;
import it.unibz.inf.ontop.querymanager.QueryControllerQuery;
import it.unibz.inf.ontop.sql.JDBCConnectionManager;
import junit.framework.TestCase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.unibz.inf.ontop.sesame.SemanticIndexManager;

import java.io.File;
import java.net.URI;
import java.sql.Connection;

/**
 * Tests if QuestOWL can be initialized on top of an existing semantic index
 * created by the SemanticIndexManager.
 */
public class SemanticIndexManagerLUBMMySQLTest extends TestCase {

	String driver = "org.postgresql.Driver";
	String url = "jdbc:postgresql://127.0.0.1/tmp";
	String username = "postgres";
	String password = "gray769watt724!@#";
	
	String owlfile = "../quest-owlapi3/src/test/resources/test/lubm-ex-20-uni1/merge.owl";

	OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OBDADataSource source;
	
	Logger log = LoggerFactory.getLogger(this.getClass());

	public SemanticIndexManagerLUBMMySQLTest() throws Exception {
		manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument(new File(owlfile));

		source = fac.getDataSource(URI.create("http://www.obda.org/ABOXDUMP1testx1"));
		source.setParameter(RDBMSourceParameterConstants.DATABASE_DRIVER, driver);
		source.setParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD, password);
		source.setParameter(RDBMSourceParameterConstants.DATABASE_URL, url);
		source.setParameter(RDBMSourceParameterConstants.DATABASE_USERNAME, username);
		source.setParameter(RDBMSourceParameterConstants.IS_IN_MEMORY, "false");
		source.setParameter(RDBMSourceParameterConstants.USE_DATASOURCE_FOR_ABOXDUMP, "true");
	}

	@Override
	public void setUp() throws Exception {
		Connection conn = null;
		try {
			conn = JDBCConnectionManager.getJDBCConnectionManager().createConnection(source);
			SemanticIndexManager simanager = new SemanticIndexManager(ontology, conn);
			simanager.setupRepository(true);
		} catch (Exception e) {
			throw e;
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}
	
	@Override
	public void tearDown() {
		Connection conn = null;
		try {
			conn = JDBCConnectionManager.getJDBCConnectionManager().createConnection(source);

			SemanticIndexManager simanager = new SemanticIndexManager(ontology, conn);
			simanager.dropRepository();
		} catch (Exception e) {
			
		} finally {
			if (conn != null) {
				try {
				conn.close();
				} catch (Exception e) {
					
				}
			} 
		}
	}

	public void test2RestoringAndLoading() throws Exception {
		
		Connection conn = null;
		try {
			conn = JDBCConnectionManager.getJDBCConnectionManager().createConnection(source);
			SemanticIndexManager simanager = new SemanticIndexManager(ontology, conn);
			//simanager.restoreRepository();
			int inserts = simanager.insertData(ontology, 20000, 5000);
			simanager.updateMetadata();
			log.debug("Inserts: {}", inserts);
//			assertEquals(30033, inserts);
		} catch (Exception e) {
			throw e;
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}

	public void test3InitializingQuest() throws Exception {
		QuestOWLFactory fac = new QuestOWLFactory();

		QuestPreferences pref = new QuestPreferences();
		pref.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
		pref.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
		pref.setCurrentValueOf(QuestPreferences.STORAGE_LOCATION, QuestConstants.JDBC);
		pref.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
		pref.setCurrentValueOf(QuestPreferences.JDBC_DRIVER, driver);
		pref.setCurrentValueOf(QuestPreferences.JDBC_URL, url);
		pref.setCurrentValueOf(QuestPreferences.DBUSER, username);
		pref.setCurrentValueOf(QuestPreferences.DBPASSWORD, password);

//		fac.setPreferenceHolder(pref);
//
//		QuestOWL quest = (QuestOWL) fac.createReasoner(ontology);

		QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().preferences(pref).build();
        QuestOWL quest = factory.createReasoner(ontology, config);
        
		
		QuestOWLConnection qconn = (QuestOWLConnection) quest.getConnection();
		QuestOWLStatement st = (QuestOWLStatement) qconn.createStatement();

		QueryController qc = new QueryController();
		QueryIOManager qman = new QueryIOManager(qc);
		qman.load("../quest-owlapi3/src/test/resources/test/treewitness/LUBM-ex-20.q");

		for (QueryControllerEntity e : qc.getElements()) {
			if (!(e instanceof QueryControllerQuery)) {
				continue;
			}
			QueryControllerQuery query = (QueryControllerQuery) e;
			log.debug("Executing query: {}", query.getID() );
			log.debug("Query: \n{}", query.getQuery());
			
			long start = System.nanoTime();
			QuestOWLResultSet res = (QuestOWLResultSet)st.executeTuple(query.getQuery());
			long end = System.nanoTime();
			
			double time = (end - start) / 1000; 
			
			int count = 0;
			while (res.nextRow()) {
				count += 1;
			}
			log.debug("Total result: {}", count );
			log.debug("Elapsed time: {} ms", time);
		}
	}

}
