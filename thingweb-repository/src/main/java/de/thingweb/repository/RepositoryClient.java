/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Siemens AG and the thingweb community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Created by Sebastian Kaebisch on 10.12.2015.
 */

// UNIRES, Jetty

package de.thingweb.repository;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;

/** Class to interact with a TD repository */
public class RepositoryClient {

	private String repository_uri;
	private int repository_port;

	/** Constructer set up the endpoint address of the TD repository */
	public  RepositoryClient(String  repository_uri, int repository_port) {
	 
		this.repository_uri = repository_uri;
		this.repository_port =repository_port;
	}
	
	/**  This method takes a properties which you are looking for or a SPARQL query  
	 * @param search properties or a SPARQL query
	 * @return JSON array of relevant TD files (=empty array means no match)
	 * */
	public JSONArray tdSearch(String search) throws Exception  {
		
		URL myURL = new URL("http://"+repository_uri+":"+repository_port+"/td?query="+search);
		HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection();
		myURLConnection.setRequestMethod("GET");
		myURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		myURLConnection.setDoInput(true);
		myURLConnection.setDoOutput(true);

		InputStream in = myURLConnection.getInputStream();
								
		JSONArray jsonLDs = new JSONArray(streamToString(in));

		System.out.println(streamToString(in));
		
		return jsonLDs;
	}
	
	/** Brings input stream into string representation  */
	private  String streamToString(InputStream in) throws IOException {
		  StringBuilder out = new StringBuilder();
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  for(String line = br.readLine(); line != null; line = br.readLine()) 
		    out.append(line);
		  br.close();
		  return out.toString();
		}
}
