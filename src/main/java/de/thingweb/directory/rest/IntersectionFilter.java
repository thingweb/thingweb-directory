package de.thingweb.directory.rest;

import java.util.Iterator;
import java.util.Set;

/**
 * 
 * Composite filter that performs a AND on a set of filters
 *
 * @author Victor Charpenay
 * @creation 11.12.2017
 *
 */
public class IntersectionFilter implements CollectionFilter {
	
	private Set<CollectionFilter> filters;
	
	public IntersectionFilter(Set<CollectionFilter> filters) {
		this.filters = filters;
	}

	@Override
	public boolean keep(RESTResource child) {
		boolean keep = true;
		
		Iterator<CollectionFilter> it = filters.iterator();
		while (it.hasNext() && keep) {
			keep = keep && it.next().keep(child);
		}
		
		return keep;
	}

}
