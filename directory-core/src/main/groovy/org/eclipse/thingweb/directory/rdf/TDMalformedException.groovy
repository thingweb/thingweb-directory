package org.eclipse.thingweb.directory.rdf

import org.eclipse.thingweb.directory.ResourceMalformedException

/**
 * 
 * Exception thrown whenever an attempt is made to register
 * a Thing Description that does not comply to the W3C Thing
 * Description recommendation.
 *
 * @see
 *   <a href="http://www.w3.org/TR/wot-thing-description">
 *     W3C Thing Description Model
 *   </a>
 *
 * @author Victor Charpenay
 * @creation 28.09.2018
 *
 */
class TDMalformedException extends ResourceMalformedException {

	def TDMalformedException() { super() }
	
	def TDMalformedException(Throwable cause) { super(cause) }
	
	def TDMalformedException(String message) { super(message) }
	
	def TDMalformedException(String message, Throwable cause) { super(message, cause) } 

}
