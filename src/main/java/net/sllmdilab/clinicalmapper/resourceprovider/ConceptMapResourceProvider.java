package net.sllmdilab.clinicalmapper.resourceprovider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.ConceptMap;
import ca.uhn.fhir.model.dstu2.resource.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Parameters.Parameter;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import net.sllmdilab.clinicalmapper.database.VirtuosoDBClient;
import net.sllmdilab.commons.util.ParserUtils;
import virtuoso.jena.driver.VirtGraph;

@Component
public class ConceptMapResourceProvider implements IResourceProvider {

	private static final String URI_SNOMED = "http://snomed.info/sct";
	private static final String URI_IEEEX73 = "urn:std:iso:11073";
	private static final String URI_CLINISOFT = "http://sll-mdilab.net/BodySites/Clinisoft";
	private static final String EQUIVALENT_BROADER = "wider";
	private static final String EQUIVALENT_NARROW = "narrower";
	private static final String EQUIVALENT_SEARCH = "search";
	private static final String EQUIVALENT_EQUAL = "equal";
	private static final String EQUIVALENT_SUBSUME = "subsumes";
	
	private static final String DIRECTION_MAP = "map";
	private static final String DIRECTION_UNMAP = "unmap";
	
	private static final String SEARCH_QUERY_FILEPATH =      "/sparql_queries/SNOMED_CT_term_label_search.sparql";
	private static final String BROADER_X73_QUERY_FILEPATH = "/sparql_queries/SNOMED_IEEEX73_hierarchy_UP.sparql";
	private static final String BROADER_Clinisoft_QUERY_FILEPATH = "/sparql_queries/SNOMED_Clinisoft_hierarchy_UP.sparql";

	private static final String NARROWER_X73_QUERY_FILEPATH = "/sparql_queries/SNOMED_IEEEX73_hierarchy_DOWN.sparql";
	private static final String NARROWER_Clinisoft_QUERY_FILEPATH = "/sparql_queries/SNOMED_Clinisoft_hierarchy_DOWN.sparql";
	private static final String EQUAL_SNOMED_X73_QUERY_FILEPATH = "/sparql_queries/SNOMED_CT_to_IEEEX73_direct.sparql";
	private static final String EQUAL_Clinisoft_SNOMED_QUERY_FILEPATH = "/sparql_queries/Clinisoft_SNOMED_direct.sparql";

	private static final String EQUAL_X73_SNOMED_QUERY_FILEPATH = "/sparql_queries/IEEEX73_to_SNOMED_CT_direct.sparql";

	private static final String SNOMED_Clinisoft_MAP_QUERY_FILEPATH = "/sparql_queries/INSERT_SNOMED_Clinisoft_mapping.sparql";
	private static final String SNOMED_Clinisoft_UNMAP_QUERY_FILEPATH = "/sparql_queries/DELETE_SNOMED_Clinisoft_mapping.sparql";

	private static final String VALUE_NA = "n/a";
	
	private Logger logger = LoggerFactory.getLogger(ConceptMapResourceProvider.class);

	
	@Autowired
	private VirtuosoDBClient virtuosoClient;
	

	@Override
	public Class<? extends IBaseResource> getResourceType() {
	
		return ConceptMap.class;
	}
	
	@Read
	public ConceptMap read(@IdParam IdDt theId){
		return null;
	}
	
	@Operation(name="$translate", idempotent=true)
	public Parameters translateOperation(
	   @OperationParam(name="code") CodeDt code,
	   @OperationParam(name="system") UriDt system,
	   @OperationParam(name="target") UriDt target,
	   @OperationParam(name="equivalence") CodeDt equivalence,
	   @OperationParam(name="label") StringDt search_string) throws Exception
	{
		Parameters params = null;
		
		// Case where SNOMED-CT ID is asked for search string
		if(equivalence.getValue().equals(EQUIVALENT_SEARCH)){
			params = new Parameters();
			Parameter paramResult = params.addParameter();
			paramResult.setName("result");
			
			// Run sparql query to search
			if(target != null && target.getValue().equals(URI_SNOMED)){
				
				String sparqlQuery;
				try {
					sparqlQuery = getQuery(SEARCH_QUERY_FILEPATH);
					sparqlQuery = sparqlQuery.replace("<search_string>", search_string.getValue());
				} catch (Exception e) {
					logger.error(e.getMessage());
					throw new Exception(e);
				}
				
				String xmlResponse = null;
				NodeList listResults = null;
				int elemCount;
				try{
					VirtGraph virtGraph = virtuosoClient.connect();
					xmlResponse = virtuosoClient.sendSparql(virtGraph, sparqlQuery);
					Document doc = ParserUtils.parseXmlString(xmlResponse);
					listResults = doc.getElementsByTagName("result");
					// Set match elements
					elemCount=listResults.getLength();
				}
				catch(Exception e){
					elemCount = 0;
				}
				
				logger.info(xmlResponse);
				
				BooleanDt boolVal = (BooleanDt) new BooleanDt();
				if(elemCount > 0)
					boolVal.setValue(Boolean.TRUE);
				else
					boolVal.setValue(Boolean.FALSE);
				paramResult.setValue(boolVal);
				
				for(int i=0; i<elemCount; i++){
					Element elemResult = (Element) listResults.item(i);
					String id = getBindingValue(elemResult, "codeSNOMEDCT");
					String label = getBindingValue(elemResult, "snomedLabel");
					
					Parameter paramMatch = params.addParameter();
					paramMatch.setName("match");	
					
					Parameter paramEquiv = new Parameter().setName("equivalence");
					paramMatch.addPart(paramEquiv);
					CodeDt codeEquiv = new CodeDt();
					codeEquiv.setValue("specializes");
					paramEquiv.setValue(codeEquiv);
					
					Parameter paramConcept = new Parameter().setName("concept");
					paramMatch.addPart(paramConcept);
					CodingDt codeVal = new CodingDt();
					codeVal.setCode(id);
					codeVal.setDisplay(label);
					paramConcept.setValue(codeVal);
				}
			}
		}
		if(equivalence.getValue().equals(EQUIVALENT_EQUAL) || equivalence.getValue().equals(EQUIVALENT_SUBSUME)){
			params = new Parameters();
			Parameter paramResult = params.addParameter();
			paramResult.setName("result");
			
			String sparqlQuery;
			// Run sparql query for equal match
			if(target != null && target.getValue().equals(URI_IEEEX73) &&
			   system != null && system.getValue().equals(URI_SNOMED)	)
				sparqlQuery = getQuery(EQUAL_SNOMED_X73_QUERY_FILEPATH);
			else if(target != null && target.getValue().equals(URI_SNOMED) &&
				   system != null && system.getValue().equals(URI_IEEEX73)	)
					sparqlQuery = getQuery(EQUAL_X73_SNOMED_QUERY_FILEPATH);		
			else if(target != null && target.getValue().equals(URI_SNOMED) &&
					   system != null && system.getValue().equals(URI_CLINISOFT)	)
						sparqlQuery = getQuery(EQUAL_Clinisoft_SNOMED_QUERY_FILEPATH);		
			else
				throw new Exception("wrong system and/or target parameters");
				
			try {					
				sparqlQuery = sparqlQuery.replace("<CODE>", code.getValue());
			} catch (Exception e) {
				logger.error(e.getMessage());
				throw new Exception(e);
			}
			
			String xmlResponse = null;
			NodeList listResults = null;
			int elemCount;
			try{
				VirtGraph virtGraph = virtuosoClient.connect();
				xmlResponse = virtuosoClient.sendSparql(virtGraph, sparqlQuery);
				Document doc = ParserUtils.parseXmlString(xmlResponse);
				listResults = doc.getElementsByTagName("result");
				// Set match elements
				elemCount=listResults.getLength();
			}
			catch(Exception e){
				elemCount = 0;
			}
			logger.debug(xmlResponse);
			
			BooleanDt boolVal = (BooleanDt) new BooleanDt();
			if(elemCount > 0)
				boolVal.setValue(Boolean.TRUE);
			else
				boolVal.setValue(Boolean.FALSE);
			paramResult.setValue(boolVal);
			
			for(int i=0; i<elemCount; i++){
				Element elemResult = (Element) listResults.item(i);
				String source_this_label = getBindingValue(elemResult, "source_this_label");
				String source_this_id = getBindingValue(elemResult, "source_this_id");
				String target_this_label = getBindingValue(elemResult, "target_this_label");
				String target_this_id = getBindingValue(elemResult, "target_this_id");
				
				Parameter paramMatch = params.addParameter();
				paramMatch.setName("match");	
				
				Parameter paramEquiv = new Parameter().setName("equivalence");
				paramMatch.addPart(paramEquiv);
				CodeDt codeEquiv = new CodeDt();
				codeEquiv.setValue("equal");
				paramEquiv.setValue(codeEquiv);
				
				if(target_this_id != null && !target_this_id.equals("(NULL)")){
					Parameter paramConcept = new Parameter().setName("concept");
					paramMatch.addPart(paramConcept);
					CodingDt codeVal = new CodingDt();
					codeVal.setCode(target_this_id);
					codeVal.setDisplay(target_this_label);	
					paramConcept.setValue(codeVal);
					
					// add source as well, in case it is subsumed/wider target concept that is
					// actually matched
					Parameter paramProduct = new Parameter().setName("product");
					paramMatch.addPart(paramProduct);
					paramConcept = new Parameter().setName("concept");
					paramProduct.addPart(paramConcept);						
					codeVal = new CodingDt();
					codeVal.setCode(source_this_id);
					if(source_this_label != null)
						codeVal.setDisplay(source_this_label);
					else
						codeVal.setDisplay(VALUE_NA);
					paramConcept.setValue(codeVal);
				}
			}
		}
		// Case where subclass hierarchy is asked
		else if(equivalence.getValue().equals(EQUIVALENT_NARROW)){			
			// SPARQL query to DIVE in 		
			// Run sparql query to get hierarchy
			if(target != null && target.getValue().equals(URI_IEEEX73)){
				params = executeHierarchyQuery(NARROWER_X73_QUERY_FILEPATH, code);
			}
			else if(target != null && target.getValue().equals(URI_CLINISOFT)){
				params = executeHierarchyQuery(NARROWER_Clinisoft_QUERY_FILEPATH, code);
			}
		}
		else if (equivalence.getValue().equals(EQUIVALENT_BROADER)){
			// SPARQL query to SURFACE UP 		
			// Run sparql query to get hierarchy
			if(target != null && target.getValue().equals(URI_IEEEX73)){
				params = executeHierarchyQuery(BROADER_X73_QUERY_FILEPATH, code);
			}
			else if(target != null && target.getValue().equals(URI_CLINISOFT)){
				params = executeHierarchyQuery(BROADER_Clinisoft_QUERY_FILEPATH, code);
			}
		}
		
		return params;
	}
	
	@Operation(name="$map", idempotent=true)
	public Parameters mapOperation(
	   @OperationParam(name="sourcecode") CodeDt sourcecode,
	   @OperationParam(name="sourcesystem") UriDt sourcesystem,
	   @OperationParam(name="targetcode") CodeDt targetcode,
	   @OperationParam(name="targetsystem") UriDt targetsystem,
	   @OperationParam(name="direction") CodeDt direction) 
			   throws Exception
	{
		Parameters params = new Parameters();
		Parameter paramResult = params.addParameter();
		paramResult.setName("result");
		
		String sparqlQuery;
		if(direction != null && direction.getValue().equals(DIRECTION_MAP))
			sparqlQuery = getQuery(SNOMED_Clinisoft_MAP_QUERY_FILEPATH);
		else
			sparqlQuery = getQuery(SNOMED_Clinisoft_UNMAP_QUERY_FILEPATH);

		// remove underscore to match the URI of an RDF entity
		sparqlQuery = sparqlQuery.replace("<SCT_ID>", sourcecode.getValue().replace("_", ""));
		sparqlQuery = sparqlQuery.replace("<CS_ID>", targetcode.getValue().replace("_", ""));
		
		String xmlResponse;
		NodeList listResults;
		try{
			VirtGraph virtGraph = virtuosoClient.connect();
			xmlResponse = virtuosoClient.sendSparql(virtGraph, sparqlQuery);
			Document doc = ParserUtils.parseXmlString(xmlResponse);
			listResults = doc.getElementsByTagName("result");
			paramResult.setValue((IDatatype) new BooleanDt().setValue(Boolean.TRUE));
		}
		catch(Exception e){
			paramResult.setValue((IDatatype) new BooleanDt().setValue(Boolean.FALSE));
		}
		
		return params;
	}
	
	private Parameters executeHierarchyQuery(String queryFile, CodeDt code ) throws Exception{
		Parameters params = new Parameters();
		Parameter paramResult = params.addParameter();
		paramResult.setName("result");
		
		String sparqlQuery;
		try {
			sparqlQuery = getQuery(queryFile);
			sparqlQuery = sparqlQuery.replace("<CODE>", code.getValue());
			} 
		catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e);
		}
		
		String xmlResponse = null;
		NodeList listResults = null;
		int elemCount;
		try{
			VirtGraph virtGraph = virtuosoClient.connect();
			xmlResponse = virtuosoClient.sendSparql(virtGraph, sparqlQuery);
			Document doc = ParserUtils.parseXmlString(xmlResponse);
			listResults = doc.getElementsByTagName("result");
			// Set match elements
			elemCount=listResults.getLength();
		}
		catch(Exception e){
			elemCount = 0;
		}
		logger.debug(xmlResponse);
		

		BooleanDt boolVal = (BooleanDt) new BooleanDt();
		if(elemCount > 0)
			boolVal.setValue(Boolean.TRUE);
		else
			boolVal.setValue(Boolean.FALSE);
		paramResult.setValue(boolVal);
		
		for(int i=0; i<elemCount; i++){
			Element elemResult = (Element) listResults.item(i);
			String source_this_label = getBindingValue(elemResult, "source_this_label");
			String source_this_id = getBindingValue(elemResult, "source_this_id");
			String target_this_label = getBindingValue(elemResult, "target_this_label");
			String target_this_id = getBindingValue(elemResult, "target_this_id");
			String map_tree_label = getBindingValue(elemResult, "map_tree_label");
			String map_tree_id = getBindingValue(elemResult, "map_tree_id");
			
			Parameter paramMatch = params.addParameter();
			paramMatch.setName("match");	
			
			Parameter paramEquiv = new Parameter().setName("equivalence");
			paramMatch.addPart(paramEquiv);
			CodeDt codeEquiv = new CodeDt();
			codeEquiv.setValue(EQUIVALENT_BROADER);
			paramEquiv.setValue(codeEquiv);
			
			// 1st are the source codes
			
			// Target system hierarchy goes to Concept
			Parameter paramConcept = new Parameter().setName("concept");
			paramMatch.addPart(paramConcept);
			CodingDt codeVal = new CodingDt();
			if(target_this_id != null && !target_this_id.equals("(NULL)"))
				codeVal.setCode(target_this_id);
			else
				codeVal.setCode(VALUE_NA);
			if(target_this_label != null && !target_this_label.equals("(NULL)"))
				codeVal.setDisplay(target_this_label);
			else
				codeVal.setDisplay(VALUE_NA);
			paramConcept.setValue(codeVal);
			
			// Source system  hierarchy goes to Product.Concept
			Parameter paramProduct = new Parameter().setName("product");
			paramMatch.addPart(paramProduct);
			paramConcept = new Parameter().setName("concept");
			paramProduct.addPart(paramConcept);						
			codeVal = new CodingDt();
			codeVal.setCode(source_this_id);
			if(source_this_label != null)
				codeVal.setDisplay(source_this_label);
			else
				codeVal.setDisplay(VALUE_NA);
			paramConcept.setValue(codeVal);
			
			
			String[] arrayMapTreeLabel = map_tree_label.split("\n");
			String[] arrayMapTreeID = map_tree_id.split("\n");
			
			for(int j=0; j<arrayMapTreeID.length; j++){
				String[] mappingLabel = arrayMapTreeLabel[j].split("\\|");
				String[] mappingID = arrayMapTreeID[j].split("\\|");
				
				// Target system hierarchy goes to Concept
				paramConcept = new Parameter().setName("concept");
				paramMatch.addPart(paramConcept);
				codeVal = new CodingDt();
				if(!mappingID[1].equals("(NULL)"))
					codeVal.setCode(mappingID[1]);
				else
					codeVal.setCode(VALUE_NA);
				if(!mappingLabel[1].equals("(NULL)"))
					codeVal.setDisplay(mappingLabel[1]);
				else
					codeVal.setDisplay(VALUE_NA);
				paramConcept.setValue(codeVal);
				
				// Source system  hierarchy goes to Product.Concept
				paramProduct = new Parameter().setName("product");
				paramMatch.addPart(paramProduct);
				paramConcept = new Parameter().setName("concept");
				paramProduct.addPart(paramConcept);						
				codeVal = new CodingDt();
				codeVal.setCode(mappingID[0]);
				codeVal.setDisplay(mappingLabel[0]);
				paramConcept.setValue(codeVal);
			}
		}
		return params;
	}
	
	private String getBindingValue(Element elemResult, String bindingName) {
		String value = null;
		NodeList listBinding = elemResult.getElementsByTagName("binding");
		for(int i=0; i<listBinding.getLength(); i++ ){
			Element elemBinding = (Element) listBinding.item(i);
			String name = ParserUtils.requireAttribute(elemBinding, "name");
			if( name.equals(bindingName)){
				// Now get the literal value
				NodeList listLiterals = elemBinding.getElementsByTagName("literal");
				if(listLiterals.getLength() == 0)
					break;
				
				value = ParserUtils.requireContent((Element) listLiterals.item(0));
				break;
			}
		}
		return value;
	}

	// Read the sparql query from file saved as a resource
	private String getQuery(String queryFile) throws Exception{
	// read from file
		
	String val = "";
	try
	{
		InputStream is = this.getClass().getResourceAsStream(queryFile);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        
         // reads each line
         String l;
         while((l = r.readLine()) != null) {
            val = val + l + "\n";
         } 
		is.close();
	} catch (Exception e) {
			throw new Exception("Reading sparql file failed:" + queryFile, e);
		}
	return val;  	
	}

}
