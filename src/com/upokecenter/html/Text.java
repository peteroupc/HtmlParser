package com.upokecenter.html;

import com.upokecenter.html.HtmlParser.NodeType;
import com.upokecenter.util.IntList;

class Text extends Node implements IText {
	public IntList text=new IntList();
	public Text() {
		super(NodeType.TEXT_NODE);
	}

	@Override
	public String toDebugString(){
		return "\""+text.toString().replace("\n","~~~~")+"\"\n";
	}


	@Override
	public String getData(){
		return text.toString();
	}

}