/*
 * ITunesParser.java
 *
 * Created on August 1, 2005, 8:29 PM
 *
 * This library is provided under dual licenses.
 * You may choose the terms of the Lesser General Public License or the Apache
 * License at your discretion.
 *
 *  Copyright (C) 2005  Robert Cooper, Temple of the Screaming Penguin
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 
 */
package org.rometools.feed.module.itunes.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.rometools.feed.module.itunes.AbstractITunesObject;
import org.rometools.feed.module.itunes.EntryInformationImpl;
import org.rometools.feed.module.itunes.FeedInformationImpl;
import org.rometools.feed.module.itunes.types.Category;
import org.rometools.feed.module.itunes.types.Duration;
import org.rometools.feed.module.itunes.types.Subcategory;

import com.sun.syndication.io.ModuleParser;
import com.sun.syndication.io.WireFeedParser;

/**
 * @version $Revision: 1.10 $
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet" Cooper</a>
 */
public class ITunesParser implements ModuleParser {
    static Logger log = Logger.getLogger(ITunesParser.class.getName());
    Namespace ns = Namespace.getNamespace(AbstractITunesObject.URI);

    /** Creates a new instance of ITunesParser */
    public ITunesParser() {
    }

    public void setParser(final WireFeedParser feedParser) {
    }

    @Override
    public String getNamespaceUri() {
        return AbstractITunesObject.URI;
    }

    @Override
    public com.sun.syndication.feed.module.Module parse(final Element element, final Locale locale) {
        AbstractITunesObject module = null;

        if (element.getName().equals("channel")) {
            final FeedInformationImpl feedInfo = new FeedInformationImpl();
            module = feedInfo;

            // Now I am going to get the channel specific tags
            final Element owner = element.getChild("owner", ns);

            if (owner != null) {
                final Element name = owner.getChild("name", ns);

                if (name != null) {
                    feedInfo.setOwnerName(name.getValue().trim());
                }

                final Element email = owner.getChild("email", ns);

                if (email != null) {
                    feedInfo.setOwnerEmailAddress(email.getValue().trim());
                }
            }

            final Element image = element.getChild("image", ns);

            if (image != null && image.getAttributeValue("href") != null) {
                try {
                    final URL imageURL = new URL(image.getAttributeValue("href").trim());
                    feedInfo.setImage(imageURL);
                } catch (final MalformedURLException e) {
                    log.finer("Malformed URL Exception reading itunes:image tag: " + image.getAttributeValue("href"));
                }
            }

            final List categories = element.getChildren("category", ns);
            for (final Iterator it = categories.iterator(); it.hasNext();) {
                final Element category = (Element) it.next();
                if (category != null && category.getAttribute("text") != null) {
                    final Category cat = new Category();
                    cat.setName(category.getAttribute("text").getValue().trim());

                    final Element subcategory = category.getChild("category", ns);

                    if (subcategory != null && subcategory.getAttribute("text") != null) {
                        final Subcategory subcat = new Subcategory();
                        subcat.setName(subcategory.getAttribute("text").getValue().trim());
                        cat.setSubcategory(subcat);
                    }

                    feedInfo.getCategories().add(cat);
                }
            }

        } else if (element.getName().equals("item")) {
            final EntryInformationImpl entryInfo = new EntryInformationImpl();
            module = entryInfo;

            // Now I am going to get the item specific tags

            final Element duration = element.getChild("duration", ns);

            if (duration != null && duration.getValue() != null) {
                final Duration dur = new Duration(duration.getValue().trim());
                entryInfo.setDuration(dur);
            }
        }
        if (module != null) {
            // All these are common to both Channel and Item
            final Element author = element.getChild("author", ns);

            if (author != null && author.getText() != null) {
                module.setAuthor(author.getText());
            }

            final Element block = element.getChild("block", ns);

            if (block != null) {
                module.setBlock(true);
            }

            final Element explicit = element.getChild("explicit", ns);

            if (explicit != null && explicit.getValue() != null && explicit.getValue().trim().equalsIgnoreCase("yes")) {
                module.setExplicit(true);
            }

            final Element keywords = element.getChild("keywords", ns);

            if (keywords != null) {
                final StringTokenizer tok = new StringTokenizer(getXmlInnerText(keywords).trim(), ",");
                final String[] keywordsArray = new String[tok.countTokens()];

                for (int i = 0; tok.hasMoreTokens(); i++) {
                    keywordsArray[i] = tok.nextToken();
                }

                module.setKeywords(keywordsArray);
            }

            final Element subtitle = element.getChild("subtitle", ns);

            if (subtitle != null) {
                module.setSubtitle(subtitle.getTextTrim());
            }

            final Element summary = element.getChild("summary", ns);

            if (summary != null) {
                module.setSummary(summary.getTextTrim());
            }
        }

        return module;
    }

    protected String getXmlInnerText(final Element e) {
        final StringBuffer sb = new StringBuffer();
        final XMLOutputter xo = new XMLOutputter();
        final List children = e.getContent();
        sb.append(xo.outputString(children));

        return sb.toString();
    }
}
