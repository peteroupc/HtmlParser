package com.upokecenter.util;

import java.io.IOException;

public interface IMarkableCharacterInput extends ICharacterInput {

	public int getMarkPosition();

	public void setMarkPosition(int pos) throws IOException;

	public int markIfNeeded();

	public void markToEnd();

	public void moveBack(int count) throws IOException;

}