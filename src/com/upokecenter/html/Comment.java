package com.upokecenter.html;

import com.upokecenter.html.HtmlParser.NodeType;

class Comment extends Node implements IComment {
	String data;

	@Override
	public String getData(){
		return data;
	}


	void setData(String data){
		this.data=data;
	}

	Comment() {
		super(NodeType.COMMENT_NODE);
	}

	@Override String toDebugString(){
		return "<!-- "+getData().toString()+" -->\n";
	}
}