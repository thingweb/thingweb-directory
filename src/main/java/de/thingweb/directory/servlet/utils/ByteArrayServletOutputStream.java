package de.thingweb.directory.servlet.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
	
public class ByteArrayServletOutputStream extends ServletOutputStream {

	ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	public byte[] getBytes() {
		return out.toByteArray();
	}
	
	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener arg0) {
		// TODO ?
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}
	
	public void reset() {
		out.reset();
	}
	
	public int size() {
		return out.size();
	}
	
	@Override
	public void flush() throws IOException {
		out.flush();
	}
	
}