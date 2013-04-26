package com.upokecenter.html;


 class Attr implements IAttr {
	StringBuilder name;
	StringBuilder value;
	String prefix=null;

	String localName=null;

	String nameString=null;
	String valueString=null;
	String namespace=null;
	public Attr(){
		name=new StringBuilder();
		value=new StringBuilder();
	}

	public Attr(Attr attr){
		nameString=attr.getName();
		valueString=attr.getValue();
		prefix=attr.prefix;
		localName=attr.localName;
		namespace=attr.namespace;
	}
	public Attr(char ch){
		name=new StringBuilder();
		value=new StringBuilder();
		name.append(ch);
	}

	public Attr(int ch){
		name=new StringBuilder();
		value=new StringBuilder();
		name.appendCodePoint(ch);
	}

	public Attr(String name, String value){
		nameString=name;
		valueString=value;
	}
	 void appendToName(int ch){
		if(nameString!=null)
			throw new AssertionError();
		name.appendCodePoint(ch);
	}

	 void appendToValue(int ch){
		if(valueString!=null)
			throw new AssertionError();
		value.appendCodePoint(ch);
	}

	 void commitValue(){
		if(value==null)
			throw new AssertionError();
		valueString=value.toString();
		value=null;
	}

	/* (non-Javadoc)
	 * @see com.upokecenter.html.IAttr#getLocalName()
	 */
	@Override
	public String getLocalName(){
		return (namespace==null) ? getName() : localName;
	}

	/* (non-Javadoc)
	 * @see com.upokecenter.html.IAttr#getName()
	 */
	@Override
	public String getName(){
		return (nameString!=null) ? nameString : name.toString();
	}

	/* (non-Javadoc)
	 * @see com.upokecenter.html.IAttr#getNamespaceURI()
	 */
	@Override
	public String getNamespaceURI(){
		return namespace;
	}

	/* (non-Javadoc)
	 * @see com.upokecenter.html.IAttr#getPrefix()
	 */
	@Override
	public String getPrefix() {
		return prefix;
	}
	/* (non-Javadoc)
	 * @see com.upokecenter.html.IAttr#getValue()
	 */
	@Override
	public String getValue() {
		return (valueString!=null) ? valueString : value.toString();
	}

	 boolean isAttribute(String name, String namespace){
		String thisname=getLocalName();
		boolean match=(name==null ? thisname==null : name.equals(thisname));
		if(!match)return false;
		match=(namespace==null ? this.namespace==null : namespace.equals(this.namespace));
		return match;
	}

	 void setName(String value2) {
		if(value2==null)
			throw new IllegalArgumentException();
		nameString=value2;
		name=null;
	}

	 void setNamespace(String value){
		if(value==null)
			throw new IllegalArgumentException();
		namespace=value;
		nameString=getName();
		int io=nameString.indexOf(':');
		if(io>=1){
			prefix=nameString.substring(0,io);
			localName=nameString.substring(io+1);
		} else {
			prefix="";
			localName=getName();
		}
	}

	/**
	 * NOTE: Set after setNamespace, or it
	 * may be overwritten
	 * @param attrprefix
	 */
	public void setPrefix(String attrprefix) {
		prefix=attrprefix;
	}

	 void setValue(String value2) {
		if(value2==null)
			throw new IllegalArgumentException();
		valueString=value2;
		value=null;
	}
	@Override
	public String toString(){
		return "[Attribute: "+getName()+"="+getValue()+"]";
	}

}