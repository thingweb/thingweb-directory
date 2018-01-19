package com.ontotext.graphdb.example.util;


import com.ontotext.trree.config.OWLIMSailSchema;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositorySchema;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * A useful class for creating a local (embedded) GraphDB database (no networking needed).
 */
public class EmbeddedGraphDB implements Closeable {
    private LocalRepositoryManager repositoryManager;

    /**
     * Creates a new embedded instance of GraphDB in the provided directory.
     *
     * @param baseDir a directory where to store repositories
     * @throws RepositoryException
     */
    public EmbeddedGraphDB(String baseDir) throws RepositoryException {
        repositoryManager = new LocalRepositoryManager(new File(baseDir));
        repositoryManager.initialize();
    }

    /**
     * Creates a repository with the given ID.
     *
     * @param repositoryId a new repository ID
     * @throws RDFHandlerException
     * @throws RepositoryConfigException
     * @throws RDFParseException
     * @throws IOException
     * @throws GraphUtilException
     * @throws RepositoryException
     */
    public void createRepository(String repositoryId) throws RDFHandlerException, RepositoryConfigException, RDFParseException, IOException, RepositoryException {
        createRepository(repositoryId, null, null);
    }

    /**
     * Creates a repository with the given ID, label and optional override parameters.
     *
     * @param repositoryId    a new repository ID
     * @param repositoryLabel a repository label, or null if none should be set
     * @param overrides       a map of repository creation parameters that override the defaults, or null if none should be overridden
     * @throws RDFParseException
     * @throws IOException
     * @throws RDFHandlerException
     * @throws GraphUtilException
     * @throws RepositoryConfigException
     * @throws RepositoryException
     */
    public void createRepository(String repositoryId, String repositoryLabel, Map<String, String> overrides)
            throws RDFParseException, IOException, RDFHandlerException,
            RepositoryConfigException, RepositoryException {
        if (repositoryManager.hasRepositoryConfig(repositoryId)) {
            throw new RuntimeException("Repository " + repositoryId + " already exists.");
        }

        TreeModel graph = new TreeModel();

        InputStream config = EmbeddedGraphDB.class.getResourceAsStream("/repo-defaults.ttl");
        RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
        rdfParser.setRDFHandler(new StatementCollector(graph));
        rdfParser.parse(config, RepositoryConfigSchema.NAMESPACE);
        config.close();
        
        Resource repositoryNode = Models.subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElse(null);

        graph.add(repositoryNode, RepositoryConfigSchema.REPOSITORYID, 
        		SimpleValueFactory.getInstance().createLiteral(repositoryId));

        if (repositoryLabel != null) {
            graph.add(repositoryNode, RDFS.LABEL, 
            		SimpleValueFactory.getInstance().createLiteral(repositoryLabel));
        }

        if (overrides != null) {
            Resource configNode = (Resource)Models.object(graph.filter(null, SailRepositorySchema.SAILIMPL, null)).orElse(null);
            for (Map.Entry<String, String> e : overrides.entrySet()) {
                IRI key = SimpleValueFactory.getInstance().createIRI(OWLIMSailSchema.NAMESPACE + e.getKey());
                Literal value = SimpleValueFactory.getInstance().createLiteral(e.getValue());
                graph.remove(configNode, key, null);
                graph.add(configNode, key, value);
            }
        }

        RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);

        repositoryManager.addRepositoryConfig(repositoryConfig);
    }

    public Repository getRepository(String repositoryId) throws RepositoryException, RepositoryConfigException {
        return repositoryManager.getRepository(repositoryId);
    }

    @Override
    public void close() throws IOException {
        repositoryManager.shutDown();
    }

    /**
     * A convenience method to create a temporary repository and open a connection to it.
     * When the connection is closed all underlying objects (EmbeddedGraphDB and LocalRepositoryManager)
     * will be closed as well. The temporary repository is created in a unique temporary directory
     * that will be deleted when the program terminates.
     *
     * @param ruleset ruleset to use for the repository, e.g. owl-horst-optimized
     * @return a RepositoryConnection to a new temporary repository
     * @throws IOException
     * @throws RepositoryException
     * @throws RDFParseException
     * @throws GraphUtilException
     * @throws RepositoryConfigException
     * @throws RDFHandlerException
     */
    public static RepositoryConnection openConnectionToTemporaryRepository(String ruleset) throws IOException, RepositoryException,
            RDFParseException, RepositoryConfigException, RDFHandlerException {
        // Temporary directory where repository data will be stored.
        // The directory will be deleted when the program terminates.
        File baseDir = FileUtil.createTempDir("graphdb-examples");
        baseDir.deleteOnExit();

        // Create an instance of EmbeddedGraphDB and a single repository in it.
        final EmbeddedGraphDB embeddedGraphDB = new EmbeddedGraphDB(baseDir.getAbsolutePath());
        embeddedGraphDB.createRepository("tmp-repo", null, Collections.singletonMap("ruleset", ruleset));

        // Get the newly created repository and open a connection to it.
        Repository repository = embeddedGraphDB.getRepository("tmp-repo");
        RepositoryConnection connection = repository.getConnection();

        // Wrap the connection in order to close the instance of EmbeddedGraphDB on connection close
        return new RepositoryConnectionWrapper(repository, connection) {
            @Override
            public void close() throws RepositoryException {
                super.close();
                try {
                    embeddedGraphDB.close();
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
            }
        };
    }
}
