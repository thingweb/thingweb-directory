package de.thingweb.directory.sparql.client;

import java.time.LocalDateTime;

import javax.swing.text.html.HTMLDocument.BlockElement;

import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_GreaterThan;
import org.apache.jena.sparql.expr.E_Now;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateDrop;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

import de.thingweb.directory.vocabulary.TD;

public class Queries {

	public static final String UNION_GRAPH_URI = "/all";

	/**
	 * ASK WHERE {
	 *   ?res dct:valid ?date
	 *   FILTER (now() > ?date)
	 * }
	 * 
	 * @param res
	 * @return
	 */
	public static Query hasExpired(Resource res) {
		Query q = QueryFactory.create();
		q.setQueryAskType();
		
		ElementGroup element = new ElementGroup();
		q.setQueryPattern(element);

		// ?res dct:valid ?date
		Node date = Var.alloc("date");
		Triple valid = new Triple(res.asNode(), DCTerms.valid.asNode(), date);
		element.addTriplePattern(valid);
		
		// FILTER (now() > ?date)
		Node now = getDateTime(0).asNode(); // FIXME now() always returns false?
		Expr ex = new E_GreaterThan(ExprUtils.nodeToExpr(now), ExprUtils.nodeToExpr(date));
		ElementFilter filter = new ElementFilter(ex);
		element.addElement(filter);
		
		return q;
	}
	
	/**
	 * INSERT DATA {
	 *   ?res dct:created now() .
	 *   ?res dct:modified now() .
	 *   ?res dct:valid now() + ?lifetime .
	 * }
	 * 
	 * @param res
	 * @param lifetime
	 * @return
	 */
	public static Update createTimeout(Resource res, int lifetime) {
		Literal now = getDateTime(0);
		Literal timeout = getDateTime(lifetime);

		Triple created = new Triple(res.asNode(), DCTerms.created.asNode(), now.asNode());
		Triple modified = new Triple(res.asNode(), DCTerms.modified.asNode(), now.asNode());
		Triple valid = new Triple(res.asNode(), DCTerms.valid.asNode(), timeout.asNode());

		// INSERT DATA { ... }
		QuadDataAcc qd = new QuadDataAcc();
		qd.addTriple(created);
		qd.addTriple(modified);
		qd.addTriple(valid);
		
		return new UpdateDataInsert(qd);
	}
	
	/**
	 * DELETE {
	 *   ?res dct:modified ?modified ;
	 *        dct:valid ?valid .
	 * } INSERT {
	 *   ?res dct:modified now() ;
	 *        dct:valid now() + ?lifetime .
	 * } WHERE {
	 *   ?res dct:modified ?modified ;
	 *        dct:valid ?valid .
	 * }
	 * 
	 * @param res
	 * @param lifetime
	 * @return
	 */
	public static Update updateTimeout(Resource res, int lifetime) {
		Literal now = getDateTime(0);
		Literal timeout = getDateTime(lifetime);
		
		UpdateDeleteInsert up = new UpdateDeleteInsert();

		Triple modified = new Triple(res.asNode(), DCTerms.modified.asNode(), Var.alloc("modified"));
		Triple valid = new Triple(res.asNode(), DCTerms.valid.asNode(), Var.alloc("valid"));
		Triple modifiedNow = new Triple(res.asNode(), DCTerms.modified.asNode(), now.asNode());
		Triple validUntilTimeout = new Triple(res.asNode(), DCTerms.valid.asNode(), timeout.asNode());
		
		// DELETE { ... }
		up.getDeleteAcc().addTriple(modified);
		up.getDeleteAcc().addTriple(valid);

		// INSERT { ... }
		up.getInsertAcc().addTriple(modifiedNow);
		up.getInsertAcc().addTriple(validUntilTimeout);

		// WHERE { ... }
		ElementGroup element = new ElementGroup();
		element.addTriplePattern(modified);
		element.addTriplePattern(valid);
		up.setElement(element);
		
		return up;
	}
	
	/**
	 * INSERT DATA {
	 *   GRAPH ?id {
	 *     ...
	 *   }
	 *   GRAPH ?union {
	 *     ...
	 *   }
	 * }
	 * 
	 * @param id
	 * @param m
	 * @return
	 */
	public static Update loadGraph(Resource id, Model m) {
		QuadDataAcc qd = new QuadDataAcc();
		UpdateDataInsert up = new UpdateDataInsert(qd);
		
		Node union = NodeFactory.createURI(UNION_GRAPH_URI);
		ExtendedIterator<Triple> it = m.getGraph().find();
		while (it.hasNext()) {
			Triple t = it.next();

			// GRAPH ?id
			qd.addQuad(new Quad(id.asNode(), t));
			// GRAPH ?union
			qd.addQuad(new Quad(union, t));
		}

		return up;
	}
	
	public static Update replaceGraph(Resource id, Model m) {
		return null; // TODO
	}
	
	/**
	 * DELETE {
	 *   GRAPH ?id {
	 *     ?s ?p ?o
	 *   }
	 *   GRAPH ?union {
	 *      ?s ?p ?o
	 *   }
	 * } WHERE {
	 *   GRAPH ?id {
	 *      ?s ?p ?o
	 *   }
	 * };
	 * DROP ?id
	 * 
	 * @param g
	 * @return
	 */
	public static UpdateRequest deleteGraph(Resource id) {
		// DELETE { ... }
		UpdateDeleteInsert delete = new UpdateDeleteInsert();
		delete.setHasInsertClause(false);

		Triple t = new Triple(Var.alloc("s"), Var.alloc("p"), Var.alloc("o"));
		Node union = NodeFactory.createURI(UNION_GRAPH_URI);
		
		// GRAPH ?id
		delete.getDeleteAcc().addQuad(new Quad(id.asNode(), t));
		
		// GRAPH ?union
		delete.getDeleteAcc().addQuad(new Quad(union, t));
		
		// WHERE GRAPH ?id
		ElementGroup element = new ElementGroup();
		element.addTriplePattern(t);
		Element inGraph = new ElementNamedGraph(id.asNode(), element);
		delete.setElement(inGraph);
		
		// DROP ?id
		UpdateDrop drop = new UpdateDrop(id.asNode());
		
		return new UpdateRequest().add(delete).add(drop);
	}
	
	/**
	 * ASK WHERE {
	 *   GRAPH ?id {
	 *     ?s ?p ?o
	 *   }
	 * }
	 * @param id
	 * @return
	 */
	public static Query exists(Resource id) {
		Query q = QueryFactory.create();
		q.setQueryAskType();
		
		Triple t = new Triple(Var.alloc("s"), Var.alloc("p"), Var.alloc("o"));

		// GRAPH ?id { ?s ?o ?p }
		ElementGroup element = new ElementGroup();
		element.addTriplePattern(t);
		Element inGraph = new ElementNamedGraph(id.asNode(), element);
		q.setQueryPattern(inGraph);
		
		return q;
	}
	
	/**
	 * CONSTRUCT {
	 *   ?s ?p ?o
	 * } WHERE {
	 *   GRAPH ?id {
	 *     ?s ?p ?o
	 *   }
	 * }
	 * 
	 * @param id
	 * @return
	 */
	public static Query getGraph(Resource id) {
		Query q = QueryFactory.create();
		q.setQueryConstructType();
		
		Triple t = new Triple(Var.alloc("s"), Var.alloc("p"), Var.alloc("o"));
		
		// CONSTRUCT { ?s ?p ?o }
		QuadAcc qa = new QuadAcc();
		qa.addTriple(t);
		q.setConstructTemplate(new Template(qa));

		// GRAPH ?id { ?s ?o ?p }
		ElementGroup element = new ElementGroup();
		element.addTriplePattern(t);
		Element inGraph = new ElementNamedGraph(id.asNode(), element);
		q.setQueryPattern(inGraph);
		
		return q;
	}

	/**
	 * SELECT ?id WHERE {
	 *   GRAPH ?union {
	 *     ...pattern...
	 *   }
	 *   GRAPH ?id {
	 *     ?thing a td:Thing
	 *   }
	 * }
	 * 
	 * @param pattern should include at least the variable ?thing
	 * @return
	 */
	public static Query filterTDs(Element pattern) {
		Query q = QueryFactory.create();
		q.setQuerySelectType();
		
		Node union = NodeFactory.createURI(UNION_GRAPH_URI);
		Node id = Var.alloc("id");
		Node thing = Var.alloc("thing");
		Triple t = new Triple(thing, RDF.type.asNode(), TD.Thing.asNode());

		// GRAPH ?union
		Element inUnionGraph = new ElementNamedGraph(union, pattern);

		// GRAPH ?id
		ElementGroup isThing = new ElementGroup();
		isThing.addTriplePattern(t);
		Element inIdGraph = new ElementNamedGraph(id, isThing);
		
		ElementGroup all = new ElementGroup();
		all.addElement(inUnionGraph);
		all.addElement(inIdGraph);
		q.setQueryPattern(all);
		
		q.getResultVars().add("id"); // TODO better integration?
		
		return q;
	}

	private static Literal getDateTime(int lifetime) {
		LocalDateTime d = LocalDateTime.now();
		d = d.plusSeconds(lifetime);
		
		// ISO 8601 string format
		return ResourceFactory.createTypedLiteral(d.toString(), XSDDateTimeType.XSDdateTime);
	}
	
}
