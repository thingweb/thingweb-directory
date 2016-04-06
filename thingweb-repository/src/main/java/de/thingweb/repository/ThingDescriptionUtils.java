package de.thingweb.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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
  
  public static String streamToString(InputStream s) throws IOException {
    StringWriter w = new StringWriter();
    InputStreamReader r = new InputStreamReader(s, "UTF-8");
    char[] buf = new char [512];
    int len;
    
    while ((len = r.read(buf)) > 0) {
      w.write(buf, 0, len);
    }
    s.close();
    
    return w.toString();
  }

}
