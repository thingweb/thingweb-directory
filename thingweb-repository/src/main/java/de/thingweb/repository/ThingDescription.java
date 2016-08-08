package de.thingweb.repository;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ThingDescription implements Comparable<ThingDescription> {
	
	private String id;
	private Calendar lifetime;
	
	public ThingDescription(String id) {
		
		this.id = id;
		this.lifetime = Calendar.getInstance();
	}
	
	public ThingDescription(String id, String time) {
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		this.id = id;
		this.lifetime = Calendar.getInstance();
		try {
			this.lifetime.setTime(dateFormat.parse(time));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o instanceof ThingDescription) {
			if (this.id.equals(((ThingDescription) o).id)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public int compareTo(ThingDescription td) {
		return this.lifetime.compareTo(td.lifetime);
	}

	public String getId() {
		return this.id;
	}
	
	public Calendar getLifetime() {
		return this.lifetime;
	}

}