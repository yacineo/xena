//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.4-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.03.24 at 12:10:52 AM GMT 
//

package net.sf.mpxj.mspdi.schema;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import net.sf.mpxj.WorkContour;

@SuppressWarnings("all") public class Adapter9 extends XmlAdapter<String, WorkContour>
{

   public WorkContour unmarshal(String value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.parseWorkContour(value));
   }

   public String marshal(WorkContour value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.printWorkContour(value));
   }

}
