package org.eclipse.thingweb.directory.servlet;

import org.eclipse.thingweb.directory.ResourceManager;
import org.eclipse.thingweb.directory.ResourceManagerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

    public class TDLookUpSparqlServlet extends HttpServlet {

        private static final long serialVersionUID = 5679530570591631536L;

        public final static String DEFAULT_MEDIA_TYPE = "application/td+json";

        public static final String DEFAULT_QUERY = "SELECT DISTINCT ?s ?p ?o WHERE { GRAPH ?id { ?s ?p ?o }}";

        private static final String LOOKUP_SPARQL_TYPE = "sparql";

        private static final String QUERY_PARAMETER = "query";

        private final ResourceManager manager = ResourceManagerFactory.get("td");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String contentType = req.getHeader("Accept");
            if (contentType == null) contentType = DEFAULT_MEDIA_TYPE;

            Map<String, String> parameters = new HashMap<>();
            // TODO

            String query = req.getParameter(QUERY_PARAMETER);
            if (query == null) query = DEFAULT_QUERY;

            resp.setContentType(contentType);
            manager.lookUp(LOOKUP_SPARQL_TYPE, resp.getOutputStream(), contentType, query, parameters);
        }

    }
