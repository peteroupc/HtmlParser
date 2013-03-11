package com.upokecenter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IStreamObjectSerializer<T> {
	public T readObjectFromStream(InputStream stream) throws IOException;
	public void writeObjectToStream(T obj, OutputStream file) throws IOException;
}
