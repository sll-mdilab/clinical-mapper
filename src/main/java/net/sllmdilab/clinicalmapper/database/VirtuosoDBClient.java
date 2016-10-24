package net.sllmdilab.clinicalmapper.database;

import net.sllmdilab.commons.exceptions.DatabaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

public class VirtuosoDBClient {
	private Logger logger = LoggerFactory.getLogger(VirtuosoDBClient.class);

	private final String graphUri;
	private final String rdfUser;
	private final String rdfPassword;
	
	public VirtuosoDBClient(String graphUri, String rdfUser, String rdfPassword) {
		this.graphUri = graphUri;
		this.rdfUser = rdfUser;
		this.rdfPassword = rdfPassword;
	}

	public VirtGraph connect() {
		VirtGraph virtGraph = new VirtGraph(null, graphUri, rdfUser, rdfPassword, true);
		virtGraph.setReadFromAllGraphs(true);
		
		return virtGraph;
	}
	
	public void close(VirtGraph virtGraph) {
		virtGraph.close();
	}

	public String sendSparql(VirtGraph virtGraph, String sparqlQuery) throws Exception {
		try {
			QueryExecution qe = VirtuosoQueryExecutionFactory.create(sparqlQuery, virtGraph);
			long start = System.currentTimeMillis();

			ResultSet rs = qe.execSelect();
			String result = ResultSetFormatter.asXMLString(rs);

			
			logger.info("query executed and results fetched in:" + (System.currentTimeMillis() - start) + " msec");

			if (logger.isDebugEnabled()) {
				logger.debug("### Start of query result ###");
				logger.debug(result);
				logger.debug("### End of query result ###");
			}

			return result;

		} catch (Exception e) {
			throw new DatabaseException("query failed:" + sparqlQuery, e);
		}
	}
}
