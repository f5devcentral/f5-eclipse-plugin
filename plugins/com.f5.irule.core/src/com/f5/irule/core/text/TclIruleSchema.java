/*******************************************************************************
 * Copyright 2015-2017 F5 Networks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.f5.irule.core.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility class for reading irule-schema.json<br>
 * and creating lists of commands and events with a map to their descriptions.<br>
 * It currently reads the irule-schema.json file from the local file system<br>
 * but eventually it should be able to get the file from the connected Big-IP.
 */
public enum TclIruleSchema {
    INSTANCE;

    private static boolean haveSchema = false;
    private static String[] commandNames;
    private static String[] eventNames;
    private static HashMap<String, String> descriptions = new HashMap<String, String>();
    private static HashMap<String, String> examples = new HashMap<String, String>();

    public static String[] getCommandNames () {
        if (!haveSchema) {
            readLocalSchema();
        }

        return commandNames;
    }

    public static String[] getEventNames () {
        if (!haveSchema) {
            readLocalSchema();
        }

        return eventNames;
    }

    public static String getDescription (String name) {
        if (!haveSchema) {
            readLocalSchema();
        }

        return descriptions.get(name);
    }
    
    public static String getExamples (String name) {
        if (!haveSchema) {
            readLocalSchema();
        }

        return examples.get(name);
    }

    public static boolean isEvent (String name) {
        if (!haveSchema) {
            readLocalSchema();
        }
        
        return (java.util.Arrays.asList(eventNames).indexOf(name) > -1);
    }

 // TODO get schema from Big-IP.  If not available then read local schema.
    private static boolean readLocalSchema () {
		try {
			URL url = new URL("platform:/plugin/com.f5.irule.core/files/irule-schema.json");
	        InputStream inputStream = url.openConnection().getInputStream();
	        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

	        boolean readStatus = readSchema(in);
            in.close();
            haveSchema = true;
            return readStatus;

		} catch (MalformedURLException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
    }

	private static boolean readSchema (BufferedReader in) {
		JsonParser parser = new JsonParser();
		JsonElement root = parser.parse(in);

		ArrayList<String> commands = new ArrayList<String>();
		ArrayList<String> events = new ArrayList<String>();

		if (root.isJsonArray()) {
			JsonArray jarr = root.getAsJsonArray();

			// Each Entry
			for (JsonElement je : jarr) {
				if (je.isJsonObject()) {
					JsonObject jo = (JsonObject) je;
					Set<Entry<String, JsonElement>> ens = jo.entrySet();

					if (ens == null) {
						continue;
					}

					// Iterate JSON Elements with Key values
					String name = null;
					String description = null;
					String example = null;

					for (Entry<String, JsonElement> en : ens) {
						String key = en.getKey();
						JsonElement val = en.getValue();
						if (key.equals("commandName")) {
							name = val.getAsString();
							commands.add(name);

						} else if (key.equals("eventName")) {
							name = val.getAsString();
							events.add(name);

						} else if (key.equals("description")) {
							description = val.getAsString();
							
                        } else if (key.equals("examples")) {
                            example = val.getAsString();
                        }
					}

					if (name != null && description != null) {
						descriptions.put(name, description);
					}

                    if (name != null && example != null) {
                        examples.put(name, example);
                    }
                }
			}
		}

		commandNames = new String[commands.size()];
		commandNames = commands.toArray(commandNames);

		eventNames = new String[events.size()];
		eventNames = events.toArray(eventNames);

		return true;
	}
}
