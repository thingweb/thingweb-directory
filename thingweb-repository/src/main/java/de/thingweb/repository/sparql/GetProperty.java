package de.thingweb.repository.sparql;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;

public class GetProperty extends FunctionBase1 {

	@Override
	public NodeValue exec(NodeValue v) {
		// TODO call WoT API
		return NodeValue.makeString("FALSE");
	}

}
