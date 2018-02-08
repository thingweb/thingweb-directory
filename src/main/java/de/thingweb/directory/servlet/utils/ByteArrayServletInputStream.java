package de.thingweb.directory.servlet.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
	
public class ByteArrayServletInputStream extends ServletInputStream {

	private final ByteArrayInputStream in;
	
	public ByteArrayServletInputStream(byte[] bytes) {
		 in = new ByteArrayInputStream(bytes);
	}
	
	@Override
	public boolean isReady() {
		return true;
	}
	
	@Override
	public boolean isFinished() {
		return true;
	}
	
	@Override
	public int read() throws IOException {
		return in.read();
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return in.read(b);
	}
	
	@Override
	public void setReadListener(ReadListener arg0) {
		// TODO ?
	}
	
}