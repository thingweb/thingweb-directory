package de.thingweb.directory;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class ThingDescriptionUtils {


  // *********** FOR JENA-TEXT ************

  /**
  * Extracts the key words from the model's statements.
  * This includes the Literals and the localNames of Resources and Predicates.
  * 
  * @param model Model to extract key words
  * @return List of key word strings
  */
  public List<String> getModelKeyWords(Model model) {
	
	List<String> keyWords = new ArrayList<String>();
	StmtIterator statementIter = model.listStatements();
	Statement s;
	Property predicate;
	RDFNode object;
	
	while (statementIter.hasNext()) {
	  s = statementIter.nextStatement();
	  predicate = s.getPredicate();
	  object = s.getObject();
		
	  keyWords.add(predicate.getLocalName());

	  // local name of (non blank nodes) Resources
	  if (object instanceof Resource && object.toString().contains("/")) {
		keyWords.add(object.asResource().getLocalName());
		  
	  } else if (object instanceof Literal) {
		// object is a Literal
		keyWords.add(object.toString());
	  }
  }
  return keyWords;
 }
  

 /**
   * Does a full text searc using jena-text.
   * Returns the uris of the TDs that matched with the given key words.
   * @param keyWords Words to find in a TD content.
   * @return List of uris
   */
  public static List<String> listThingDescriptionsFromTextSearch(String keyWords) {
	
	List<String> tds = new ArrayList<>();

//	// Construct query
//	String qMatch = "";
//	String predicate, property;
//	predicate = " text:query ";
//	property = "rdfs:comment";
//	qMatch += " ?g " + predicate + "(" + property + " " + keyWords + ") . ";
//	
//	String prefix = StrUtils.strjoinNL
//			  ( "PREFIX text: <http://jena.apache.org/text#>"
//			  , "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
//			  , "PREFIX td: <http://www.w3c.org/wot/td#>"
//			  , "PREFIX qu: <http://purl.oclc.org/NET/ssnx/qu/qu#>"
//			  , "PREFIX unit: <http://purl.oclc.org/NET/ssnx/qu/unit#>"
//			  , "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>");
//	
//	// Run the query
//	Dataset dataset = ThingDirectory.get().dataset;
//	dataset.begin(ReadWrite.READ);
//	
//	try {
//	  String query = "SELECT DISTINCT ?g WHERE { " + qMatch + " GRAPH ?g { FILTER NOT EXISTS { ?ontology a <http://www.w3.org/2002/07/owl#Ontology> } } }";
//	  Query q = QueryFactory.create(prefix + "\n" + query);
//	  
//	  try {
//		QueryExecution qexec = QueryExecutionFactory.create(q , dataset);
//		ResultSet result = qexec.execSelect();
//		while (result.hasNext()) {
//		  tds.add(result.next().get("g").asResource().getURI());
//		}
//	  } catch (Exception e) {
//		throw e;
//	  }
//	} finally {
//	  dataset.end();
//	}
	
	return tds;
  }


  // *********** FOR RESOURCE DIRECTORY ************

  /**
   * Returns a list of registered end points
   * @return
   */
  public static List<String> listEndpoints() {
	
	List<String> eps = new ArrayList<>();
	
//	Dataset dataset = ThingDirectory.get().dataset;
//	dataset.begin(ReadWrite.READ);
//	
//	String prefix = "PREFIX rdf-schema: <http://www.w3.org/2000/01/rdf-schema#>";
//
//	try {
//	  String q = prefix + " SELECT ?endpoint WHERE { ?s rdf-schema:isDefinedBy ?endpoint . }";
//	  try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
//		ResultSet result = qexec.execSelect();
//		while (result.hasNext()) { 
//		eps.add(result.next().get("endpoint").toString());
//		}
//	  }
//	catch (Exception e) {
//	  throw e;
//	}
//	} finally {
//	  dataset.end();
//	}
	
	return eps;
  }

}