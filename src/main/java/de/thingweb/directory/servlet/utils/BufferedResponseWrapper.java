package de.thingweb.directory.servlet.utils;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 
 * Wrapper that keeps the response in a byte buffer for later use,
 * instead of directly sending it to the client.
 *
 * @author Victor Charpenay
 * @creation 07.02.2018
 *
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {
	
	ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();

	public BufferedResponseWrapper(HttpServletResponse response) {
		super(response);
	}
	
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return out;
	}
	
	@Override
	public int getBufferSize() {
		return out.size();
	}
	
	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(out);
	}
	
	public byte[] getOutputBytes() {
		return out.getBytes();
	}
	
	@Override
	public void sendError(int sc) throws IOException {
		setStatus(sc);
		// do nothing. Response should not be committed
	}
	
	@Override
	public void sendError(int sc, String msg) throws IOException {
		sendError(sc);
	}
	
	@Override
	public void sendRedirect(String location) throws IOException {
		setStatus(SC_TEMPORARY_REDIRECT);
		// do nothing. Response should not be committed
	}

}
