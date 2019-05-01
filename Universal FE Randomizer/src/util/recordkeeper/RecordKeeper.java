package util.recordkeeper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordKeeper {
	
	private class Entry {
		List<String> allKeys;
		Map<String, String> originalValues;
		Map<String, String> updatedValues; 
		
		Map<String, String> additionalInfo;
		
		private Entry() {
			allKeys = new ArrayList<String>();
			originalValues = new HashMap<String, String>();
			updatedValues = new HashMap<String, String>();
			additionalInfo = new HashMap<String, String>();
		}
	}
	
	private class EntryMap {
		List<String> keyList;
		Map<String, Entry> entriesByKey;
		
		private EntryMap() {
			keyList = new ArrayList<String>();
			entriesByKey = new HashMap<String, Entry>();
		}
	}
	
	private class Header {
		String title;
		
		List<String> keyList;
		Map<String, String> values;
		
		private Header() {
			keyList = new ArrayList<String>();
			values = new HashMap<String, String>();
		}
	}
	
	private Header header;
	private List<String> allCategories;
	private Map<String, EntryMap> entriesByCategory;
	
	private List<String> notes;
	
	public RecordKeeper(String title) {
		allCategories = new ArrayList<String>();
		entriesByCategory = new HashMap<String, EntryMap>();
		header = new Header();
		header.title = title;
		
		header.keyList = new ArrayList<String>();
		header.values = new HashMap<String, String>();
		
		notes = new ArrayList<String>();
	}
	
	public void addHeaderItem(String title, String value) {
		if (header.keyList.contains(title)) { header.keyList.remove(title); }
		header.keyList.add(title);
		header.values.put(title, value);
	}
	
	public void addNote(String note) {
		notes.add(note);
	}
	
	public void registerCategory(String category) {
		if (allCategories.contains(category)) {
			return;
		}
		
		EntryMap map = new EntryMap();
		entriesByCategory.put(category, map);
		allCategories.add(category);
	}
	
	public void setAdditionalInfo(String category, String entryKey, String key, String info) {
		EntryMap entryMap = entriesByCategory.get(category);
		if (entryMap == null) { return; }
		Entry entry = entryMap.entriesByKey.get(entryKey);
		if (entry == null) { return; }
		
		entry.additionalInfo.put(key, info);
	}
	
	public void clearAdditionalInfo(String category, String entryKey, String key) {
		EntryMap entryMap = entriesByCategory.get(category);
		if (entryMap == null) { return; }
		Entry entry = entryMap.entriesByKey.get(entryKey);
		if (entry == null) { return; }
		
		entry.additionalInfo.remove(key);
	}

	public void recordOriginalEntry(String category, String entryKey, String key, String originalValue) {
		 EntryMap entryMap = entriesByCategory.get(category);
		 if (entryMap == null) {
			 entryMap = new EntryMap();
			 entriesByCategory.put(category, entryMap);
			 allCategories.add(category);
		 }
		 
		 Entry entry = entryMap.entriesByKey.get(entryKey);
		 if (entry == null) {
			 entry = new Entry();
			 entryMap.keyList.add(entryKey);
			 entryMap.entriesByKey.put(entryKey, entry);
		 }
		 
		 if (!entry.allKeys.contains(key)) { 
			 entry.allKeys.add(key);
		 }
		 entry.originalValues.put(key, originalValue);
	}
	
	public void recordUpdatedEntry(String category, String entryKey, String key, String updatedValue) {
		EntryMap entryMap = entriesByCategory.get(category);
		 if (entryMap == null) {
			 entryMap = new EntryMap();
			 entriesByCategory.put(category, entryMap);
			 allCategories.add(category);
		 }
		 
		 Entry entry = entryMap.entriesByKey.get(entryKey);
		 if (entry == null) {
			 entry = new Entry();
			 entryMap.keyList.add(entryKey);
			 entryMap.entriesByKey.put(entryKey, entry);
		 }
		 if (!entry.allKeys.contains(key)) { 
			 entry.allKeys.add(key);
		 }
		 entry.updatedValues.put(key, updatedValue);
	}
	
	public void sortKeysInCategory(String category) {
		EntryMap entryMap = entriesByCategory.get(category);
		if (entryMap != null) {
			Collections.sort(entryMap.keyList);
		}
	}
	
	public void sortKeysInCategoryAndSubcategories(String category) {
		Set<String> keys = entriesByCategory.keySet();
		for (String key : keys) {
			if (key.startsWith(category)) {
				EntryMap entryMap = entriesByCategory.get(key);
				if (entryMap != null) {
					Collections.sort(entryMap.keyList);
				}
			}
		}
	}
	
	public Boolean exportRecordsToHTML(String outputPath) {
		try {
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputPath), Charset.forName("UTF-8").newEncoder());
			writer.write("<html><meta http-equiv=\"Content-Type\" content = \"text/html; charset=utf-8\" /><head><style>\n");
			writer.write("table, th, td {\n\tborder: 1px solid black;\n}\n");
			writer.write(".notes {\n\twidth: 66%;\n\tmargin: auto;\n}\n");
			writer.write("</style></head><body>\n");
			writer.write("<center><h1><p>Changelog for " + header.title + "</p></h1><br>\n");
			writer.write("<hr>\n");
			writer.write("<table>\n");
			for (String key : header.keyList) {
				String value = header.values.get(key);
				writer.write("<tr><td>" + key + "</td><td>" + value + "</td></tr>\n");
			}
			writer.write("</table>\n");
			writer.write("<br><hr><br>\n");
			
			if (!notes.isEmpty()) {
				writer.write("<h2>Notes</h2><br>\n</center>\n");
				writer.write("<div class=\"notes\"><ul>");
				for (String note : notes) {
					writer.write("<li>" + note + "</li>\n");
				}
				writer.write("</ul></div>\n<center>\n<br><hr><br>\n");
			}
			
			for (String category : allCategories) {
				writer.write("<h2 id=\"" + keyFromString(category) + "\">" + category + "</h2>");
				int column = 0;
				writer.write("<table>\n");
				EntryMap entries = entriesByCategory.get(category);
				for (String entryKey : entries.keyList) {
					if (column == 0) { writer.write("<tr>\n"); }
					writer.write("<td><a href=\"#" + keyFromString(entryKey) + "\">" + entryKey + "</a></td>\n");
					column += 1;
					if (column == 4) {
						column = 0;
						writer.write("</tr>\n");
					}
				}
				writer.write("</table>\n");
				writer.write("<br><hr><br>\n");
			}
			
			for (String category : allCategories) {
				EntryMap entries = entriesByCategory.get(category);
				for (String entryKey : entries.keyList) {
					Entry entry = entries.entriesByKey.get(entryKey);
					writer.write("<h3 id=\"" + keyFromString(entryKey) + "\">" + entryKey + "</h3><br>\n");
					writer.write("<table>\n");
					boolean hasAdditionalInfo = !entry.additionalInfo.isEmpty();
					for (String key : entry.allKeys) {
						String oldValue = entry.originalValues.get(key);
						String newValue = entry.updatedValues.get(key);
						writer.write("<tr><td>" + key + "</td><td>" + (oldValue != null ? oldValue : "(null)") + "</td><td>" + (newValue != null ? newValue : "(null)") + "</td>");
						if (hasAdditionalInfo) {
							boolean hasInfoForKey = entry.additionalInfo.containsKey(key);
							writer.write("<td>" + (hasInfoForKey ? entry.additionalInfo.get(key) : "") + "</td>");
						}
						writer.write("</tr>\n");
					}
					writer.write("</table>\n");
					writer.write("<a href=\"#" + keyFromString(category) + "\">Back to " + category + "</a>\n");
				}
				writer.write("<br><hr><br>\n");
			}
			
			writer.write("</body></html>\n");
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private String keyFromString(String string) {
		return string.replace(' ', '_');
	}
}
