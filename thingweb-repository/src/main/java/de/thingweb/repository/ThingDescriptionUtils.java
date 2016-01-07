package de.thingweb.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;

public class ThingDescriptionUtils
{

  public static List<String> listThingDescriptions(String query) {
    List<String> tds = new ArrayList<>();
    
    Dataset dataset = Repository.get().dataset;
    dataset.begin(ReadWrite.READ);

    try {
      String q = "SELECT DISTINCT ?g WHERE { GRAPH ?g { " + query + " }}";
      try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
        ResultSet result = qexec.execSelect();
        while (result.hasNext()) {
          tds.add(result.next().get("g").asResource().getURI());
        }
      }
    } finally {
      dataset.end();
    }

    return tds;
  }

}
