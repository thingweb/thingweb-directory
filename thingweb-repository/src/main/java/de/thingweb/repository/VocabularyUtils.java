package de.thingweb.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.QueryExecUtils;

public class VocabularyUtils {

  public static Set<String> listVocabularies() {
	Set<String> tds = new HashSet<>();
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);

	try {
	  String q = "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?o a <http://www.w3.org/2002/07/owl#Ontology> } }";
	  QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
	  ResultSet result = qexec.execSelect();
	  while (result.hasNext()) {
	   tds.add(result.next().get("g").asResource().getURI());
	  }
	} catch (Exception e) {
	  throw e;
	} finally {
	  dataset.end();
	}

	return tds;
  }
  
  public static Model mergeVocabularies() {
    // TODO add argument to scope the operation
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);
	
	try {
	  String q = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?ontology a <http://www.w3.org/2002/07/owl#Ontology> . ?s ?p ?o } }";
	  QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
	  return qexec.execConstruct();
	} catch (Exception e) {
	  throw e;
	} finally {
	  dataset.end();
	}
  }

}