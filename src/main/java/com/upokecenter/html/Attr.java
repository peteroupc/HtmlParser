package com.upokecenter.html;

  class Attr implements IAttr {
    private StringBuilder valueName;
    private StringBuilder value;
    private String valuePrefix = null;

    private String valueLocalName = null;

    private String valueNameString = null;
    private String valueString = null;
    private String value_namespace = null;

    public Attr() {
      this.valueName = new StringBuilder();
      this.value = new StringBuilder();
    }

    public Attr(IAttr attr) {
      this.valueNameString = attr.GetName();
      this.valueString = attr.GetValue();
      this.valuePrefix = attr.GetPrefix();
      this.valueLocalName = attr.GetLocalName();
      this.value_namespace = attr.GetNamespaceURI();
    }

    public Attr(char ch) {
      this.valueName = new StringBuilder();
      this.value = new StringBuilder();
      this.valueName.append(ch);
    }

    public Attr(int ch) {
      this.valueName = new StringBuilder();
      this.value = new StringBuilder();
      if (ch <= 0xffff) {
        { this.valueName.append((char)ch);
        }
      } else if (ch <= 0x10ffff) {
        this.valueName.append((char)((((ch - 0x10000) >> 10) & 0x3ff) |
          0xd800));
        this.valueName.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
      }
    }

    public Attr(String valueName, String value) {
      this.valueNameString = valueName;
      this.valueString = value;
    }

    void AppendToName(int ch) {
      if (this.valueNameString != null) {
        throw new IllegalStateException();
      }
      if (ch <= 0xffff) {
        { this.valueName.append((char)ch);
        }
      } else if (ch <= 0x10ffff) {
        this.valueName.append((char)((((ch - 0x10000) >> 10) & 0x3ff) |
          0xd800));
        this.valueName.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
      }
    }

    void AppendToValue(int ch) {
      if (this.valueString != null) {
        throw new IllegalStateException();
      }
      if (ch <= 0xffff) {
        { this.value.append((char)ch);
        }
      } else if (ch <= 0x10ffff) {
        this.value.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
        this.value.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
      }
    }

    void CommitValue() {
      if (this.value == null) {
        throw new IllegalStateException();
      }
      this.valueString = this.value.toString();
      this.value = null;
    }

    /* (non-Javadoc)
     * @see Com.Upokecenter.Html.IAttr#GetLocalName()
     */
    public String GetLocalName() {
      return (this.value_namespace == null) ? this.GetName() :
        this.valueLocalName;
    }

    /* (non-Javadoc)
     * @see Com.Upokecenter.Html.IAttr#GetName()
     */
    public String GetName() {
      return (this.valueNameString != null) ? this.valueNameString :
        this.valueName.toString();
    }

    /* (non-Javadoc)
     * @see Com.Upokecenter.Html.IAttr#GetNamespaceURI()
     */
    public String GetNamespaceURI() {
      return this.value_namespace;
    }

    /* (non-Javadoc)
     * @see Com.Upokecenter.Html.IAttr#GetPrefix()
     */
    public String GetPrefix() {
      return this.valuePrefix;
    }
    /* (non-Javadoc)
     * @see Com.Upokecenter.Html.IAttr#GetValue()
     */
    public String GetValue() {
      return (this.valueString != null) ? this.valueString :
        this.value.toString();
    }

    boolean IsAttribute(String attrName, String value_namespace) {
      String thisname = this.GetLocalName();
      boolean match = attrName == null ? thisname == null : attrName.equals(
          thisname);
      if (!match) {
        return false;
      }
      match = value_namespace == null ? this.value_namespace == null :
        value_namespace.equals(this.value_namespace);
      return match;
    }

    void SetName(String value2) {
      if (value2 == null) {
        throw new IllegalArgumentException();
      }
      this.valueNameString = value2;
      this.valueName = null;
    }

    void SetNamespace(String value) {
      if (value == null) {
        throw new IllegalArgumentException();
      }
      this.value_namespace = value;
      this.valueNameString = this.GetName();
      int io = this.valueNameString.indexOf(':');
      if (io >= 1) {
        this.valuePrefix = this.valueNameString.substring(0, io - 0);
        this.valueLocalName = this.valueNameString.substring(io + 1);
      } else {
        this.valuePrefix = "";
        this.valueLocalName = this.GetName();
      }
    }

    /**
     * NOTE: Set after SetNamespace, or it may be overwritten @param attrprefix.
     * @param attrprefix The parameter {@code attrprefix} is a text string.
     */
    public void SetPrefix(String attrprefix) {
      this.valuePrefix = attrprefix;
    }

    void SetValue(String value2) {
      if (value2 == null) {
        throw new IllegalArgumentException();
      }
      this.valueString = value2;
      this.value = null;
    }

    @Override public String toString() {
      return "[Attribute: " + this.GetName() + "=" + this.GetValue() + "]";
    }
  }
