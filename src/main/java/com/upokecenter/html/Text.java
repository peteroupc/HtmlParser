package com.upokecenter.html;

/*

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

  class Text extends Node implements IText {
    public Text() {
 super(NodeType.TEXT_NODE);
      this.propVarvaluetext = new StringBuilder();
    }

    public final StringBuilder getValueText() { return propVarvaluetext; }
private final StringBuilder propVarvaluetext;

    public String GetData() {
      return this.getValueText().toString();
    }

    public String GetName() {
      return "#valueText";
    }

    @Override public String GetTextContent() {
      return this.getValueText().toString();
    }

    @Override String ToDebugString() {
      return "\"" + this.getValueText().toString().replace("\n", "~~~~") + "\"\n";
    }
  }
