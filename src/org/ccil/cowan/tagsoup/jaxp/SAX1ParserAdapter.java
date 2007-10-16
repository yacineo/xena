package org.ccil.cowan.tagsoup.jaxp;

import java.io.IOException;
import org.xml.sax.*;

/**
 * This is a simpler adapter class that allows using SAX1 interface on top
 * of basic SAX2 implementation, such as TagSoup.
 *
 * @author Tatu Saloranta (cowtowncoder@yahoo.com)
 * @deprecated
 */
@Deprecated
public class SAX1ParserAdapter implements org.xml.sax.Parser {
	final XMLReader xmlReader;

	public SAX1ParserAdapter(XMLReader xr) {
		xmlReader = xr;
	}

	// Sax1 API impl

	public void parse(InputSource source) throws SAXException {
		try {
			xmlReader.parse(source);
		} catch (IOException ioe) {
			throw new SAXException(ioe);
		}
	}

	public void parse(String systemId) throws SAXException {
		try {
			xmlReader.parse(systemId);
		} catch (IOException ioe) {
			throw new SAXException(ioe);
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
    public void setDocumentHandler(DocumentHandler h) {
		xmlReader.setContentHandler(new DocHandlerWrapper(h));
	}

	public void setDTDHandler(DTDHandler h) {
		xmlReader.setDTDHandler(h);
	}

	public void setEntityResolver(EntityResolver r) {
		xmlReader.setEntityResolver(r);
	}

	public void setErrorHandler(ErrorHandler h) {
		xmlReader.setErrorHandler(h);
	}

	public void setLocale(java.util.Locale locale) throws SAXException {
		/*
		 * I have no idea what this is supposed to do... so let's throw an exception
		 */
		throw new SAXNotSupportedException("TagSoup does not implement setLocale() method");
	}

	// Helper classes:

	/**
	 * We need another helper class to deal with differences between
	 * Sax2 handler (content handler), and Sax1 handler (document handler)
	 * @deprecated
	 */
	@Deprecated
    final static class DocHandlerWrapper implements ContentHandler {
		final DocumentHandler docHandler;

		final AttributesWrapper mAttrWrapper = new AttributesWrapper();

		/**
		 * @deprecated
		 */
		@Deprecated
        DocHandlerWrapper(DocumentHandler h) {
			docHandler = h;
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			docHandler.characters(ch, start, length);
		}

		public void endDocument() throws SAXException {
			docHandler.endDocument();
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName == null) {
				qName = localName;
			}
			docHandler.endElement(qName);
		}

		public void endPrefixMapping(String prefix) {
			// no equivalent in SAX1, ignore
		}

		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			docHandler.ignorableWhitespace(ch, start, length);
		}

		public void processingInstruction(String target, String data) throws SAXException {
			docHandler.processingInstruction(target, data);
		}

		public void setDocumentLocator(Locator locator) {
			docHandler.setDocumentLocator(locator);
		}

		public void skippedEntity(String name) {
			// no equivalent in SAX1, ignore
		}

		public void startDocument() throws SAXException {
			docHandler.startDocument();
		}

		public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
			if (qName == null) {
				qName = localName;
			}
			// Also, need to wrap Attributes to look like AttributeLost
			mAttrWrapper.setAttributes(attrs);
			docHandler.startElement(qName, mAttrWrapper);
		}

		public void startPrefixMapping(String prefix, String uri) {
			// no equivalent in SAX1, ignore
		}
	}

	/**
	 * And one more helper to deal with attribute access differences
	 * @deprecated
	 */
	@Deprecated
    final static class AttributesWrapper implements AttributeList {
		Attributes attrs;

		public AttributesWrapper() {
		}

		public void setAttributes(Attributes a) {
			attrs = a;
		}

		public int getLength() {
			return attrs.getLength();
		}

		public String getName(int i) {
			String n = attrs.getQName(i);
			return (n == null) ? attrs.getLocalName(i) : n;
		}

		public String getType(int i) {
			return attrs.getType(i);
		}

		public String getType(String name) {
			return attrs.getType(name);
		}

		public String getValue(int i) {
			return attrs.getValue(i);
		}

		public String getValue(String name) {
			return attrs.getValue(name);
		}
	}
}
