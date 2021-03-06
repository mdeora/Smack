/**
 *
 * Copyright the original author or authors
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
 */
package org.jivesoftware.smackx.privacy.provider;

import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.privacy.packet.Privacy;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The PrivacyProvider parses {@link Privacy} packets. {@link Privacy}
 * Parses the <tt>query</tt> sub-document and creates an instance of {@link Privacy}.
 * For each <tt>item</tt> in the <tt>list</tt> element, it creates an instance 
 * of {@link PrivacyItem}.
 * 
 * @author Francisco Vives
 */
public class PrivacyProvider extends IQProvider<Privacy> {

    @Override
    public Privacy parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException {
        Privacy privacy = new Privacy();
        /* privacy.addExtension(PacketParserUtils.parsePacketExtension(parser
                .getName(), parser.getNamespace(), parser)); */
        privacy.addExtension(new DefaultPacketExtension(parser.getName(), parser.getNamespace()));
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("active")) {
                	String activeName = parser.getAttributeValue("", "name");
                	if (activeName == null) {
                		privacy.setDeclineActiveList(true);
                	} else {
                		privacy.setActiveName(activeName);
                	}
                }
                else if (parser.getName().equals("default")) {
                	String defaultName = parser.getAttributeValue("", "name");
                	if (defaultName == null) {
                		privacy.setDeclineDefaultList(true);
                	} else {
                		privacy.setDefaultName(defaultName);
                	}
                }
                else if (parser.getName().equals("list")) {
                    parseList(parser, privacy);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }

        return privacy;
	}
	
	// Parse the list complex type
	public void parseList(XmlPullParser parser, Privacy privacy) throws XmlPullParserException, IOException {
        boolean done = false;
        String listName = parser.getAttributeValue("", "name");
        ArrayList<PrivacyItem> items = new ArrayList<PrivacyItem>();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                	items.add(parseItem(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("list")) {
                    done = true;
                }
            }
        }

        privacy.setPrivacyList(listName, items);
	}
	
	// Parse the list complex type
	public PrivacyItem parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean done = false;
        // Retrieves the required attributes
        String actionValue = parser.getAttributeValue("", "action");
        String orderValue = parser.getAttributeValue("", "order");
        String type = parser.getAttributeValue("", "type");

        /* 
         * According the action value it sets the allow status. The fall-through action is assumed 
         * to be "allow"
         */
        boolean allow = true;
        if ("allow".equalsIgnoreCase(actionValue)) {
        	allow = true;
        } else if ("deny".equalsIgnoreCase(actionValue)) {
        	allow = false;
        }
        // Set the order number
        int order = Integer.parseInt(orderValue);

        PrivacyItem item;
        if (type != null) {
            // If the type is not null, then we are dealing with a standard privacy item
            String value = parser.getAttributeValue("", "value");
            item = new PrivacyItem(PrivacyItem.Type.valueOf(type), value, allow, order);

            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("iq")) {
                        item.setFilterIQ(true);
                    }
                    if (parser.getName().equals("message")) {
                        item.setFilterMessage(true);
                    }
                    if (parser.getName().equals("presence-in")) {
                        item.setFilterPresenceIn(true);
                    }
                    if (parser.getName().equals("presence-out")) {
                        item.setFilterPresenceOut(true);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("item")) {
                        done = true;
                    }
                }
            }
        }
        else {
            // If the type is null, then we are dealing with the fall-through privacy item.
            item = new PrivacyItem(allow, order);
        }
        return item;
	}
}
