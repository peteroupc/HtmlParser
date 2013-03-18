package com.upokecenter.html;

import com.upokecenter.util.IntList;

class Text extends Node implements IText {
	public IntList text=new IntList();
	public Text() {
		super(NodeType.TEXT_NODE);
	}

	@Override
	 String toDebugString(){
		return "\""+text.toString().replace("\n","~~~~")+"\"\n";
	}

	@Override
	public  String getTextContent(){
		return text.toString();
	}

	@Override
	public String getData(){
		return text.toString();
	}

}