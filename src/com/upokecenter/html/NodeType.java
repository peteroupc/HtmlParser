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

package com.upokecenter.html;

/**
 * 
 * Contains constants for node types.
 * 
 * @author Peter
 *
 */
public final class NodeType {
	private NodeType(){}
	/**
	 * A document node.
	 */
	public static final int DOCUMENT_NODE=9;
	/**
	 * A comment node.
	 */
	public static final int COMMENT_NODE=8;
	/**
	 * An HTML element node.
	 */
	public static final int ELEMENT_NODE=1;
	/**
	 * A node containing text.
	 */
	public static final int TEXT_NODE=3;
	/**
	 * A DOCTYPE node.
	 */
	public static final int DOCUMENT_TYPE_NODE = 10;
}