package de.thingweb.directory.resources;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.CollectionFilter;
import de.thingweb.directory.rest.CollectionFilterFactory;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.IntersectionFilter;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTResourceFactory;

public class DirectoryCollectionResource extends CollectionResource {
	
	/**
	 * 
	 * Filters out expired resources from the children list
	 * (but does not delete them permanently)
	 * 
	 * TODO permanently delete child resources?
	 *
	 * @author Victor Charpenay
	 * @creation 11.12.2017
	 *
	 */
	protected static class LifetimeFilter implements CollectionFilter {
		
		public LifetimeFilter() {
			// public constructor
		}
		
		@Override
		public boolean keep(RESTResource child) {
			return child instanceof DirectoryResource && !((DirectoryResource) child).hasExpired();
		}
		
	}

	public DirectoryCollectionResource(String path, RESTResourceFactory f) {
		super(path, f, new CollectionFilterFactory() {
			
			@Override
			public CollectionFilter create(Map<String, String> parameters) throws BadRequestException {
				return new LifetimeFilter();
			}
		});
	}
	
	public DirectoryCollectionResource(String path, RESTResourceFactory f, CollectionFilterFactory ff) {
		super(path, f, new CollectionFilterFactory() {
			
			@Override
			public CollectionFilter create(Map<String, String> parameters) throws BadRequestException {
				Set<CollectionFilter> filters = new HashSet<>();
				
				filters.add(new LifetimeFilter());
				filters.add(ff.create(parameters));
				
				return new IntersectionFilter(filters);
			}
		});
	}
	
}
