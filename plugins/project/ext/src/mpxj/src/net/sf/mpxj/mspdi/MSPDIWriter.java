/*
 * file:       MSPDIWriter.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2005
 * date:       2005-12-30
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

package net.sf.mpxj.mspdi;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import net.sf.mpxj.AccrueType;
import net.sf.mpxj.DateRange;
import net.sf.mpxj.Day;
import net.sf.mpxj.Duration;
import net.sf.mpxj.ExtendedAttributeResourceFields;
import net.sf.mpxj.ExtendedAttributeTaskFields;
import net.sf.mpxj.MPPResourceField;
import net.sf.mpxj.MPPTaskField;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectCalendarHours;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ProjectHeader;
import net.sf.mpxj.Relation;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.ResourceField;
import net.sf.mpxj.ScheduleFrom;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskField;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.TimephasedResourceAssignment;
import net.sf.mpxj.mspdi.schema.ObjectFactory;
import net.sf.mpxj.mspdi.schema.Project;
import net.sf.mpxj.mspdi.schema.TimephasedDataType;
import net.sf.mpxj.utility.DateUtility;
import net.sf.mpxj.utility.NumberUtility;
import net.sf.mpxj.writer.AbstractProjectWriter;

/**
 * This class creates a new MSPDI file from the contents of an ProjectFile instance.
 */
public final class MSPDIWriter extends AbstractProjectWriter
{
   /**
    * {@inheritDoc}
    */
   public void write(ProjectFile projectFile, OutputStream stream) throws IOException
   {
      try
      {
         m_projectFile = projectFile;

         JAXBContext context = JAXBContext.newInstance("net.sf.mpxj.mspdi.schema");
         Marshaller marshaller = context.createMarshaller();
         marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

         m_factory = new ObjectFactory();
         Project project = m_factory.createProject();

         writeProjectHeader(project);
         writeProjectExtendedAttributes(project);
         writeCalendars(project);
         writeResources(project);
         writeTasks(project);
         writeAssignments(project);

         DatatypeConverter.setParentFile(m_projectFile);
         marshaller.marshal(project, stream);
      }

      catch (JAXBException ex)
      {
         throw new IOException(ex.toString());
      }

      finally
      {
         m_projectFile = null;
         m_factory = null;
      }
   }

   /**
    * This method writes project header data to an MSPDI file.
    *
    * @param project Root node of the MSPDI file
    */
   private void writeProjectHeader(Project project)
   {
      ProjectHeader header = m_projectFile.getProjectHeader();

      project.setActualsInSync(Boolean.valueOf(header.getActualsInSync()));
      project.setAdminProject(Boolean.valueOf(header.getAdminProject()));
      project.setAuthor(header.getAuthor());
      project.setAutoAddNewResourcesAndTasks(Boolean.valueOf(header.getAutoAddNewResourcesAndTasks()));
      project.setAutolink(Boolean.valueOf(header.getAutolink()));
      project.setBaselineForEarnedValue(NumberUtility.getBigInteger(header.getBaselineForEarnedValue()));
      project.setCalendarUID(BigInteger.ONE);
      project.setCategory(header.getCategory());
      project.setCompany(header.getCompany());
      project.setCreationDate(DatatypeConverter.printDate(header.getCreationDate()));
      project.setCriticalSlackLimit(NumberUtility.getBigInteger(header.getCriticalSlackLimit()));
      project.setCurrencyCode(header.getCurrencyCode());
      project.setCurrencyDigits(BigInteger.valueOf(header.getCurrencyDigits().intValue()));
      project.setCurrencySymbol(header.getCurrencySymbol());
      project.setCurrencySymbolPosition(header.getSymbolPosition());
      project.setCurrentDate(DatatypeConverter.printDate(header.getCurrentDate()));
      project.setDaysPerMonth(NumberUtility.getBigInteger(header.getDaysPerMonth()));
      project.setDefaultFinishTime(DatatypeConverter.printTime(header.getDefaultEndTime()));
      project.setDefaultFixedCostAccrual(header.getDefaultFixedCostAccrual());
      project.setDefaultOvertimeRate(DatatypeConverter.printRate(header.getDefaultOvertimeRate()));
      project.setDefaultStandardRate(DatatypeConverter.printRate(header.getDefaultStandardRate()));
      project.setDefaultStartTime(DatatypeConverter.printTime(header.getDefaultStartTime()));
      project.setDefaultTaskEVMethod(DatatypeConverter.printEarnedValueMethod(header.getDefaultTaskEarnedValueMethod()));
      project.setDefaultTaskType(header.getDefaultTaskType());
      project.setDurationFormat(DatatypeConverter.printDurationTimeUnits(header.getDefaultDurationUnits()));
      project.setEarnedValueMethod(DatatypeConverter.printEarnedValueMethod(header.getEarnedValueMethod()));
      project.setEditableActualCosts(Boolean.valueOf(header.getEditableActualCosts()));
      project.setExtendedCreationDate(DatatypeConverter.printDate(header.getExtendedCreationDate()));
      project.setFinishDate(DatatypeConverter.printDate(header.getFinishDate()));
      project.setFiscalYearStart(Boolean.valueOf(header.getFiscalYearStart()));
      project.setFYStartDate(NumberUtility.getBigInteger(header.getFiscalYearStartMonth()));
      project.setHonorConstraints(Boolean.valueOf(header.getHonorConstraints()));
      project.setInsertedProjectsLikeSummary(Boolean.valueOf(header.getInsertedProjectsLikeSummary()));
      project.setLastSaved(DatatypeConverter.printDate(header.getLastSaved()));
      project.setManager(header.getManager());
      project.setMicrosoftProjectServerURL(Boolean.valueOf(header.getMicrosoftProjectServerURL()));
      project.setMinutesPerDay(NumberUtility.getBigInteger(header.getMinutesPerDay()));
      project.setMinutesPerWeek(NumberUtility.getBigInteger(header.getMinutesPerWeek()));
      project.setMoveCompletedEndsBack(Boolean.valueOf(header.getMoveCompletedEndsBack()));
      project.setMoveCompletedEndsForward(Boolean.valueOf(header.getMoveCompletedEndsForward()));
      project.setMoveRemainingStartsBack(Boolean.valueOf(header.getMoveRemainingStartsBack()));
      project.setMoveRemainingStartsForward(Boolean.valueOf(header.getMoveRemainingStartsForward()));
      project.setMultipleCriticalPaths(Boolean.valueOf(header.getMultipleCriticalPaths()));
      project.setName(header.getName());
      project.setNewTasksEffortDriven(Boolean.valueOf(header.getNewTasksEffortDriven()));
      project.setNewTasksEstimated(Boolean.valueOf(header.getNewTasksEstimated()));
      project.setNewTaskStartDate(header.getNewTaskStartIsProjectStart() == true ? BigInteger.ZERO : BigInteger.ONE);
      project.setProjectExternallyEdited(Boolean.valueOf(header.getProjectExternallyEdited()));
      project.setRemoveFileProperties(Boolean.valueOf(header.getRemoveFileProperties()));
      project.setRevision(NumberUtility.getBigInteger(header.getRevision()));
      project.setScheduleFromStart(Boolean.valueOf(header.getScheduleFrom() == ScheduleFrom.START));
      project.setSplitsInProgressTasks(Boolean.valueOf(header.getSplitInProgressTasks()));
      project.setSpreadActualCost(Boolean.valueOf(header.getSpreadActualCost()));
      project.setSpreadPercentComplete(Boolean.valueOf(header.getSpreadPercentComplete()));
      project.setStartDate(DatatypeConverter.printDate(header.getStartDate()));
      project.setStatusDate(DatatypeConverter.printDate(header.getStatusDate()));
      project.setSubject(header.getSubject());
      project.setTaskUpdatesResource(Boolean.valueOf(header.getUpdatingTaskStatusUpdatesResourceStatus()));
      project.setTitle(header.getProjectTitle());
      project.setUID(header.getUniqueID());
      project.setWeekStartDay(DatatypeConverter.printDay(header.getWeekStartDay()));
      project.setWorkFormat(DatatypeConverter.printWorkUnits(header.getDefaultWorkUnits()));
   }

   /**
    * This method writes project extended attribute data into an MSPDI file.
    *
    * @param project Root node of the MSPDI file
    */
   private void writeProjectExtendedAttributes(Project project)
   {
      Project.ExtendedAttributes attributes = m_factory.createProjectExtendedAttributes();
      project.setExtendedAttributes(attributes);
      List<Project.ExtendedAttributes.ExtendedAttribute> list = attributes.getExtendedAttribute();

      writeTaskFieldAliases(list);
      writeResourceFieldAliases(list);
   }

   /**
    * Writes field aliases.
    * 
    * @param list field alias list
    */
   private void writeTaskFieldAliases(List<Project.ExtendedAttributes.ExtendedAttribute> list)
   {
      Map<TaskField, String> fieldAliasMap = m_projectFile.getTaskFieldAliasMap();

      for (int loop = 0; loop < ExtendedAttributeTaskFields.FIELD_ARRAY.length; loop++)
      {
         TaskField key = ExtendedAttributeTaskFields.FIELD_ARRAY[loop];
         Integer fieldID = Integer.valueOf(MPPTaskField.getID(key) | MPPTaskField.TASK_FIELD_BASE);
         String name = key.getName();
         String alias = fieldAliasMap.get(key);

         Project.ExtendedAttributes.ExtendedAttribute attribute = m_factory.createProjectExtendedAttributesExtendedAttribute();
         list.add(attribute);
         attribute.setFieldID(fieldID.toString());
         attribute.setFieldName(name);
         attribute.setAlias(alias);
      }
   }

   /**
    * Writes field aliases.
    * 
    * @param list field alias list
    */
   private void writeResourceFieldAliases(List<Project.ExtendedAttributes.ExtendedAttribute> list)
   {
      Map<ResourceField, String> fieldAliasMap = m_projectFile.getResourceFieldAliasMap();

      for (int loop = 0; loop < ExtendedAttributeResourceFields.FIELD_ARRAY.length; loop++)
      {
         ResourceField key = ExtendedAttributeResourceFields.FIELD_ARRAY[loop];
         Integer fieldID = Integer.valueOf(MPPResourceField.getID(key) | MPPResourceField.RESOURCE_FIELD_BASE);
         String name = key.getName();
         String alias = fieldAliasMap.get(key);

         Project.ExtendedAttributes.ExtendedAttribute attribute = m_factory.createProjectExtendedAttributesExtendedAttribute();
         list.add(attribute);
         attribute.setFieldID(fieldID.toString());
         attribute.setFieldName(name);
         attribute.setAlias(alias);
      }
   }

   /**
    * This method writes calendar data to an MSPDI file.
    *
    * @param project Root node of the MSPDI file
    */
   private void writeCalendars(Project project)
   {
      //
      // First step, find all of the base calendars and resource calendars,
      // add them to a list ready for processing, and create a map between
      // names and unique IDs
      //
      LinkedList<ProjectCalendar> calendarList = new LinkedList<ProjectCalendar>(m_projectFile.getBaseCalendars());

      for (Resource resource : m_projectFile.getAllResources())
      {
         ProjectCalendar cal = resource.getResourceCalendar();
         if (cal != null)
         {
            calendarList.add(cal);
         }
      }

      //
      // Create the new MSPDI calendar list
      //
      Project.Calendars calendars = m_factory.createProjectCalendars();
      project.setCalendars(calendars);
      List<Project.Calendars.Calendar> calendar = calendars.getCalendar();

      //
      // Process each calendar in turn
      //
      for (ProjectCalendar cal : calendarList)
      {
         calendar.add(writeCalendar(cal));
      }
   }

   /**
    * This method writes data for a single calendar to an MSPDI file.
    *
    * @param bc Base calendar data
    * @return New MSPDI calendar instance
    */
   private Project.Calendars.Calendar writeCalendar(ProjectCalendar bc)
   {
      //
      // Create a calendar
      //
      Project.Calendars.Calendar calendar = m_factory.createProjectCalendarsCalendar();
      calendar.setUID(NumberUtility.getBigInteger(bc.getUniqueID()));
      calendar.setIsBaseCalendar(Boolean.valueOf(bc.isBaseCalendar()));

      if (bc.isBaseCalendar() == false)
      {
         ProjectCalendar base = bc.getBaseCalendar();
         if (base != null)
         {
            calendar.setBaseCalendarUID(NumberUtility.getBigInteger(base.getUniqueID()));
         }
      }

      calendar.setName(bc.getName());

      //
      // Create a list of normal days
      //
      Project.Calendars.Calendar.WeekDays days = m_factory.createProjectCalendarsCalendarWeekDays();
      Project.Calendars.Calendar.WeekDays.WeekDay.WorkingTimes.WorkingTime time;
      ProjectCalendarHours bch;

      List<Project.Calendars.Calendar.WeekDays.WeekDay> dayList = days.getWeekDay();

      for (int loop = 1; loop < 8; loop++)
      {
         int workingFlag = bc.getWorkingDay(Day.getInstance(loop));

         if (workingFlag != ProjectCalendar.DEFAULT)
         {
            Project.Calendars.Calendar.WeekDays.WeekDay day = m_factory.createProjectCalendarsCalendarWeekDaysWeekDay();
            dayList.add(day);
            day.setDayType(BigInteger.valueOf(loop));
            day.setDayWorking(Boolean.valueOf(workingFlag == ProjectCalendar.WORKING));

            if (workingFlag == ProjectCalendar.WORKING)
            {
               Project.Calendars.Calendar.WeekDays.WeekDay.WorkingTimes times = m_factory.createProjectCalendarsCalendarWeekDaysWeekDayWorkingTimes();
               day.setWorkingTimes(times);
               List<Project.Calendars.Calendar.WeekDays.WeekDay.WorkingTimes.WorkingTime> timesList = times.getWorkingTime();

               bch = bc.getCalendarHours(Day.getInstance(loop));
               if (bch != null)
               {
                  for (DateRange range : bch)
                  {
                     if (range != null)
                     {
                        time = m_factory.createProjectCalendarsCalendarWeekDaysWeekDayWorkingTimesWorkingTime();
                        timesList.add(time);

                        time.setFromTime(DatatypeConverter.printTime(range.getStart()));
                        time.setToTime(DatatypeConverter.printTime(range.getEnd()));
                     }
                  }
               }
            }
         }
      }

      //
      // Create a list of exceptions
      //
      // A quirk of MS Project is that these exceptions must be
      // in date order in the file, otherwise they are ignored
      //
      List<ProjectCalendarException> exceptions = new ArrayList<ProjectCalendarException>(bc.getCalendarExceptions());
      Collections.sort(exceptions);

      for (ProjectCalendarException exception : exceptions)
      {
         boolean working = exception.getWorking();

         Project.Calendars.Calendar.WeekDays.WeekDay day = m_factory.createProjectCalendarsCalendarWeekDaysWeekDay();
         dayList.add(day);
         day.setDayType(BIGINTEGER_ZERO);
         day.setDayWorking(Boolean.valueOf(working));

         Project.Calendars.Calendar.WeekDays.WeekDay.TimePeriod period = m_factory.createProjectCalendarsCalendarWeekDaysWeekDayTimePeriod();
         day.setTimePeriod(period);
         period.setFromDate(DatatypeConverter.printDate(exception.getFromDate()));
         period.setToDate(DatatypeConverter.printDate(exception.getToDate()));

         if (working == true)
         {
            Project.Calendars.Calendar.WeekDays.WeekDay.WorkingTimes times = m_factory.createProjectCalendarsCalendarWeekDaysWeekDayWorkingTimes();
            day.setWorkingTimes(times);
            List<Project.Calendars.Calendar.WeekDays.WeekDay.WorkingTimes.WorkingTime> timesList = times.getWorkingTime();

            for (DateRange range : exception)
            {
               time = m_factory.createProjectCalendarsCalendarWeekDaysWeekDayWorkingTimesWorkingTime();
               timesList.add(time);

               time.setFromTime(DatatypeConverter.printTime(range.getStart()));
               time.setToTime(DatatypeConverter.printTime(range.getEnd()));
            }
         }
      }

      //
      // Do not add a weekdays tag to the calendar unless it
      // has valid entries.
      // Fixes SourceForge bug 1854747: MPXJ and MSP 2007 XML formats
      //
      if (!dayList.isEmpty())
      {
         calendar.setWeekDays(days);
      }

      return (calendar);
   }

   /**
    * This method writes resource data to an MSPDI file.
    *
    * @param project Root node of the MSPDI file
    */
   private void writeResources(Project project)
   {
      Project.Resources resources = m_factory.createProjectResources();
      project.setResources(resources);
      List<Project.Resources.Resource> list = resources.getResource();

      for (Resource resource : m_projectFile.getAllResources())
      {
         list.add(writeResource(resource));
      }
   }

   /**
    * This method writes data for a single resource to an MSPDI file.
    *
    * @param mpx Resource data
    * @return New MSPDI resource instance
    */
   private Project.Resources.Resource writeResource(Resource mpx)
   {
      Project.Resources.Resource xml = m_factory.createProjectResourcesResource();
      ProjectCalendar cal = mpx.getResourceCalendar();
      if (cal != null)
      {
         xml.setCalendarUID(NumberUtility.getBigInteger(cal.getUniqueID()));
      }

      xml.setAccrueAt(mpx.getAccrueAt());
      xml.setActiveDirectoryGUID(mpx.getActiveDirectoryGUID());
      xml.setActualCost(DatatypeConverter.printCurrency(mpx.getActualCost()));
      xml.setActualOvertimeCost(DatatypeConverter.printCurrency(mpx.getActualOvertimeCost()));
      xml.setActualOvertimeWork(DatatypeConverter.printDuration(this, mpx.getActualOvertimeWork()));
      xml.setActualOvertimeWorkProtected(DatatypeConverter.printDuration(this, mpx.getActualOvertimeWorkProtected()));
      xml.setActualWork(DatatypeConverter.printDuration(this, mpx.getActualWork()));
      xml.setActualWorkProtected(DatatypeConverter.printDuration(this, mpx.getActualWorkProtected()));
      xml.setACWP(DatatypeConverter.printCurrency(mpx.getACWP()));
      xml.setAvailableFrom(DatatypeConverter.printDate(mpx.getAvailableFrom()));
      xml.setAvailableTo(DatatypeConverter.printDate(mpx.getAvailableTo()));
      xml.setBCWS(DatatypeConverter.printCurrency(mpx.getBCWS()));
      xml.setBCWP(DatatypeConverter.printCurrency(mpx.getBCWP()));
      xml.setBookingType(mpx.getBookingType());
      xml.setIsBudget(Boolean.valueOf(mpx.getBudget()));
      xml.setCanLevel(Boolean.valueOf(mpx.getCanLevel()));
      xml.setCode(mpx.getCode());
      xml.setCost(DatatypeConverter.printCurrency(mpx.getCost()));
      xml.setCostPerUse(DatatypeConverter.printCurrency(mpx.getCostPerUse()));
      xml.setCostVariance(DatatypeConverter.printCurrency(mpx.getCostVariance()));
      xml.setCreationDate(DatatypeConverter.printDate(mpx.getCreationDate()));
      xml.setCV(DatatypeConverter.printCurrency(mpx.getCV()));
      xml.setEmailAddress(mpx.getEmailAddress());
      xml.setFinish(DatatypeConverter.printDate(mpx.getFinish()));
      xml.setGroup(mpx.getGroup());
      xml.setHyperlink(mpx.getHyperlink());
      xml.setHyperlinkAddress(mpx.getHyperlinkAddress());
      xml.setHyperlinkSubAddress(mpx.getHyperlinkSubAddress());
      xml.setID(NumberUtility.getBigInteger(mpx.getID()));
      xml.setInitials(mpx.getInitials());
      xml.setIsEnterprise(Boolean.valueOf(mpx.getEnterprise()));
      xml.setIsGeneric(Boolean.valueOf(mpx.getGeneric()));
      xml.setIsInactive(Boolean.valueOf(mpx.getInactive()));
      xml.setIsNull(Boolean.valueOf(mpx.getNull()));
      xml.setMaterialLabel(mpx.getMaterialLabel());
      xml.setMaxUnits(DatatypeConverter.printUnits(mpx.getMaxUnits()));
      xml.setName(mpx.getName());
      xml.setNotes(mpx.getNotes());
      xml.setNTAccount(mpx.getNtAccount());
      xml.setOverAllocated(Boolean.valueOf(mpx.getOverAllocated()));
      xml.setOvertimeCost(DatatypeConverter.printCurrency(mpx.getOvertimeCost()));
      xml.setOvertimeRate(DatatypeConverter.printRate(mpx.getOvertimeRate()));
      xml.setOvertimeRateFormat(DatatypeConverter.printTimeUnit(mpx.getOvertimeRateFormat()));
      xml.setOvertimeWork(DatatypeConverter.printDuration(this, mpx.getOvertimeWork()));
      xml.setPeakUnits(DatatypeConverter.printUnits(mpx.getPeakUnits()));
      xml.setPercentWorkComplete(NumberUtility.getBigInteger(mpx.getPercentWorkComplete()));
      xml.setPhonetics(mpx.getPhonetics());
      xml.setRegularWork(DatatypeConverter.printDuration(this, mpx.getRegularWork()));
      xml.setRemainingCost(DatatypeConverter.printCurrency(mpx.getRemainingCost()));
      xml.setRemainingOvertimeCost(DatatypeConverter.printCurrency(mpx.getRemainingOvertimeCost()));
      xml.setRemainingOvertimeWork(DatatypeConverter.printDuration(this, mpx.getRemainingOvertimeWork()));
      xml.setRemainingWork(DatatypeConverter.printDuration(this, mpx.getRemainingWork()));
      xml.setStandardRate(DatatypeConverter.printRate(mpx.getStandardRate()));
      xml.setStandardRateFormat(DatatypeConverter.printTimeUnit(mpx.getStandardRateFormat()));
      xml.setStart(DatatypeConverter.printDate(mpx.getStart()));
      xml.setSV(DatatypeConverter.printCurrency(mpx.getSV()));
      xml.setType(mpx.getType());
      xml.setUID(mpx.getUniqueID());
      xml.setWork(DatatypeConverter.printDuration(this, mpx.getWork()));
      xml.setWorkGroup(mpx.getWorkGroup());
      xml.setWorkVariance(new BigDecimal(DatatypeConverter.printDurationInMinutes(mpx.getWorkVariance()) * 1000));

      writeResourceExtendedAttributes(xml, mpx);

      writeResourceBaselines(xml, mpx);

      return (xml);
   }

   /**
    * Writes resource baseline data.
    * 
    * @param xmlResource MSPDI resource
    * @param mpxjResource MPXJ resource
    */
   private void writeResourceBaselines(Project.Resources.Resource xmlResource, Resource mpxjResource)
   {
      Project.Resources.Resource.Baseline baseline = m_factory.createProjectResourcesResourceBaseline();
      xmlResource.getBaseline().add(baseline);

      baseline.setNumber(BigInteger.ZERO);
      baseline.setCost(DatatypeConverter.printCurrency(mpxjResource.getBaselineCost()));
      baseline.setWork(DatatypeConverter.printDuration(this, mpxjResource.getBaselineWork()));

      for (int loop = 1; loop <= 10; loop++)
      {
         baseline = m_factory.createProjectResourcesResourceBaseline();
         xmlResource.getBaseline().add(baseline);

         baseline.setNumber(BigInteger.valueOf(loop));
         baseline.setCost(DatatypeConverter.printCurrency(mpxjResource.getBaselineCost(loop)));
         baseline.setWork(DatatypeConverter.printDuration(this, mpxjResource.getBaselineWork(loop)));
      }
   }

   /**
    * This method writes extended attribute data for a resource.
    *
    * @param xml MSPDI resource
    * @param mpx MPXJ resource
    */
   private void writeResourceExtendedAttributes(Project.Resources.Resource xml, Resource mpx)
   {
      Project.Resources.Resource.ExtendedAttribute attrib;
      List<Project.Resources.Resource.ExtendedAttribute> extendedAttributes = xml.getExtendedAttribute();
      Object value;
      ResourceField mpxFieldID;
      Integer xmlFieldID;

      for (int loop = 0; loop < ExtendedAttributeResourceFields.FIELD_ARRAY.length; loop++)
      {
         mpxFieldID = ExtendedAttributeResourceFields.FIELD_ARRAY[loop];
         value = mpx.getCachedValue(mpxFieldID);

         if (value != null)
         {
            xmlFieldID = Integer.valueOf(MPPResourceField.getID(mpxFieldID) | MPPResourceField.RESOURCE_FIELD_BASE);

            attrib = m_factory.createProjectResourcesResourceExtendedAttribute();
            extendedAttributes.add(attrib);
            attrib.setFieldID(xmlFieldID.toString());
            attrib.setValue(DatatypeConverter.printExtendedAttribute(this, value, mpxFieldID.getDataType()));
            attrib.setDurationFormat(printExtendedAttributeDurationFormat(value));
         }
      }
   }

   /**
    * This method writes task data to an MSPDI file.
    *
    * @param project Root node of the MSPDI file
    */
   private void writeTasks(Project project)
   {
      Project.Tasks tasks = m_factory.createProjectTasks();
      project.setTasks(tasks);
      List<Project.Tasks.Task> list = tasks.getTask();

      for (Task task : m_projectFile.getAllTasks())
      {
         list.add(writeTask(task));
      }
   }

   /**
    * This method writes data for a single task to an MSPDI file.
    *
    * @param mpx Task data
    * @return new task instance
    */
   private Project.Tasks.Task writeTask(Task mpx)
   {
      Project.Tasks.Task xml = m_factory.createProjectTasksTask();

      xml.setActualCost(DatatypeConverter.printCurrency(mpx.getActualCost()));
      xml.setActualDuration(DatatypeConverter.printDuration(this, mpx.getActualDuration()));
      xml.setActualFinish(DatatypeConverter.printDate(mpx.getActualFinish()));
      xml.setActualOvertimeCost(DatatypeConverter.printCurrency(mpx.getActualOvertimeCost()));
      xml.setActualOvertimeWork(DatatypeConverter.printDuration(this, mpx.getActualOvertimeWork()));
      xml.setActualOvertimeWorkProtected(DatatypeConverter.printDuration(this, mpx.getActualOvertimeWorkProtected()));
      xml.setActualStart(DatatypeConverter.printDate(mpx.getActualStart()));
      xml.setActualWork(DatatypeConverter.printDuration(this, mpx.getActualWork()));
      xml.setActualWorkProtected(DatatypeConverter.printDuration(this, mpx.getActualWorkProtected()));
      xml.setACWP(DatatypeConverter.printCurrency(mpx.getACWP()));
      xml.setBCWP(DatatypeConverter.printCurrency(mpx.getBCWP()));
      xml.setBCWS(DatatypeConverter.printCurrency(mpx.getBCWS()));
      xml.setCalendarUID(getTaskCalendarID(mpx));
      xml.setConstraintDate(DatatypeConverter.printDate(mpx.getConstraintDate()));
      xml.setConstraintType(DatatypeConverter.printConstraintType(mpx.getConstraintType()));
      xml.setContact(mpx.getContact());
      xml.setCost(DatatypeConverter.printCurrency(mpx.getCost()));
      xml.setCreateDate(DatatypeConverter.printDate(mpx.getCreateDate()));
      xml.setCritical(Boolean.valueOf(mpx.getCritical()));
      xml.setCV(DatatypeConverter.printCurrency(mpx.getCV()));
      xml.setDeadline(DatatypeConverter.printDate(mpx.getDeadline()));
      xml.setDuration(DatatypeConverter.printDuration(this, mpx.getDuration()));
      xml.setDurationFormat(DatatypeConverter.printDurationTimeUnits(mpx.getDurationFormat()));
      xml.setDurationFormat(DatatypeConverter.printDurationTimeUnits(mpx.getDuration()));
      xml.setEarlyFinish(DatatypeConverter.printDate(mpx.getEarlyFinish()));
      xml.setEarlyStart(DatatypeConverter.printDate(mpx.getEarlyStart()));
      xml.setEarnedValueMethod(DatatypeConverter.printEarnedValueMethod(mpx.getEarnedValueMethod()));
      xml.setEffortDriven(Boolean.valueOf(mpx.getEffortDriven()));
      xml.setEstimated(Boolean.valueOf(mpx.getEstimated()));
      xml.setExternalTask(Boolean.valueOf(mpx.getExternalTask()));
      xml.setExternalTaskProject(mpx.getProject());

      Date finishDate = mpx.getFinish();
      if (finishDate != null)
      {
         xml.setFinish(DatatypeConverter.printDate(finishDate));
      }

      xml.setFinishVariance(BigInteger.valueOf((long) DatatypeConverter.printDurationInMinutes(mpx.getFinishVariance()) * 1000));
      xml.setFixedCost(DatatypeConverter.printCurrency(mpx.getFixedCost()));

      AccrueType fixedCostAccrual = mpx.getFixedCostAccrual();
      if (fixedCostAccrual == null)
      {
         fixedCostAccrual = AccrueType.PRORATED;
      }
      xml.setFixedCostAccrual(fixedCostAccrual);

      // This is not correct
      //xml.setFreeSlack(BigInteger.valueOf((long)DatatypeConverter.printDurationInMinutes(mpx.getFreeSlack())*1000));
      xml.setFreeSlack(BIGINTEGER_ZERO);
      xml.setHideBar(Boolean.valueOf(mpx.getHideBar()));
      xml.setIsNull(Boolean.valueOf(mpx.getNull()));
      xml.setIsSubproject(Boolean.valueOf(mpx.getSubProject() != null));
      xml.setIsSubprojectReadOnly(Boolean.valueOf(mpx.getSubprojectReadOnly()));
      xml.setHyperlink(mpx.getHyperlink());
      xml.setHyperlinkAddress(mpx.getHyperlinkAddress());
      xml.setHyperlinkSubAddress(mpx.getHyperlinkSubAddress());
      xml.setID(NumberUtility.getBigInteger(mpx.getID()));
      xml.setIgnoreResourceCalendar(Boolean.valueOf(mpx.getIgnoreResourceCalendar()));
      xml.setLateFinish(DatatypeConverter.printDate(mpx.getLateFinish()));
      xml.setLateStart(DatatypeConverter.printDate(mpx.getLateStart()));
      xml.setLevelAssignments(Boolean.valueOf(mpx.getLevelAssignments()));
      xml.setLevelingCanSplit(Boolean.valueOf(mpx.getLevelingCanSplit()));

      if (mpx.getLevelingDelay() != null)
      {
         xml.setLevelingDelay(BigInteger.valueOf((long) mpx.getLevelingDelay().getDuration()));
         xml.setLevelingDelayFormat(DatatypeConverter.printDurationTimeUnits(mpx.getLevelingDelayFormat()));
      }

      xml.setMilestone(Boolean.valueOf(mpx.getMilestone()));
      xml.setName(mpx.getName());
      xml.setNotes(mpx.getNotes());
      xml.setOutlineLevel(NumberUtility.getBigInteger(mpx.getOutlineLevel()));
      xml.setOutlineNumber(mpx.getOutlineNumber());
      xml.setOverAllocated(Boolean.valueOf(mpx.getOverAllocated()));
      xml.setOvertimeCost(DatatypeConverter.printCurrency(mpx.getOvertimeCost()));
      xml.setOvertimeWork(DatatypeConverter.printDuration(this, mpx.getOvertimeWork()));
      xml.setPercentComplete(NumberUtility.getBigInteger(mpx.getPercentageComplete()));
      xml.setPercentWorkComplete(NumberUtility.getBigInteger(mpx.getPercentageWorkComplete()));
      xml.setPhysicalPercentComplete(NumberUtility.getBigInteger(mpx.getPhysicalPercentComplete()));
      xml.setPriority(DatatypeConverter.printPriority(mpx.getPriority()));
      xml.setRecurring(Boolean.valueOf(mpx.getRecurring()));
      xml.setRegularWork(DatatypeConverter.printDuration(this, mpx.getRegularWork()));
      xml.setRemainingCost(DatatypeConverter.printCurrency(mpx.getRemainingCost()));

      if (mpx.getRemainingDuration() == null)
      {
         Duration duration = mpx.getDuration();

         if (duration != null)
         {
            double amount = duration.getDuration();
            amount -= ((amount * NumberUtility.getDouble(mpx.getPercentageComplete())) / 100);
            xml.setRemainingDuration(DatatypeConverter.printDuration(this, Duration.getInstance(amount, duration.getUnits())));
         }
      }
      else
      {
         xml.setRemainingDuration(DatatypeConverter.printDuration(this, mpx.getRemainingDuration()));
      }

      xml.setRemainingOvertimeCost(DatatypeConverter.printCurrency(mpx.getRemainingOvertimeCost()));
      xml.setRemainingOvertimeWork(DatatypeConverter.printDuration(this, mpx.getRemainingOvertimeWork()));
      xml.setRemainingWork(DatatypeConverter.printDuration(this, mpx.getRemainingWork()));
      xml.setResume(DatatypeConverter.printDate(mpx.getResume()));
      xml.setResumeValid(Boolean.valueOf(mpx.getResumeValid()));
      xml.setRollup(Boolean.valueOf(mpx.getRollup()));
      xml.setStart(DatatypeConverter.printDate(mpx.getStart()));
      xml.setStartVariance(BigInteger.valueOf((long) DatatypeConverter.printDurationInMinutes(mpx.getStartVariance()) * 1000));
      xml.setStop(DatatypeConverter.printDate(mpx.getStop()));
      xml.setSubprojectName(mpx.getSubprojectName());
      xml.setSummary(Boolean.valueOf(mpx.getSummary()));
      xml.setTotalSlack(BigInteger.valueOf((long) DatatypeConverter.printDurationInMinutes(mpx.getTotalSlack()) * 1000));
      xml.setType(mpx.getType());
      xml.setUID(mpx.getUniqueID());
      xml.setWBS(mpx.getWBS());
      xml.setWBSLevel(mpx.getWBSLevel());
      xml.setWork(DatatypeConverter.printDuration(this, mpx.getWork()));
      xml.setWorkVariance(new BigDecimal(DatatypeConverter.printDurationInMinutes(mpx.getWorkVariance()) * 1000));

      writePredecessors(xml, mpx);

      writeTaskExtendedAttributes(xml, mpx);

      writeTaskBaselines(xml, mpx);

      return (xml);
   }

   /**
    * Writes task baseline data.
    * 
    * @param xmlTask MSPDI task
    * @param mpxjTask MPXJ task
    */
   private void writeTaskBaselines(Project.Tasks.Task xmlTask, Task mpxjTask)
   {
      Project.Tasks.Task.Baseline baseline = m_factory.createProjectTasksTaskBaseline();
      xmlTask.getBaseline().add(baseline);

      baseline.setNumber(BigInteger.ZERO);
      baseline.setCost(DatatypeConverter.printCurrency(mpxjTask.getBaselineCost()));
      baseline.setDuration(DatatypeConverter.printDuration(this, mpxjTask.getBaselineDuration()));
      baseline.setDurationFormat(DatatypeConverter.printDurationTimeUnits(mpxjTask.getBaselineDuration()));
      baseline.setFinish(DatatypeConverter.printDate(mpxjTask.getBaselineFinish()));
      baseline.setStart(DatatypeConverter.printDate(mpxjTask.getBaselineStart()));
      baseline.setWork(DatatypeConverter.printDuration(this, mpxjTask.getBaselineWork()));

      for (int loop = 1; loop <= 10; loop++)
      {
         baseline = m_factory.createProjectTasksTaskBaseline();
         xmlTask.getBaseline().add(baseline);

         baseline.setNumber(BigInteger.valueOf(loop));
         baseline.setCost(DatatypeConverter.printCurrency(mpxjTask.getBaselineCost(loop)));
         baseline.setDuration(DatatypeConverter.printDuration(this, mpxjTask.getBaselineDuration(loop)));
         baseline.setDurationFormat(DatatypeConverter.printDurationTimeUnits(mpxjTask.getBaselineDuration(loop)));
         baseline.setFinish(DatatypeConverter.printDate(mpxjTask.getBaselineFinish(loop)));
         baseline.setStart(DatatypeConverter.printDate(mpxjTask.getBaselineStart(loop)));
         baseline.setWork(DatatypeConverter.printDuration(this, mpxjTask.getBaselineWork(loop)));
      }
   }

   /**
    * This method writes extended attribute data for a task.
    *
    * @param xml MSPDI task
    * @param mpx MPXJ task
    */
   private void writeTaskExtendedAttributes(Project.Tasks.Task xml, Task mpx)
   {
      Project.Tasks.Task.ExtendedAttribute attrib;
      List<Project.Tasks.Task.ExtendedAttribute> extendedAttributes = xml.getExtendedAttribute();
      Object value;
      TaskField mpxFieldID;
      Integer xmlFieldID;

      for (int loop = 0; loop < ExtendedAttributeTaskFields.FIELD_ARRAY.length; loop++)
      {
         mpxFieldID = ExtendedAttributeTaskFields.FIELD_ARRAY[loop];
         value = mpx.getCachedValue(mpxFieldID);

         if (value != null)
         {
            xmlFieldID = Integer.valueOf(MPPTaskField.getID(mpxFieldID) | MPPTaskField.TASK_FIELD_BASE);

            attrib = m_factory.createProjectTasksTaskExtendedAttribute();
            extendedAttributes.add(attrib);
            attrib.setFieldID(xmlFieldID.toString());
            attrib.setValue(DatatypeConverter.printExtendedAttribute(this, value, mpxFieldID.getDataType()));
            attrib.setDurationFormat(printExtendedAttributeDurationFormat(value));
         }
      }
   }

   /**
    * Converts a duration to duration time units.
    *
    * @param value duration value
    * @return duration time units
    */
   private BigInteger printExtendedAttributeDurationFormat(Object value)
   {
      BigInteger result = null;
      if (value instanceof Duration)
      {
         result = DatatypeConverter.printDurationTimeUnits(((Duration) value).getUnits());
      }
      return (result);
   }

   /**
    * This method retrieves the UID for a calendar associated with a task.
    *
    * @param mpx MPX Task instance
    * @return calendar UID
    */
   private BigInteger getTaskCalendarID(Task mpx)
   {
      BigInteger result = null;
      ProjectCalendar cal = mpx.getCalendar();
      if (cal != null)
      {
         result = NumberUtility.getBigInteger(cal.getUniqueID());
      }
      else
      {
         result = BigInteger.valueOf(-1);
      }
      return (result);
   }

   /**
    * This method writes predecessor data to an MSPDI file.
    * We have to deal with a slight anomaly in this method that is introduced
    * by the MPX file format. It would be possible for someone to create an
    * MPX file with both the predecessor list and the unique ID predecessor
    * list populated... which means that we must process both and avoid adding
    * duplicate predecessors. Also interesting to note is that MSP98 populates
    * the predecessor list, not the unique ID predecessor list, as you might
    * expect.
    *
    * @param xml MSPDI task data
    * @param mpx MPX task data
    */
   private void writePredecessors(Project.Tasks.Task xml, Task mpx)
   {
      TreeSet<Integer> set = new TreeSet<Integer>();
      List<Project.Tasks.Task.PredecessorLink> list = xml.getPredecessorLink();

      //
      // Process the list of predecessors specified by Unique ID
      //
      List<Relation> predecessors = mpx.getUniqueIDPredecessors();
      if (predecessors != null)
      {
         for (Relation rel : predecessors)
         {
            Integer taskUniqueID = rel.getTaskUniqueID();
            set.add(taskUniqueID);
            list.add(writePredecessor(taskUniqueID, rel.getType(), rel.getDuration()));
         }
      }

      //
      // Process the list of predecessors specified by ID.
      // Note that this code ensures that if both lists are populated,
      // we avoid creating duplicate links.
      //
      predecessors = mpx.getPredecessors();
      if (predecessors != null)
      {
         for (Relation rel : predecessors)
         {
            Integer taskUniqueID = rel.getTaskUniqueID();
            if (set.contains(taskUniqueID) == false)
            {
               list.add(writePredecessor(taskUniqueID, rel.getType(), rel.getDuration()));
            }
         }
      }
   }

   /**
    * This method writes a single predecessor link to the MSPDI file.
    *
    * @param taskID The task UID
    * @param type The predecessor type
    * @param lag The lag duration
    * @return A new link to be added to the MSPDI file
    */
   private Project.Tasks.Task.PredecessorLink writePredecessor(Integer taskID, RelationType type, Duration lag)
   {
      Project.Tasks.Task.PredecessorLink link = m_factory.createProjectTasksTaskPredecessorLink();

      link.setPredecessorUID(NumberUtility.getBigInteger(taskID));
      link.setType(BigInteger.valueOf(type.getValue()));

      if (lag != null && lag.getDuration() != 0)
      {
         link.setLinkLag(BigInteger.valueOf((long) DatatypeConverter.printDurationInMinutes(lag) * 10));
         link.setLagFormat(DatatypeConverter.printDurationTimeUnits(lag.getUnits()));
      }

      return (link);
   }

   /**
    * This method writes assignment data to an MSPDI file.
    *
    * @param project Root node of the MSPDI file
    */
   private void writeAssignments(Project project)
   {
      int uid = 0;
      Project.Assignments assignments = m_factory.createProjectAssignments();
      project.setAssignments(assignments);
      List<Project.Assignments.Assignment> list = assignments.getAssignment();

      for (ResourceAssignment assignment : m_projectFile.getAllResourceAssignments())
      {
         list.add(writeAssignment(assignment, uid));
         ++uid;
      }

      //
      // Check to see if we have any tasks that have a percent complete value
      // but do not have resource assignments. If any exist, then we must
      // write a dummy resource assignment record to ensure that the MSPDI
      // file shows the correct percent complete amount for the task.
      //
      for (Task task : m_projectFile.getAllTasks())
      {
         double percentComplete = NumberUtility.getDouble(task.getPercentageComplete());
         if (percentComplete != 0 && task.getResourceAssignments().isEmpty() == true)
         {
            ResourceAssignment dummy = m_projectFile.newResourceAssignment(task);
            Duration duration = task.getDuration();
            if (duration == null)
            {
               duration = Duration.getInstance(0, TimeUnit.HOURS);
            }
            double durationValue = duration.getDuration();
            TimeUnit durationUnits = duration.getUnits();
            double actualWork = (durationValue * percentComplete) / 100;
            double remainingWork = durationValue - actualWork;

            dummy.setResourceUniqueID(NULL_RESOURCE_ID);
            dummy.setWork(duration);
            dummy.setActualWork(Duration.getInstance(actualWork, durationUnits));
            dummy.setRemainingWork(Duration.getInstance(remainingWork, durationUnits));

            list.add(writeAssignment(dummy, uid));
            ++uid;
         }
      }
   }

   /**
    * This method writes data for a single assignment to an MSPDI file.
    *
    * @param mpx Resource assignment data
    * @param uid Unique ID for the new assignment
    * @return New MSPDI assignment instance
    */
   private Project.Assignments.Assignment writeAssignment(ResourceAssignment mpx, int uid)
   {
      Project.Assignments.Assignment xml = m_factory.createProjectAssignmentsAssignment();

      xml.setActualCost(DatatypeConverter.printCurrency(mpx.getActualCost()));
      xml.setActualWork(DatatypeConverter.printDuration(this, mpx.getActualWork()));
      xml.setCost(DatatypeConverter.printCurrency(mpx.getCost()));
      xml.setDelay(BigInteger.valueOf((long) DatatypeConverter.printDurationInMinutes(mpx.getDelay()) * 1000));
      xml.setFinish(DatatypeConverter.printDate(mpx.getFinish()));
      xml.setOvertimeWork(DatatypeConverter.printDuration(this, mpx.getOvertimeWork()));
      xml.setRemainingWork(DatatypeConverter.printDuration(this, mpx.getRemainingWork()));
      xml.setResourceUID(BigInteger.valueOf(NumberUtility.getInt(mpx.getResourceUniqueID())));
      xml.setStart(DatatypeConverter.printDate(mpx.getStart()));
      xml.setTaskUID(NumberUtility.getBigInteger(mpx.getTask().getUniqueID()));
      xml.setUID(BigInteger.valueOf(uid));
      xml.setUnits(DatatypeConverter.printUnits(mpx.getUnits()));
      xml.setWork(DatatypeConverter.printDuration(this, mpx.getWork()));
      xml.setWorkContour(mpx.getWorkContour());

      writeAssignmentTimephasedData(mpx, xml);

      return (xml);
   }

   /**
    * Writes the timephased data for a resource assignment.
    * 
    * @param mpx MPXJ assignment
    * @param xml MSDPI assignment
    */
   private void writeAssignmentTimephasedData(ResourceAssignment mpx, Project.Assignments.Assignment xml)
   {
      if (mpx.getHasTimephasedData())
      {
         List<TimephasedDataType> list = xml.getTimephasedData();
         ProjectCalendar calendar = mpx.getCalendar();
         BigInteger assignmentID = xml.getUID();

         List<TimephasedResourceAssignment> complete = mpx.getTimephasedComplete();
         TimephasedResourceAssignment lastComplete = null;
         if (!complete.isEmpty())
         {
            lastComplete = complete.get(complete.size() - 1);
         }

         List<TimephasedResourceAssignment> planned = mpx.getTimephasedPlanned();
         TimephasedResourceAssignment firstPlanned = null;
         if (!planned.isEmpty())
         {
            firstPlanned = planned.get(0);
         }

         writeAssignmentTimephasedData(assignmentID, list, splitDays(calendar, mpx.getTimephasedPlanned(), null, lastComplete), 1);
         writeAssignmentTimephasedData(assignmentID, list, splitDays(calendar, complete, firstPlanned, null), 2);
      }
   }

   /**
    * Splits timephased data into individual days.
    * 
    * @param calendar current calendar
    * @param list list of timephased assignment data
    * @param first first planned assignment
    * @param last last completed assignment
    * @return list of timephased data ready for output
    */
   private List<TimephasedResourceAssignment> splitDays(ProjectCalendar calendar, List<TimephasedResourceAssignment> list, TimephasedResourceAssignment first, TimephasedResourceAssignment last)
   {
      List<TimephasedResourceAssignment> result = new LinkedList<TimephasedResourceAssignment>();

      for (TimephasedResourceAssignment assignment : list)
      {
         Date startDate = assignment.getStart();
         Date finishDate = assignment.getFinish();
         Date startDay = DateUtility.getDayStartDate(startDate);
         Date finishDay = DateUtility.getDayStartDate(finishDate);
         if (startDay.getTime() == finishDay.getTime())
         {
            Date startTime = calendar.getStartTime(startDay);
            Date currentStart = DateUtility.setTime(startDay, startTime);
            if (startDate.getTime() > currentStart.getTime())
            {
               boolean paddingRequired = true;

               if (last != null)
               {
                  Date lastFinish = last.getFinish();
                  if (lastFinish.getTime() == startDate.getTime())
                  {
                     paddingRequired = false;
                  }
                  else
                  {
                     Date lastFinishDay = DateUtility.getDayStartDate(lastFinish);
                     if (startDay.getTime() == lastFinishDay.getTime())
                     {
                        currentStart = lastFinish;
                     }
                  }
               }

               if (paddingRequired)
               {
                  Duration zeroHours = Duration.getInstance(0, TimeUnit.HOURS);
                  TimephasedResourceAssignment padding = new TimephasedResourceAssignment();
                  padding.setStart(currentStart);
                  padding.setFinish(startDate);
                  padding.setTotalWork(zeroHours);
                  padding.setWorkPerDay(zeroHours);
                  result.add(padding);
               }
            }

            result.add(assignment);

            Date endTime = calendar.getFinishTime(startDay);
            Date currentFinish = DateUtility.setTime(startDay, endTime);
            if (finishDate.getTime() < currentFinish.getTime())
            {
               boolean paddingRequired = true;

               if (first != null)
               {
                  Date firstStart = first.getStart();
                  if (firstStart.getTime() == finishDate.getTime())
                  {
                     paddingRequired = false;
                  }
                  else
                  {
                     Date firstStartDay = DateUtility.getDayStartDate(firstStart);
                     if (finishDay.getTime() == firstStartDay.getTime())
                     {
                        currentFinish = firstStart;
                     }
                  }
               }

               if (paddingRequired)
               {
                  Duration zeroHours = Duration.getInstance(0, TimeUnit.HOURS);
                  TimephasedResourceAssignment padding = new TimephasedResourceAssignment();
                  padding.setStart(finishDate);
                  padding.setFinish(currentFinish);
                  padding.setTotalWork(zeroHours);
                  padding.setWorkPerDay(zeroHours);
                  result.add(padding);
               }
            }
         }
         else
         {
            Date currentStart = startDate;
            Calendar cal = Calendar.getInstance();
            boolean isWorking = calendar.isWorkingDate(currentStart);
            while (currentStart.getTime() < finishDate.getTime())
            {
               if (isWorking)
               {
                  Date endTime = calendar.getFinishTime(currentStart);
                  Date currentFinish = DateUtility.setTime(currentStart, endTime);

                  TimephasedResourceAssignment split = new TimephasedResourceAssignment();
                  split.setStart(currentStart);
                  split.setFinish(currentFinish);
                  split.setTotalWork(assignment.getWorkPerDay());
                  split.setWorkPerDay(assignment.getWorkPerDay());
                  result.add(split);
               }

               cal.setTime(currentStart);
               cal.add(Calendar.DAY_OF_YEAR, 1);
               currentStart = cal.getTime();
               isWorking = calendar.isWorkingDate(currentStart);
               if (isWorking)
               {
                  Date startTime = calendar.getStartTime(currentStart);
                  DateUtility.setTime(cal, startTime);
                  currentStart = cal.getTime();
               }
            }
         }
      }

      return result;
   }

   /**
    * Writes a list of timephased data to the MSPDI file.
    * 
    * @param assignmentID current assignment ID
    * @param list output list of timephased data items 
    * @param data input list of timephased data
    * @param type list type (planned or completed)
    */
   private void writeAssignmentTimephasedData(BigInteger assignmentID, List<TimephasedDataType> list, List<TimephasedResourceAssignment> data, int type)
   {
      for (TimephasedResourceAssignment mpx : data)
      {
         TimephasedDataType xml = m_factory.createTimephasedDataType();
         list.add(xml);

         xml.setStart(DatatypeConverter.printDate(mpx.getStart()));
         xml.setFinish(DatatypeConverter.printDate(mpx.getFinish()));
         xml.setType(BigInteger.valueOf(type));
         xml.setUID(assignmentID);
         xml.setUnit(DatatypeConverter.printDurationTimeUnits(mpx.getTotalWork()));
         xml.setValue(DatatypeConverter.printDuration(this, mpx.getTotalWork()));
      }
   }

   /**
    * Package-private accessor method used to retrieve the project file
    * currently being processed by this writer.
    *
    * @return project file instance
    */
   ProjectFile getProjectFile()
   {
      return (m_projectFile);
   }

   private ObjectFactory m_factory;

   private ProjectFile m_projectFile;

   private static final BigInteger BIGINTEGER_ZERO = BigInteger.valueOf(0);

   private static final Integer NULL_RESOURCE_ID = Integer.valueOf(-65535);
}
