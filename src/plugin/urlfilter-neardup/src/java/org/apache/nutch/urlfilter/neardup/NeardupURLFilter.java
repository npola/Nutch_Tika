/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.urlfilter.neardup;

// JDK imports
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.net.URLFilter;
import org.apache.nutch.plugin.Extension;
import org.apache.nutch.plugin.PluginRepository;
import org.apache.nutch.util.URLUtil;


public class NeardupURLFilter implements URLFilter {

    private int counter = 0;
    private Configuration conf;
    private String[] dbFieldnames;
    private Map<String, String> parseFieldnames;
    private String[] contentFieldnames;
    private static final String db_CONF_PROPERTY = "index.db.md";
    private static final String parse_CONF_PROPERTY = "index.parse.md";
    private static final String content_CONF_PROPERTY = "index.content.md";

private static final Logger LOG = LoggerFactory
      .getLogger(NeardupURLFilter.class);

      public NeardupURLFilter() {

        if(LOG.isWarnEnabled())
        {
          LOG.warn("A Nearduplication Plugin Instance is Created and its a serious Warning! :p");
        }
  }

  public NeardupURLFilter(Configuration conf){
    this.conf = conf;
  }

    /**
   * Sets the configuration.
   */
  public void setConf(Configuration conf) {

    this.conf = conf;
    dbFieldnames = conf.getStrings(db_CONF_PROPERTY);
    parseFieldnames = new HashMap<String, String>();
    for (String metatag : conf.getStrings(parse_CONF_PROPERTY)) {
      parseFieldnames.put(metatag.toLowerCase(Locale.ROOT), metatag);
        if(LOG.isWarnEnabled())
        {         
          LOG.warn("Parse Field Names "+metatag);
        }
    }
    contentFieldnames = conf.getStrings(content_CONF_PROPERTY);
  }

    public Configuration getConf() {
    return this.conf;
  }

    public String filter(String url) {

    try {

      // match for suffix, domain, and host in that order. more general will
      // override more specific
      
      return url;
    } catch (Exception e) {

      // if an error happens, allow the url to pass
      LOG.error("Could not apply filter on url: " + url + "\n"
          + org.apache.hadoop.util.StringUtils.stringifyException(e));
      return null;
    }
  }

}
