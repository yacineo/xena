//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.4-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.03.24 at 12:10:52 AM GMT 
//

package net.sf.mpxj.mspdi.schema;

import javax.xml.bind.annotation.adapters.XmlAdapter;

@SuppressWarnings("all") public class Adapter5 extends XmlAdapter<String, String>
{

   public String unmarshal(String value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.parseString(value));
   }

   public String marshal(String value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.printString(value));
   }

}
