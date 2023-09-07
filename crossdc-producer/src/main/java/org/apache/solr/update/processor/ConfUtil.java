/*
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
package org.apache.solr.update.processor;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.crossdc.common.CrossDcConf;
import org.apache.solr.crossdc.common.KafkaCrossDcConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Properties;

import static org.apache.solr.crossdc.common.KafkaCrossDcConf.BOOTSTRAP_SERVERS;
import static org.apache.solr.crossdc.common.KafkaCrossDcConf.TOPIC_NAME;

public class ConfUtil {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void fillProperties(SolrZkClient solrClient, Map<String, Object> properties) {
    Properties zkProps = null;
    try {
      if (solrClient.exists(System.getProperty(CrossDcConf.ZK_CROSSDC_PROPS_PATH,
              CrossDcConf.CROSSDC_PROPERTIES), true)) {
        byte[] data = solrClient.getData(System.getProperty(CrossDcConf.ZK_CROSSDC_PROPS_PATH,
            CrossDcConf.CROSSDC_PROPERTIES), null, null, true);

        if (data == null) {
          log.error(CrossDcConf.CROSSDC_PROPERTIES + " file in Zookeeper has no data");
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, CrossDcConf.CROSSDC_PROPERTIES
              + " file in Zookeeper has no data");
        }

        zkProps = new Properties();
        zkProps.load(new ByteArrayInputStream(data));

        KafkaCrossDcConf.readZkProps(properties, zkProps);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted looking for CrossDC configuration in Zookeeper", e);
      throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE, e);
    } catch (Exception e) {
      log.error("Exception looking for CrossDC configuration in Zookeeper", e);
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Exception looking for CrossDC configuration in Zookeeper", e);
    }

    if (properties.get(BOOTSTRAP_SERVERS) == null) {
      log.error(
          "bootstrapServers not specified for producer in CrossDC configuration props={}, zkProps={}",
          properties, zkProps);
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "bootstrapServers not specified for producer");
    }

    if (properties.get(TOPIC_NAME) == null) {
      log.error(
          "topicName not specified for producer in CrossDC configuration props={}, zkProps={}",
          properties, zkProps);
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "topicName not specified for producer");
    }
  }
}
