package com.upokecenter.html;


class Comment extends Node implements IComment {
	String data;

	@Override
	public String getData(){
		return data;
	}


	@Override
	void setData(String data){
		this.data=data;
	}

	Comment() {
		super(NodeType.COMMENT_NODE);
	}

	@Override String toDebugString(){
		return "<!-- "+getData().toString()+" -->\n";
	}


	@Override
	public String getTextContent(){
		return null;
	}

}