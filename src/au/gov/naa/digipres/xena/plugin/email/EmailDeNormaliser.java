/*
 * Created on 12/03/2007
 * justinw5
 * 
 */
package au.gov.naa.digipres.xena.plugin.email;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.AttributesImpl;

import au.gov.naa.digipres.xena.kernel.XenaException;
import au.gov.naa.digipres.xena.kernel.XenaInputSource;
import au.gov.naa.digipres.xena.kernel.normalise.AbstractDeNormaliser;
import au.gov.naa.digipres.xena.util.FilenameEncoder;
import au.gov.naa.digipres.xena.util.XmlDeNormaliser;

public class EmailDeNormaliser extends AbstractDeNormaliser
{
	private static final String XHTML_URI = "http://www.w3.org/1999/xhtml";
	
	private TransformerHandler attachmentXMLWriter;
	private TransformerHandler rootXMLWriter;
	private File tempAttachmentFile;
	private OutputStream attachmentOutputStream;
	private int partLevel = 0;
	private int topLevelAttachmentIndex = -1;
	private String currentAttachmentName;

	@Override
	public String getName()
	{
		return "Email Denormaliser";
	}

	/* (non-Javadoc)
	 * @see au.gov.naa.digipres.xena.kernel.normalise.AbstractDeNormaliser#getOutputFileExtension(au.gov.naa.digipres.xena.kernel.XenaInputSource)
	 */
	@Override
	public String getOutputFileExtension(XenaInputSource xis) throws XenaException
	{
		return "xml";
	}

	/* (non-Javadoc)
	 * @see au.gov.naa.digipres.xena.kernel.normalise.AbstractDeNormaliser#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		if (topLevelAttachmentIndex == -1)
		{
			// We are not in the top-level attachment so write out to root output
			rootXMLWriter.characters(ch, start, length);
		}
		else
		{
			// We are in the top-level attachment so write out to attachment output
			attachmentXMLWriter.characters(ch, start, length);
		}
	}

	/* (non-Javadoc)
	 * @see au.gov.naa.digipres.xena.kernel.normalise.AbstractDeNormaliser#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException
	{
		// Initialise the writer for the root XML file
		
        // create our transform handler
        SAXTransformerFactory transformFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        try 
        {
            rootXMLWriter = transformFactory.newTransformerHandler();
        	rootXMLWriter.setResult(streamResult);
        	rootXMLWriter.startDocument();
        } 
        catch (TransformerConfigurationException e) 
        {
            throw new SAXException("Unable to create transformerHandler due to transformer configuration exception.");
        }
	}

	/* (non-Javadoc)
	 * @see au.gov.naa.digipres.xena.kernel.normalise.AbstractDeNormaliser#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException
	{
        rootXMLWriter.endDocument();		
	}

	/* (non-Javadoc)
	 * @see au.gov.naa.digipres.xena.kernel.normalise.AbstractDeNormaliser#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException
	{
		boolean elementIsTopLevelAttachment = false;
		if (qName.equalsIgnoreCase(MessageNormaliser.EMAIL_PREFIX + ":" + MessageNormaliser.PART_TAG))
		{
			if (partLevel == topLevelAttachmentIndex)
			{
				elementIsTopLevelAttachment = true;
				try
				{
					// Close writers
					attachmentXMLWriter.endDocument();
					attachmentOutputStream.flush();
					attachmentOutputStream.close();
				}
				catch (IOException e)
				{
					throw new SAXException("Problem closing output stream.", e);
				}
				
				// Export current attachment, using the temporary xml file we have created
				try
				{
					XenaInputSource xis = new XenaInputSource(tempAttachmentFile);
					
			        // get the unwrapper for this package. This will most likely be empty as multipage does
					// not currently wrap the pages it contains separately, however you never know what the
					// future will hold... use the EmptyWrapper if we can't find a wrapper.
			        XMLFilter unwrapper;
			        try 
			        {
			            unwrapper = normaliserManager.getPluginManager().getMetaDataWrapperManager().getUnwrapper(xis);
			        } 
			        catch (XenaException xe) 
			        {
			            unwrapper = normaliserManager.getPluginManager().getMetaDataWrapperManager().getEmptyWrapper().getUnwrapper();
			        }
			           
			        // Get the appropriate denormaliser
			        String tag = normaliserManager.unwrapGetTag(xis, unwrapper);
			        AbstractDeNormaliser deNormaliser = normaliserManager.lookupDeNormaliser(tag);
			        if (deNormaliser == null) 
			        {
			        	// Just use basic XML denormaliser
			        	deNormaliser = new XmlDeNormaliser();
			        }
			        
			        // Construct output file name
			        String outputFileExtension = deNormaliser.getOutputFileExtension(xis);
			        String pageOutputFilename = currentAttachmentName;
			        
			        /*
			         * This code adds the extension that the type gives us _if_ the name given to us
			         * by the meta data wrapper does not have the same extension. This could happen in a
			         * number of situations, most notably, for the plaintext, the default extension is
			         * txt, however many file extensions are valid text files. at the end of the day, this
			         * will at least reduce the instances of simple.txt -> simple.txt.txt and the like,
			         * and still give a reasonable indication of what is actually in the file.
			         * 
			         */
			        if (outputFileExtension != null) 
			        {
			            if (!(pageOutputFilename.toLowerCase().endsWith("." + outputFileExtension.toLowerCase()))) 
			            {
			            	pageOutputFilename = pageOutputFilename + "." + outputFileExtension;
			            }
			        }
			        
			        // If the attachment is an email, then it won't be a proper filename and could contain
			        // characters that are invalid for filenames (eg a ':' in RE:). The current solution is
			        // to URL encode the filename... this means ugly-looking filenames but at least it will work!
			        pageOutputFilename = FilenameEncoder.encode(pageOutputFilename);
			        
			        // Export file
					normaliserManager.export(xis, outputDirectory, pageOutputFilename, true);
					
					// Add link to attachment in root output
			        AttributesImpl atts = new AttributesImpl();
			        atts.addAttribute(XHTML_URI, "href", "href", "CDATA", pageOutputFilename);
			        rootXMLWriter.startElement(XHTML_URI, "a", "a", atts);
			        char[] charArr = pageOutputFilename.toCharArray();
			        rootXMLWriter.characters(charArr, 0, charArr.length);
			        rootXMLWriter.endElement(XHTML_URI, "a", "a");
				}
				catch (Exception ex)
				{
					throw new SAXException("Problem exporting an email attachment.", ex);
				}
			}
			
			// We are ending an <email:part> tag
			partLevel--;
		}		
		
		if (topLevelAttachmentIndex == -1)
		{
			// We are not in the top-level attachment so write out to root output
			rootXMLWriter.endElement(namespaceURI, localName, qName);
		}
		else
		{
			// We are in the top-level attachment, but if this is the end of the element we don't want to write it to the attachment output
			if (elementIsTopLevelAttachment)
			{
				rootXMLWriter.endElement(namespaceURI, localName, qName);
				topLevelAttachmentIndex = -1;
			}
			else
			{
				attachmentXMLWriter.endElement(namespaceURI, localName, qName);
			}
		}
			
	}

	/* (non-Javadoc)
	 * @see au.gov.naa.digipres.xena.kernel.normalise.AbstractDeNormaliser#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
	{
		// Check for the start of a new attachment
		boolean elementIsTopLevelAttachment = false;
		if (qName.equalsIgnoreCase(MessageNormaliser.EMAIL_PREFIX + ":" + MessageNormaliser.PART_TAG))
		{
			partLevel++;
			
			// Check for the filename attribute, which indicates an attachment
			String filenameValue = atts.getValue(MessageNormaliser.EMAIL_URI, MessageNormaliser.FILENAME_ATTRIBUTE);
			if (filenameValue != null)
			{
				if (topLevelAttachmentIndex == -1)
				{
					// This is an attachment at the top level of the XML
					topLevelAttachmentIndex = partLevel;
					currentAttachmentName = filenameValue;
					elementIsTopLevelAttachment = true;
				
			        // Create a transformHandler so we can write out a temporary file for this attachment
			        attachmentXMLWriter = null;
			        SAXTransformerFactory transformFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			        try 
			        {
			        	attachmentXMLWriter = transformFactory.newTransformerHandler();
			        } 
			        catch (TransformerConfigurationException e) 
			        {
			            throw new SAXException("Unable to create transformerHandler due to transformer configuration exception.");
			        }
	
			        try
			        {
			        	// Create a temporary file from which we can export. I can't come up with a better way to do this unfortunately...
				        tempAttachmentFile = File.createTempFile("email-attachment", ".tmp");
				        attachmentOutputStream = new FileOutputStream(tempAttachmentFile);
				        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(attachmentOutputStream);
				        StreamResult tempFileResult = new StreamResult(attachmentOutputStream);
				        tempFileResult.setWriter(outputStreamWriter);
				        		        
				        attachmentXMLWriter.setResult(tempFileResult);
				        attachmentXMLWriter.startDocument();
			        }
			        catch (IOException iex)
			        {
			        	throw new SAXException("Problem creating temporary output.", iex);
			        }
			        
				}
			}
		}
		
		// Check to which output file we should output this element
		if (topLevelAttachmentIndex == -1)	
		{
			// We are not in the top-level attachment, so write out to root output file
			rootXMLWriter.startElement(namespaceURI, localName, qName, atts);
		}
		else
		{
			// We are in the top-level attachment, but if this is the start of the element we don't want to write it to the attachment output
			if (elementIsTopLevelAttachment)
			{
				rootXMLWriter.startElement(namespaceURI, localName, qName, atts);
			}
			else
			{
				attachmentXMLWriter.startElement(namespaceURI, localName, qName, atts);
			}
		}
				
	}

}
