package ch.unibe.ese.calendar;

import java.security.AccessControlException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import ch.unibe.ese.calendar.security.PrivelegedCalendarAccessPermission;

public class Calendar {
	
	private SortedSet<CalendarEvent> startDateSortedSet = new TreeSet<CalendarEvent>(new StartDateComparator());
	
	/**
	 * Constructs a calendar. Application typically create and retrieve calendars using the CalendarManager.
	 * 
	 * @param name the name of the calendar
	 * @param owner the owner of the calendar
	 */
	protected Calendar(String name, User owner) {
		this.name = name;
		this.owner = owner;
	}

	private String name;
	private User owner;
	
	/**
	 * 
	 * @return the name of this calendar
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * When <code>ch.unibe.ese.calendar.security.CalendarPolicy</code> is in place the owner can write to this calendar.
	 * 
	 * @return the owner of this calendar
	 */
	public User getOwner() {
		return owner;
	}

	/**
	 * Adds the event to the calendar
	 * 
	 * @param calendarEvent
	 */
	public void addEvent(CalendarEvent calendarEvent) {
		AccessController.checkPermission(new PrivelegedCalendarAccessPermission(name, ""));
		startDateSortedSet.add(calendarEvent);
		
	}

	/**
	 * Iterates through the events with a start date after start
	 * 
	 * @param start the date at which to start iterating events
	 * @return an iterator with events strating after start
	 */
	public Iterator<CalendarEvent> iterate(Date start) {
		Iterator<CalendarEvent> unfilteredEvents = startDateSortedSet.tailSet(new CalendarEvent(start, null, null, false)).iterator();
		return new ACFilteringIterator(unfilteredEvents);
	}
	
	/**
	 * Gets a list of events starting within the 24 hour period starting at date;
	 * 
	 * @param date the point in time specifying the start of the 24h period for which events are to be returned
	 * @return a list of the events
	 */
	public List<CalendarEvent> getEventsAt(Date date) {
		Date endDate = new Date(date.getTime()+24*60*60*1000);
		List<CalendarEvent> result = new ArrayList<CalendarEvent>();
		Iterator<CalendarEvent> iter = iterate(date);
		while (iter.hasNext()) {
			CalendarEvent ce = iter.next();
			if (ce.getStart().compareTo(endDate) > 0) {
				break;
			}
			result.add(ce);
		}
		return result;
	}
	
	private class ACFilteringIterator implements Iterator<CalendarEvent> {

		private boolean hasNext;
		private CalendarEvent next;
		private Iterator<CalendarEvent> unfilteredEvents;
		
		public ACFilteringIterator(Iterator<CalendarEvent> unfilteredEvents) {
			this.unfilteredEvents = unfilteredEvents;
			prepareNext();
		}

		private void prepareNext() {
			hasNext = false;
			if (unfilteredEvents.hasNext()) {
				CalendarEvent ce = unfilteredEvents.next();
				if (!ce.isPublic()) {
					try {
						AccessController.checkPermission(new PrivelegedCalendarAccessPermission(name, ""));
					} catch (AccessControlException e) {
						prepareNext();
						return;
					}
				}
				next = ce;
				hasNext = true;
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public CalendarEvent next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			CalendarEvent result = next;
			prepareNext();
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("not supported yet");
		}

	}

	

}
