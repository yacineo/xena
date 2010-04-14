/*
 * file:       GanttBarStartAndEndShapeType.java
 * author:     Tom Ollar
 * copyright:  (c) Packwood Software 2009
 * date:       26/03/2009
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj.mpp;

import net.sf.mpxj.utility.MpxjEnum;
import net.sf.mpxj.utility.NumberUtility;

import java.util.EnumSet;

/**
 * Represents the shape at the start end end of a Gantt bar.
 */
public enum GanttBarStartAndEndShapeType implements MpxjEnum
{
   NONE(0, "None"),
   NORTHHOMEPLATE(1, "North Home Plate"),
   SOUTHHOMEPLATE(2, "South Home Plate"),
   DIAMOND(3, "Diamond"),
   UPARROW(4, "Up Arrow"),
   DOWNARROW(5, "Down Arrow"),
   RIGHTARROW(6, "Right Arrow"),
   LEFTARROW(7, "Left Arrow"),
   UPPOINTER(8, "Up Pointer"),
   SOUTHMINIHOMEPLATE(9, "South Mini Home Plate"),
   NORTHMINIHOMEPLATE(10, "North Mini Home Plate"),
   VERTICALBAR(11, "Vertical Bar"),
   SQUARE(12, "Square"),
   DIAMONDCIRCLED(13, "Diamond Circled"),
   DOWNPOINTER(14, "Down Pointer"),
   UPARROWCIRCLED(15, "Up Arrow Circled"),
   DOWNARROWCIRCLED(1, "Down Arrow Circled"),
   UPPOINTERCIRCLED(17, "Up Pointer Circled"),
   DOWNPOINTERCIRCLED(18, "Down Pointer Circled"),
   CIRCLE(19, "Circle"),
   STAR(20, "Star");

   /**
    * Private constructor.
    *
    * @param type int version of the enum
    * @param name name of the enum
    */
   private GanttBarStartAndEndShapeType(int type, String name)
   {
      m_value = type;
      m_name = name;
   }

   /**
    * Retrieve an instance of the enum based on its int value.
    *
    * @param type int type
    * @return enum instance
    */
   public static GanttBarStartAndEndShapeType getInstance(int type)
   {
      if (type < 0 || type >= TYPE_VALUES.length)
      {
         type = NONE.getValue();
      }
      return (TYPE_VALUES[type]);
   }

   /**
    * Retrieve an instance of the enum based on its int value.
    *
    * @param type int type
    * @return enum instance
    */
   public static GanttBarStartAndEndShapeType getInstance(Number type)
   {
      int value;
      if (type == null)
      {
         value = -1;
      }
      else
      {
         value = NumberUtility.getInt(type);
      }
      return (getInstance(value));
   }

   /**
    * Accessor method used to retrieve the numeric representation of the enum.
    *
    * @return int representation of the enum
    */
   public int getValue()
   {
      return (m_value);
   }

   /**
    * Retrieve the line style name. Currently this is not localised.
    *
    * @return style name
    */
   public String getName()
   {
      return (m_name);
   }

   /**
    * Retrieve the String representation of this line style.
    *
    * @return String representation of this line style
    */
   @Override public String toString()
   {
      return (getName());
   }

   /**
    * Array mapping int types to enums.
    */
   private static final GanttBarStartAndEndShapeType[] TYPE_VALUES = new GanttBarStartAndEndShapeType[21];
   static
   {
      for (GanttBarStartAndEndShapeType e : EnumSet.range(GanttBarStartAndEndShapeType.NONE, GanttBarStartAndEndShapeType.STAR))
      {
         TYPE_VALUES[e.getValue()] = e;
      }
   }

   /**
    * Internal representation of the enum int type.
    */
   private int m_value;
   private String m_name;
}
