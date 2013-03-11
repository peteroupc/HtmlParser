package com.upokecenter.util;

public interface IBoundAction<T> {
	public void action(Object thisObject, T... parameters);
}
