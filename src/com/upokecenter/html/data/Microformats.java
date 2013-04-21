package com.upokecenter.html.data;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.upokecenter.html.HtmlDocument;
import com.upokecenter.html.IAttr;
import com.upokecenter.html.IComment;
import com.upokecenter.html.IDocument;
import com.upokecenter.html.IDocumentType;
import com.upokecenter.html.IElement;
import com.upokecenter.html.INode;
import com.upokecenter.html.IProcessingInstruction;
import com.upokecenter.html.IText;
import com.upokecenter.html.NodeType;
import com.upokecenter.json.JSONArray;
import com.upokecenter.json.JSONObject;
import com.upokecenter.util.StringUtility;
import com.upokecenter.util.URIUtility;

public final class Microformats {
	private Microformats(){}

	private static final String HTML_NAMESPACE="http://www.w3.org/1999/xhtml";
	private static final String XLINK_NAMESPACE="http://www.w3.org/1999/xlink";

	private static final String XML_NAMESPACE="http://www.w3.org/XML/1998/namespace";
	private static final String MATHML_NAMESPACE = "http://www.w3.org/1998/Math/MathML";

	private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";


	private static String getHref(IElement node){
		String name=StringUtility.toLowerCaseAscii(node.getLocalName());
		String href="";
		if("a".equals(name) || "link".equals(name) || "area".equals(name)){
			href=node.getAttribute("href");
		} else if("object".equals(name)){
			href=node.getAttribute("data");
		} else if("img".equals(name) || "source".equals(name) ||
				"track".equals(name) ||
				"iframe".equals(name) ||
				"audio".equals(name) ||
				"video".equals(name) ||
				"embed".equals(name)){
			href=node.getAttribute("src");
		} else
			return null;
		if(href==null || href.length()==0)
			return "";
		href=HtmlDocument.resolveURL(node,href,null);
		if(href==null || href.length()==0)
			return "";
		return href;
	}


	/**
	 * Gets a Microformats "u-*" value from an HTML element.
	 * It tries to find the URL from the element's attributes,
	 * if possible; otherwise from the element's text.
	 * 
	 * @param e an HTML element.
	 * @return a URL, or the empty string if none was found.
	 */
	private static String getUValue(IElement e){
		String url=getHref(e);
		if(url==null || url.length()==0){
			url=getTrimmedTextContent(e);
			if(URIUtility.isValidIRI(url))
				return url;
			else
				return "";
		}
		return url;
	}

	private static String getTrimmedTextContent(IElement element){
		String trimmed=StringUtility.trimSpaces(element.getTextContent());
		StringBuilder builder=new StringBuilder();
		boolean whitespace=false;
		for(int i=0;i<trimmed.length();i++){
			char c=trimmed.charAt(i);
			if(c==0x09||c==0x0a||c==0x0c||c==0x0d||c==0x20){
				if(!whitespace) {
					builder.append(' ');
				}
				whitespace=true;
			} else {
				whitespace=false;
				builder.append(c);
			}
		}
		return builder.toString();
	}

	private static void setValueIfAbsent(JSONObject obj, String key, Object value){
		if(!obj.has(key)){
			JSONArray arr=null;
			arr=new JSONArray();
			obj.put(key, arr);
			arr.put(value);
		}
	}

	private static void accumulateValue(JSONObject obj, String key, Object value){
		JSONArray arr=null;
		if(obj.has(key)){
			arr=obj.getJSONArray(key);
		} else {
			arr=new JSONArray();
			obj.put(key, arr);
		}
		arr.put(value);
	}

	private static void copyComponents(
			int[] src,
			int[] components,
			boolean useDate,
			boolean useTime,
			boolean useTimezone
			){
		if(useDate){
			if(src[0]!=Integer.MIN_VALUE) {
				components[0]=src[0];
			}
			if(src[1]!=Integer.MIN_VALUE) {
				components[1]=src[1];
			}
			if(src[2]!=Integer.MIN_VALUE) {
				components[2]=src[2];
			}
		}
		if(useTime){
			if(src[3]!=Integer.MIN_VALUE) {
				components[3]=src[3];
			}
			if(src[4]!=Integer.MIN_VALUE) {
				components[4]=src[4];
			}
			if(src[5]!=Integer.MIN_VALUE) {
				components[5]=src[5];
			}
		}
		if(useTimezone){
			if(src[6]!=Integer.MIN_VALUE) {
				components[6]=src[6];
			}
			if(src[7]!=Integer.MIN_VALUE) {
				components[7]=src[7];
			}
		}
	}


	private static boolean matchDateTimePattern(
			String value,
			String[] datePatterns,
			String[] timePatterns,
			int[] components,
			boolean useDate,
			boolean useTime,
			boolean useTimezone
			){
		// year, month, day, hour, minute, second, zone offset,
		// zone offset minutes
		if(!useDate && !useTime && !useTimezone)
			return false;
		int[] c=new int[8];
		int[] c2=new int[8];
		int index=0;
		int oldIndex=index;
		if(datePatterns!=null){
			// match the date patterns, if any
			for(String pattern : datePatterns){
				// reset components
				int endIndex=isDatePattern(value,index,pattern,c);
				if(endIndex>=0){
					// copy any matching components
					if(endIndex>=value.length()){
						copyComponents(c,components,useDate,
								useTime,useTimezone);
						// we have just a date
						return true;
					}
					// match the T
					if(value.charAt(endIndex)!='T')return false;
					index=endIndex+1;
					break;
				}
			}
			if(index==oldIndex)return false;
		} else {
			// Won't match date patterns, so reset all components
			// instead
			c[0]=c[1]=c[2]=c[3]=c[4]=c[5]=c[6]=c[7]=Integer.MIN_VALUE;
		}
		// match the time pattern
		for(String pattern : timePatterns){
			// reset components
			int endIndex=isDatePattern(value,index,pattern,c2);
			if(endIndex==value.length()){
				// copy any matching components
				copyComponents(c,components,useDate,
						useTime,useTimezone);
				copyComponents(c2,components,useDate,
						useTime,useTimezone);
				return true;
			}
		}
		return false;
	}


	private static void append4d(StringBuilder builder, int value){
		value=Math.abs(value);
		builder.append((char)('0'+((value/1000)%10)));
		builder.append((char)('0'+((value/100)%10)));
		builder.append((char)('0'+((value/10)%10)));
		builder.append((char)('0'+((value)%10)));
	}
	private static void append3d(StringBuilder builder, int value){
		value=Math.abs(value);
		builder.append((char)('0'+((value/100)%10)));
		builder.append((char)('0'+((value/10)%10)));
		builder.append((char)('0'+((value)%10)));
	}
	private static void append2d(StringBuilder builder, int value){
		value=Math.abs(value);
		builder.append((char)('0'+((value/10)%10)));
		builder.append((char)('0'+((value)%10)));
	}

	private static String toDateTimeString(int[] components){
		StringBuilder builder=new StringBuilder();
		if(components[0]!=Integer.MIN_VALUE){ // has a date
			// add year
			append4d(builder,components[0]);
			builder.append('-');
			if(components[1]==Integer.MIN_VALUE) {
				append3d(builder,components[2]); // year and day of year
			} else { // has month
				// add month and day
				append2d(builder,components[1]);
				builder.append('-');
				append2d(builder,components[2]);
			}
			// add T if there is a time
			if(components[3]!=Integer.MIN_VALUE) {
				builder.append('T');
			}
		}
		if(components[3]!=Integer.MIN_VALUE){
			append2d(builder,components[3]);
			builder.append(':');
			append2d(builder,components[4]);
			builder.append(':');
			append2d(builder,components[5]);
		}
		if(components[6]!=Integer.MIN_VALUE){
			if(components[6]==0 && components[7]==0) {
				builder.append('Z');
			} else if(components[6]<0){ // negative time zone offset
				builder.append('-');
				append2d(builder,components[6]);
				append2d(builder,components[7]);
			} else { // positive time zone offset
				builder.append('+');
				append2d(builder,components[6]);
				append2d(builder,components[7]);
			}
		}
		return builder.toString();
	}

	private static int isDatePattern(String value, int index, String pattern, int[] components){
		int[] c=components;
		c[0]=c[1]=c[2]=c[3]=c[4]=c[5]=c[6]=c[7]=Integer.MIN_VALUE;
		if(pattern==null)throw new NullPointerException("pattern");
		if(value==null)return -1;
		int patternValue=0;
		int valueIndex=index;
		for(int patternIndex=0;patternIndex<pattern.length();
				patternIndex++){
			if(valueIndex>=value.length())return -1;
			char vc;
			char pc=pattern.charAt(patternIndex);
			if(pc=='%'){
				patternIndex++;
				if(patternIndex>=pattern.length())return -1;
				pc=pattern.charAt(patternIndex);
				if(pc=='D'){// day of year; expect three digits
					if(valueIndex+3>value.length())return -1;
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=(vc-'0');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=patternValue*10+(vc-'0');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=patternValue*10+(vc-'0');
					if(patternValue>366)return -1;
					components[2]=patternValue;
				} else if(pc=='Y'){// year; expect four digits
					if(valueIndex+4>value.length())return -1;
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=(vc-'0');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=patternValue*10+(vc-'0');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=patternValue*10+(vc-'0');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=patternValue*10+(vc-'0');
					components[0]=patternValue;
				} else if(pc=='G'){ // expect 'Z'
					if(valueIndex+1>value.length())return -1;
					vc=value.charAt(valueIndex++);
					if(vc!='Z')return -1;
					components[6]=0; // time zone offset is 0
					components[7]=0;
				} else if(pc=='%'){ // expect 'Z'
					if(valueIndex+1>value.length())return -1;
					vc=value.charAt(valueIndex++);
					if(vc!='%')return -1;
				} else if(pc=='Z'){ // expect plus or minus, then two digits
					if(valueIndex+3>value.length())return -1;
					boolean negative=false;
					vc=value.charAt(valueIndex++);
					if(vc!='+' && vc!='-')return -1;
					negative=(vc=='-');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=(vc-'0');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=patternValue*10+(vc-'0');
					if(pc=='Z' && patternValue>12)
						return -1; // time zone offset hour
					if(negative) {
						patternValue=-patternValue;
					}
					components[6]=patternValue;
				} else if(pc=='M' || pc=='d' || pc=='H' || pc=='h' ||
						pc=='m' || pc=='s' || pc=='z'){ // expect two digits
					if(valueIndex+2>value.length())return -1;
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=(vc-'0');
					vc=value.charAt(valueIndex++);
					if(vc<'0' || vc>'9')return -1;
					patternValue=patternValue*10+(vc-'0');
					if(pc=='M' && patternValue>12)return -1;
					else if(pc=='M') {
						components[1]=patternValue; // month
					} else if(pc=='d' && patternValue>31)return -1;
					else if(pc=='d') {
						components[2]=patternValue;// day
					} else if(pc=='H' && patternValue>=24)return -1;
					else if(pc=='H') {
						components[3]=patternValue;// hour
					} else if(pc=='h' && patternValue>=12 && patternValue!=0)return -1;
					else if(pc=='h') {
						components[3]=patternValue;// hour (12-hour clock)
					} else if(pc=='m' && patternValue>=60)return -1;
					else if(pc=='m') {
						components[4]=patternValue;// minute
					} else if(pc=='s' && patternValue>60)return -1;
					else if(pc=='s') {
						components[5]=patternValue;// second
					} else if(pc=='z' && patternValue>=60)return -1;
					else if(pc=='z')
					{
						components[7]=patternValue;// timezone offset minute
					}
				} else return -1;
			} else {
				vc=value.charAt(valueIndex++);
				if(vc!=pc)return -1;
			}
		}
		// Special case: day of year
		if(components[2]!=Integer.MIN_VALUE &&
				components[0]!=Integer.MIN_VALUE &&
				components[1]==Integer.MIN_VALUE){
			int[] monthDay=getMonthAndDay(components[0],components[2]);
			//DebugUtility.log("monthday %d->%d %d",components[2],monthDay[0],monthDay[1]);
			if(monthDay==null)return -1;
			components[1]=monthDay[0];
			components[2]=monthDay[1];
		}
		if(components[3]!=Integer.MIN_VALUE &&
				components[4]==Integer.MIN_VALUE) {
			components[4]=0;
		}
		if(components[4]!=Integer.MIN_VALUE &&
				components[5]==Integer.MIN_VALUE) {
			components[5]=0;
		}
		// Special case: time zone offset
		if(components[6]!=Integer.MIN_VALUE &&
				components[7]==Integer.MIN_VALUE){
			//DebugUtility.log("spcase");
			components[7]=0;
		}
		return valueIndex;
	}

	private static Map<String,String[]> complexLegacyMap=new HashMap<String,String[]>();

	static {
		complexLegacyMap.put("adr",new String[]{"p-adr","h-adr"});
		complexLegacyMap.put("affiliation",new String[]{"p-affiliation","h-card"});
		complexLegacyMap.put("author",new String[]{"p-author","h-card"});
		complexLegacyMap.put("contact",new String[]{"p-contact","h-card"});
		complexLegacyMap.put("education",new String[]{"p-education","h-event"});
		complexLegacyMap.put("experience",new String[]{"p-experience","h-event"});
		complexLegacyMap.put("fn",new String[]{"p-item","h-item","p-name"});
		complexLegacyMap.put("geo",new String[]{"p-geo","h-geo"});
		complexLegacyMap.put("location",new String[]{"p-location","h-card","h-adr"});
		complexLegacyMap.put("photo",new String[]{"p-item","h-item","u-photo"});
		complexLegacyMap.put("review",new String[]{"p-review","h-review"});
		complexLegacyMap.put("reviewer",new String[]{"p-reviewer","h-card"});
		complexLegacyMap.put("url",new String[]{"p-item","h-item","u-url"});
	}

	private static String[] parseLegacyRel(String str){
		String[] ret=StringUtility.splitAtSpaces(
				StringUtility.toLowerCaseAscii(str));
		if(ret.length==0)return ret;
		List<String> relList=new ArrayList<String>();
		boolean hasTag=false;
		boolean hasSelf=false;
		boolean hasBookmark=false;
		for (String element : ret) {
			if(!hasTag && "tag".equals(element)){
				relList.add("p-category");
				hasTag=true;
			} else if(!hasSelf && "self".equals(element)){
				if(hasBookmark) {
					relList.add("u-url");
				}
				hasSelf=true;
			} else if(!hasBookmark && "bookmark".equals(element)){
				if(hasSelf) {
					relList.add("u-url");
				}
				hasBookmark=true;
			}
		}
		return relList.toArray(new String[]{});
	}

	private static String[] getClassNames(IElement element){
		String[] ret=StringUtility.splitAtSpaces(element.getAttribute("class"));
		String[] rel=parseLegacyRel(element.getAttribute("rel"));
		if(ret.length==0 && rel.length==0)return ret;
		// Replace old microformats class names with
		// their modern versions
		List<String> retList=new ArrayList<String>();
		for (String element2 : rel) {
			retList.add(element2);
		}
		for (String element2 : ret) {
			String legacyLabel=legacyLabelsMap.get(element2);
			if(complexLegacyMap.containsKey(element2)){
				for(String item : complexLegacyMap.get(element2)){
					retList.add(item);
				}
			}
			else if(legacyLabel!=null) {
				retList.add(legacyLabel);
			} else {
				retList.add(element2);
			}
		}
		if(retList.size()>=2){
			Set<String> stringSet=new HashSet<String>(retList);
			return stringSet.toArray(new String[]{});
		} else
			return retList.toArray(new String[]{});
	}

	private static String[] getRelNames(IElement element){
		String[] ret=StringUtility.splitAtSpaces(
				StringUtility.toLowerCaseAscii(element.getAttribute("rel")));
		if(ret.length==0)return ret;
		List<String> retList=new ArrayList<String>();
		for (String element2 : ret) {
			retList.add(element2);
		}
		if(retList.size()>=2){
			Set<String> stringSet=new HashSet<String>(retList);
			return stringSet.toArray(new String[]{});
		} else
			return retList.toArray(new String[]{});
	}

	private static List<IElement> getChildElements(INode e){
		List<IElement> elements=new ArrayList<IElement>();
		for(INode child : e.getChildNodes()){
			if(child instanceof IElement) {
				elements.add((IElement)child);
			}
		}
		return elements;
	}

	private static IElement getFirstChildElement(INode e){
		for(INode child : e.getChildNodes()){
			if(child instanceof IElement)
				return ((IElement)child);
		}
		return null;
	}

	private static JSONObject copyJson(JSONObject obj){
		try {
			return new JSONObject(obj.toString());
		} catch (ParseException e) {
			return null;
		}
	}

	private static boolean hasSingleChildElementNamed(INode e, String name){
		boolean seen=false;
		for(INode child : e.getChildNodes()){
			if(child instanceof IElement){
				if(seen)return false;
				if(!StringUtility.toLowerCaseAscii(((IElement)child).getLocalName()).equals(name))
					return false;
				seen=true;
			}
		}
		return seen;
	}

	private static final String[] legacyLabels=new String[]{
		"additional-name", "p-additional-name", "adr", "h-adr", "bday", "dt-bday", "best", "p-best", "brand", "p-brand", "category", "p-category", "count", "p-count", "country-name", "p-country-name", "description", "e-description", "dtend", "dt-end", "dtreviewed", "dt-dtreviewed", "dtstart", "dt-start", "duration", "dt-duration", "e-entry-summary", "e-summary", "email", "u-email", "entry-content", "e-content", "entry-summary", "p-summary", "entry-title",
		"p-name", "extended-address", "p-extended-address", "family-name", "p-family-name", "fn", "p-name", "geo", "h-geo", "given-name", "p-given-name", "hentry", "h-entry", "honorific-prefix", "p-honorific-prefix", "honorific-suffix", "p-honorific-suffix", "hproduct", "h-product", "hrecipe", "h-recipe", "hresume", "h-resume", "hreview", "h-review", "hreview-aggregate", "h-review-aggregate", "identifier", "u-identifier", "ingredient", "p-ingredient",
		"instructions", "e-instructions", "key", "u-key", "label", "p-label", "latitude", "p-latitude", "locality", "p-locality", "logo", "u-logo", "longitude", "p-longitude", "nickname", "p-nickname", "note", "p-note", "nutrition", "p-nutrition", "org", "p-org", "organization-name", "p-organization-name", "organization-unit", "p-organization-unit", "p-entry-summary", "p-summary", "p-entry-title", "p-name", "photo", "u-photo", "post-office-box", "p-post-office-box",
		"postal-code", "p-postal-code", "price", "p-price", "published", "dt-published", "rating", "p-rating", "region", "p-region", "rev", "dt-rev", "skill", "p-skill", "street-address", "p-street-address", "summary", "p-name", "tel", "p-tel", "tz", "p-tz", "uid", "u-uid", "updated", "dt-updated", "url", "p-url", "vcard", "h-card", "vevent", "h-event", "votes", "p-votes", "worst", "p-worst", "yield", "p-yield"
	};

	private static final Map<String,String> legacyLabelsMap=createLegacyLabelsMap();



	private static boolean implyForLink(IElement root, JSONObject subProperties){
		if(StringUtility.toLowerCaseAscii(root.getLocalName()).equals("a") &&
				root.getAttribute("href")!=null){
			// get the link's URL
			setValueIfAbsent(subProperties,"url", getUValue(root));
			List<IElement> elements=getChildElements(root);
			if(elements.size()==1 &&
					StringUtility.toLowerCaseAscii(elements.get(0).getLocalName()).equals("img")){
				String pValue=getPValue(elements.get(0)); // try to get the ALT/TITLE from the image
				if(StringUtility.isNullOrSpaces(pValue))
				{
					pValue=getPValue(root); // if empty, get text from link instead
				}
				setValueIfAbsent(subProperties,"name", pValue);
				// get the SRC of the image
				setValueIfAbsent(subProperties,"photo", getUValue(elements.get(0)));
			} else {
				// get the text content
				String pvalue=getPValue(root);
				if(!StringUtility.isNullOrSpaces(pvalue)) {
					setValueIfAbsent(subProperties,"name", pvalue);
				}
			}
			return true;
		}
		return false;
	}

	private static Map<String, String> createLegacyLabelsMap() {
		Map<String, String> map=new HashMap<String,String>();
		for(int i=0;i<legacyLabels.length;i+=2){
			map.put(legacyLabels[i], legacyLabels[i+1]);
		}
		return map;
	}

	private static void propertyWalk(IElement root,
			JSONObject properties, JSONArray children){
		String[] className=getClassNames(root);
		if(className.length>0){
			List<String> types=new ArrayList<String>();
			boolean hasProperties=false;
			for(String cls : className){
				if(cls.startsWith("p-") && properties!=null){
					hasProperties=true;
				} else if(cls.startsWith("u-") && properties!=null){
					hasProperties=true;
				} else if(cls.startsWith("dt-") && properties!=null){
					hasProperties=true;
				} else if(cls.startsWith("e-") && properties!=null){
					hasProperties=true;
				} else if(cls.startsWith("h-")){
					types.add(cls);
				}
			}
			if(types.size()==0 && hasProperties){
				// has properties and isn't a microformat
				// root
				for(String cls : className){
					if(cls.startsWith("p-")){
						String value=getPValue(root);
						if(!StringUtility.isNullOrSpaces(value)) {
							accumulateValue(properties,cls.substring(2),value);
						}
					} else if(cls.startsWith("u-")){
						accumulateValue(properties,cls.substring(2),
								getUValue(root));
					} else if(cls.startsWith("dt-")){
						accumulateValue(properties,cls.substring(3),
								getDTValue(root,getLastKnownTime(properties)));
					} else if(cls.startsWith("e-")){
						accumulateValue(properties,cls.substring(2),
								getEValue(root));
					}
				}
			} else if(types.size()>0){
				// this is a child microformat
				// with no properties
				JSONObject obj=new JSONObject();
				obj.put("type", new JSONArray(types));
				// for holding child elements with
				// properties
				JSONObject subProperties=new JSONObject();
				// for holding child microformats with no
				// property class
				JSONArray subChildren=new JSONArray();
				for(INode child : root.getChildNodes()){
					if(child instanceof IElement) {
						propertyWalk((IElement)child,
								subProperties,subChildren);
					}
				}
				if(subChildren.length()>0){
					obj.put("children", subChildren);
				}
				if(types.size()>0){
					// we imply missing properties here
					// Imply p-name and p-url
					if(!implyForLink(root,subProperties)){
						if(hasSingleChildElementNamed(root,"a")){
							implyForLink(getFirstChildElement(root),subProperties);
						} else {
							String pvalue=getPValue(root);
							if(!StringUtility.isNullOrSpaces(pvalue)) {
								setValueIfAbsent(subProperties,"name", pvalue);
							}
						}
					}
					// Also imply u-photo
					if(StringUtility.toLowerCaseAscii(root.getLocalName()).equals("img") &&
							root.getAttribute("src")!=null){
						setValueIfAbsent(subProperties,"photo", getUValue(root));
					}
					if(!subProperties.has("photo")){
						List<IElement> images=root.getElementsByTagName("img");
						// If there is only one descendant image, imply
						// u-photo
						if(images.size()==1){
							setValueIfAbsent(subProperties,"photo",
									getUValue(images.get(0)));
						}
					}
				}
				obj.put("properties", subProperties);
				if(hasProperties){
					for(String cls : className){
						if(cls.startsWith("p-")){ // property
							JSONObject clone=copyJson(obj);
							clone.put("value",getPValue(root));
							accumulateValue(properties,cls.substring(2),clone);
						} else if(cls.startsWith("u-")){ // URL
							JSONObject clone=copyJson(obj);
							clone.put("value",getUValue(root));
							accumulateValue(properties,cls.substring(2),clone);
						} else if(cls.startsWith("dt-")){ // date/time
							JSONObject clone=copyJson(obj);
							clone.put("value",getDTValue(root,getLastKnownTime(properties)));
							accumulateValue(properties,cls.substring(3),clone);
						} else if(cls.startsWith("e-")){ // date/time
							JSONObject clone=copyJson(obj);
							clone.put("value",getEValue(root));
							accumulateValue(properties,cls.substring(2),clone);
						}
					}
				} else {
					children.put(obj);
				}
				return;
			}
		}
		for(INode child : root.getChildNodes()){
			if(child instanceof IElement) {
				propertyWalk((IElement)child,properties,children);
			}
		}
	}


	private static void relWalk(IElement root,
			JSONObject properties){
		String[] className=getRelNames(root);
		if(className.length>0){
			String href=getHref(root);
			if(!StringUtility.isNullOrSpaces(href)){
				for(String cls : className){
					accumulateValue(properties,cls,href);
				}
			}
		}
		for(INode child : root.getChildNodes()){
			if(child instanceof IElement) {
				relWalk((IElement)child,properties);
			}
		}
	}
	private static void fragmentSerializeInner(
			INode current, StringBuilder builder){
		if(current.getNodeType()==NodeType.ELEMENT_NODE){
			IElement e=((IElement)current);
			String tagname=e.getTagName();
			String namespaceURI=e.getNamespaceURI();
			if(HTML_NAMESPACE.equals(namespaceURI) ||
					SVG_NAMESPACE.equals(namespaceURI) ||
					MATHML_NAMESPACE.equals(namespaceURI)){
				tagname=e.getLocalName();
			}
			builder.append('<');
			builder.append(tagname);
			for(IAttr attr : e.getAttributes()){
				namespaceURI=attr.getNamespaceURI();
				builder.append(' ');
				if(namespaceURI==null || namespaceURI.length()==0){
					builder.append(attr.getLocalName());
				} else if(namespaceURI.equals(XML_NAMESPACE)){
					builder.append("xml:");
					builder.append(attr.getLocalName());
				} else if(namespaceURI.equals(
						"http://www.w3.org/2000/xmlns/")){
					if(!"xmlns".equals(attr.getLocalName())) {
						builder.append("xmlns:");
					}
					builder.append(attr.getLocalName());
				} else if(namespaceURI.equals(XLINK_NAMESPACE)){
					builder.append("xlink:");
					builder.append(attr.getLocalName());
				} else {
					builder.append(attr.getName());
				}
				builder.append("=\"");
				String value=attr.getValue();
				for(int i=0;i<value.length();i++){
					char c=value.charAt(i);
					if(c=='&') {
						builder.append("&amp;");
					} else if(c==0xa0) {
						builder.append("&nbsp;");
					} else if(c=='"') {
						builder.append("&quot;");
					} else {
						builder.append(c);
					}
				}
				builder.append('"');
			}
			builder.append('>');
			if(HTML_NAMESPACE.equals(namespaceURI)){
				String localName=e.getLocalName();
				if("area".equals(localName) ||
						"base".equals(localName) ||
						"basefont".equals(localName) ||
						"bgsound".equals(localName) ||
						"br".equals(localName) ||
						"col".equals(localName) ||
						"embed".equals(localName) ||
						"frame".equals(localName) ||
						"hr".equals(localName) ||
						"img".equals(localName) ||
						"input".equals(localName) ||
						"keygen".equals(localName) ||
						"link".equals(localName) ||
						"menuitem".equals(localName) ||
						"meta".equals(localName) ||
						"param".equals(localName) ||
						"source".equals(localName) ||
						"track".equals(localName) ||
						"wbr".equals(localName))
					return;
				if("pre".equals(localName) ||
						"textarea".equals(localName) ||
						"listing".equals(localName)){
					for(INode node : e.getChildNodes()){
						if(node.getNodeType()==NodeType.TEXT_NODE &&
								((IText)node).getData().length()>0 &&
								((IText)node).getData().charAt(0)=='\n'){
							builder.append('\n');
						}
					}
				}
			}
			// Recurse
			for(INode child : e.getChildNodes()){
				fragmentSerializeInner(child,builder);
			}
			builder.append("</");
			builder.append(tagname);
			builder.append(">");
		} else if(current.getNodeType()==NodeType.TEXT_NODE){
			INode parent=current.getParentNode();
			if(parent instanceof IElement &&
					HTML_NAMESPACE.equals(((IElement)parent).getNamespaceURI())){
				String localName=((IElement)parent).getLocalName();
				if("script".equals(localName) ||
						"style".equals(localName) ||
						"script".equals(localName) ||
						"xmp".equals(localName) ||
						"iframe".equals(localName) ||
						"noembed".equals(localName) ||
						"noframes".equals(localName) ||
						"plaintext".equals(localName)){
					builder.append(((IText)current).getData());
				} else {
					String value=((IText)current).getData();
					for(int i=0;i<value.length();i++){
						char c=value.charAt(i);
						if(c=='&') {
							builder.append("&amp;");
						} else if(c==0xa0) {
							builder.append("&nbsp;");
						} else if(c=='<') {
							builder.append("&lt;");
						} else if(c=='>') {
							builder.append("&gt;");
						} else {
							builder.append(c);
						}
					}
				}
			}
		} else if(current.getNodeType()==NodeType.COMMENT_NODE){
			builder.append("<!--");
			builder.append(((IComment)current).getData());
			builder.append("-->");
		} else if(current.getNodeType()==NodeType.DOCUMENT_TYPE_NODE){
			builder.append("<!DOCTYPE ");
			builder.append(((IDocumentType)current).getName());
			builder.append(">");
		} else if(current.getNodeType()==NodeType.PROCESSING_INSTRUCTION_NODE){
			builder.append("<?");
			builder.append(((IProcessingInstruction)current).getTarget());
			builder.append(' ');
			builder.append(((IProcessingInstruction)current).getData());
			builder.append(">");			// NOTE: may be erroneous
		}
	}
	private static String fragmentSerialize(INode node){
		StringBuilder builder=new StringBuilder();
		for(INode child : node.getChildNodes()){
			fragmentSerializeInner(child,builder);
		}
		return builder.toString();
	}

	private static String getPValue(IElement root) {
		if(root.getAttribute("title")!=null)
			return root.getAttribute("title");
		if(StringUtility.toLowerCaseAscii(root.getLocalName()).equals("img") &&
				!StringUtility.isNullOrSpaces(root.getAttribute("alt")))
			return root.getAttribute("alt");
		return getValueContent(root,false);
	}

	private static void getValueClassInner(IElement root, List<IElement> elements){
		String[] cls=getClassNames(root);
		// Check if this is a value
		for(String c : cls){
			if(c.equals("value")){
				elements.add(root);
				return;
			} else if(c.equals("value-title")){
				elements.add(root);
				return;
			}
		}
		// Not a value; check if this is a property
		for(String c : cls){
			if(c.startsWith("p-") ||
					c.startsWith("e-") ||
					c.startsWith("dt-") ||
					c.startsWith("u-"))
				// don't traverse
				return;
		}
		for(IElement element : getChildElements(root)){
			getValueClassInner(element,elements);
		}
	}

	private static boolean hasClassName(IElement e, String className){
		String attr=e.getAttribute("class");
		if(attr==null || attr.length()<className.length())return false;
		String[] cls=StringUtility.splitAtSpaces(attr);
		for(String c : cls){
			if(c.equals(className))return true;
		}
		return false;
	}

	private static List<IElement> getValueClasses(IElement root){
		List<IElement> elements=new ArrayList<IElement>();
		for(IElement element : getChildElements(root)){
			getValueClassInner(element,elements);
		}
		return elements;
	}

	private static String elementName(IElement element){
		return StringUtility.toLowerCaseAscii(element.getLocalName());
	}

	private static String valueOrEmpty(String s){
		return s==null ? "" : s;
	}

	private static String getValueElementContent(IElement valueElement){
		if(hasClassName(valueElement,"value-title"))
			// If element has the value-title class, use
			// the title instead
			return valueOrEmpty(valueElement.getAttribute("title"));
		else if(elementName(valueElement).equals("img") ||
				elementName(valueElement).equals("area")){
			String s=valueElement.getAttribute("alt");
			return (s==null) ? "" : s;
		} else if(elementName(valueElement).equals("data")){
			String s=valueElement.getAttribute("value");
			return (s==null) ? getTrimmedTextContent(valueElement) : s;
		} else if(elementName(valueElement).equals("abbr")){
			String s=valueElement.getAttribute("title");
			return (s==null) ? getTrimmedTextContent(valueElement) : s;
		} else
			return getTrimmedTextContent(valueElement);
	}


	private static String getValueContent(IElement root, boolean dt){
		List<IElement> elements=getValueClasses(root);
		if(elements.size()==0)
			// No value elements, get the text content
			return getValueElementContent(root);
		else if(elements.size()==1){
			// One value element
			IElement valueElement=elements.get(0);
			return getValueElementContent(valueElement);
		} else {
			StringBuilder builder=new StringBuilder();
			boolean first=true;
			for(IElement element : elements){
				if(!first) {
					builder.append(' ');
				}
				first=false;
				builder.append(getValueElementContent(element));
			}
			return builder.toString();
		}
	}

	private static int[] getLastKnownTime(JSONObject obj){
		if(obj.has("start")){
			JSONArray arr=obj.getJSONArray("start");
			//DebugUtility.log("start %s",arr);
			Object result=arr.get(arr.length()-1);
			if(result instanceof String){
				int[] components=new int[]{
						Integer.MIN_VALUE,
						Integer.MIN_VALUE,
						Integer.MIN_VALUE,
						Integer.MIN_VALUE,
						Integer.MIN_VALUE,
						Integer.MIN_VALUE,
						Integer.MIN_VALUE,
						Integer.MIN_VALUE
				};
				if(matchDateTimePattern((String)result,
						new String[]{"%Y-%M-%d","%Y-%D"},
						new String[]{"%H:%m:%s","%H:%m",
						"%H:%m:%s%Z:%z",
						"%H:%m:%s%Z%z","%H:%m:%s%G",
						"%H:%m%Z:%z","%H:%m%Z%z","%H:%m%G"},
						components,true,true,true)){
					// reset the time components
					components[3]=Integer.MIN_VALUE;
					components[4]=Integer.MIN_VALUE;
					components[5]=Integer.MIN_VALUE;
					components[6]=Integer.MIN_VALUE;
					components[7]=Integer.MIN_VALUE;
					//DebugUtility.log("match %s",Arrays.toString(components));
					return components;
				} else {
					//DebugUtility.log("no match");
				}
			}
		}
		return null;
	}

	private static String getDTValueContent(IElement valueElement){
		String elname=elementName(valueElement);
		String text="";
		if(hasClassName(valueElement,"value-title"))
			return valueOrEmpty(valueElement.getAttribute("title"));
		else if(elname.equals("img") || elname.equals("area")){
			String s=valueElement.getAttribute("alt");
			text=(s==null) ? "" : s;
		} else if(elname.equals("data")){
			String s=valueElement.getAttribute("value");
			text=(s==null) ? getTrimmedTextContent(valueElement) : s;
		} else if(elname.equals("abbr")){
			String s=valueElement.getAttribute("title");
			text=(s==null) ? getTrimmedTextContent(valueElement) : s;
		} else if(elname.equals("del") || elname.equals("ins") || elname.equals("time")){
			String s=valueElement.getAttribute("datetime");
			if(StringUtility.isNullOrSpaces(s)) {
				s=valueElement.getAttribute("title");
			}
			text=(s==null) ? getTrimmedTextContent(valueElement) : s;
		} else {
			text=getTrimmedTextContent(valueElement);
		}
		return text;
	}

	private static String getDTValue(IElement root, int[] source) {
		List<IElement> valueElements=getValueClasses(root);
		boolean haveDate=false,haveTime=false,haveTimeZone=false;
		int[] components=new int[]{
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MIN_VALUE
		};
		if(source!=null){
			copyComponents(source,components,true,true,true);
		}
		if(valueElements.size()==0)
			// No value elements, get the text content
			return getDTValueContent(root);
		for(IElement valueElement : valueElements){
			String text=getDTValueContent(valueElement);
			if(matchDateTimePattern(text, // check date or date+time
					new String[]{"%Y-%M-%d","%Y-%D"},
					new String[]{"%H:%m:%s","%H:%m",
					"%H:%m:%s%Z:%z",
					"%H:%m:%s%Z%z","%H:%m:%s%G",
					"%H:%m%Z:%z","%H:%m%Z%z",
			"%H:%m%G"},components,!haveDate,!haveTime,!haveTimeZone)){
				// check if components are defined
				if(components[0]!=Integer.MIN_VALUE) {
					haveDate=true;
				}
				if(components[3]!=Integer.MIN_VALUE) {
					haveTime=true;
				}
				if(components[6]!=Integer.MIN_VALUE) {
					haveTimeZone=true;
				}
			} else if(matchDateTimePattern(text, // check time-only formats
					null,
					new String[]{"%H:%m:%s","%H:%m",
					"%H:%m:%s%Z:%z",
					"%H:%m:%s%Z%z","%H:%m:%s%G",
					"%H:%m%Z:%z","%H:%m%Z%z",
			"%H:%m%G"},components,false,!haveTime,!haveTimeZone)){
				// check if components are defined
				if(components[3]!=Integer.MIN_VALUE) {
					haveTime=true;
				}
				if(components[6]!=Integer.MIN_VALUE) {
					haveTimeZone=true;
				}
			} else if(matchDateTimePattern(text,
					null,new String[]{"%Z:%z","%Z%z","%Z","%G"},
					components,false,false,!haveTimeZone)){ // check timezone formats
				if(components[6]!=Integer.MIN_VALUE) {
					haveTimeZone=true;
				}
			} else if(matchDateTimePattern(StringUtility.toLowerCaseAscii(text),
					null,new String[]{"%h:%m:%sa.m.", // AM clock values
				"%h:%m:%sam","%h:%ma.m.","%h:%mam",
				"%ha.m.","%ham"},
				components,false,!haveTime,false)){ // check AM time formats
				if(components[3]!=Integer.MIN_VALUE){
					haveTime=true;
					// convert AM hour to 24-hour clock
					if(components[3]==12) {
						components[3]=0;
					}
				}
			} else if(matchDateTimePattern(StringUtility.toLowerCaseAscii(text),
					null,new String[]{"%h:%m:%sp.m.", // PM clock values
				"%h:%m:%spm","%h:%mp.m.","%h:%mpm","%hp.m.","%hpm"},
				components,false,!haveTime,false)){ // check PM time formats
				if(components[3]!=Integer.MIN_VALUE){
					haveTime=true;
					// convert PM hour to 24-hour clock
					if(components[3]<12) {
						components[3]+=12;
					}
				}
			}
		}
		if(components[0]!=Integer.MIN_VALUE)
			return toDateTimeString(components);
		return getDTValueContent(root);

	}

	private static String getEValue(IElement root) {
		return fragmentSerialize(root);
	}

	private static final int[] normalDays = {
		0, 31, 28, 31, 30, 31, 30, 31, 31, 30,
		31, 30, 31 };
	private static final int[] leapDays = {
		0, 31, 29, 31, 30, 31, 30, 31, 31, 30,
		31, 30, 31 };

	private static int[] getMonthAndDay(int year, int day){
		assert day>=0;
		int[] dayArray = ((year & 3) != 0 || (year % 100 == 0 && year % 400 != 0)) ?
				normalDays : leapDays;
		int month=1;
		while(day<=0 || day>dayArray[month]){
			if(day>dayArray[month]){
				day-=dayArray[month];
				month++;
				if(month>12)return null;
			}
			if(day<=0){
				month--;
				if(month<1)return null;
				day+=dayArray[month];
			}
		}
		assert month>=1 : month+" "+day;
		assert month<=12 : month+" "+day;
		assert day>=1 : month+" "+day;
		assert day<=31 : month+" "+day;
		return new int[]{month,day};
	}

	/**
	 * 
	 * Scans an HTML document for Microformats.org metadata.
	 * The resulting object will contain an "items" property,
	 * an array of all Microformats items.  Each item will
	 * have a "type" and "properties" properties.
	 * 
	 * @param root the document to scan.
	 * @return a JSON object containing Microformats metadata
	 */
	public static JSONObject getMicroformatsJSON(IDocument root){
		if((root)==null)throw new NullPointerException("root");
		return getMicroformatsJSON(root.getDocumentElement());
	}

	/**
	 * 
	 * Scans an HTML element for Microformats.org metadata.
	 * The resulting object will contain an "items" property,
	 * an array of all Microformats items.  Each item will
	 * have a "type" and "properties" properties.
	 * 
	 * @param root the element to scan.
	 * @return a JSON object containing Microformats metadata
	 */
	public static JSONObject getMicroformatsJSON(IElement root){
		if((root)==null)throw new NullPointerException("root");
		JSONObject obj=new JSONObject();
		JSONArray items=new JSONArray();
		propertyWalk(root,null,items);
		obj.put("items", items);
		return obj;
	}

	public static JSONObject getRelJSON(IDocument root){
		if((root)==null)throw new NullPointerException("root");
		return getRelJSON(root.getDocumentElement());
	}

	public static JSONObject getRelJSON(IElement root){
		if((root)==null)throw new NullPointerException("root");
		JSONObject obj=new JSONObject();
		JSONArray items=new JSONArray();
		JSONObject item=new JSONObject();
		accumulateValue(item,"type","rel");
		JSONObject props=new JSONObject();
		relWalk(root,props);
		item.put("properties", props);
		items.put(item);
		obj.put("items", items);
		return obj;
	}
}
