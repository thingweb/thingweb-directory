package org.eclipse.thingweb.directory

/**
 * 
 * Exception thrown whenever an attempt is made to register
 * a resource with malformed content (e.g. CoRE Link format
 * syntax error).
 *
 * @author Victor Charpenay
 * @creation 28.09.2018
 *
 */
class ResourceMalformedException extends Exception {

	def ResourceMalformedException() { super() }
	
	def ResourceMalformedException(Throwable cause) { super(cause) }
	
	def ResourceMalformedException(String message) { super(message) }
	
	def ResourceMalformedException(String message, Throwable cause) { super(message, cause) } 
	
}
