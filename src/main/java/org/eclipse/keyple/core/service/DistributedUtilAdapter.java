/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import com.google.gson.JsonObject;
import java.util.List;
import org.calypsonet.terminal.card.ChannelControl;
import org.calypsonet.terminal.card.ProxyReaderApi;
import org.calypsonet.terminal.card.spi.CardRequestSpi;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.eclipse.keyple.core.distributed.remote.spi.AbstractRemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;

/**
 * Utility class of distributed components.
 *
 * @since 2.0.0
 */
final class DistributedUtilAdapter {

  /** Constructor. */
  private DistributedUtilAdapter() {}

  /**
   * Executes remotely the provided JSON input data of a specific plugin service, parses the
   * provided JSON output data, checks if the JSON contains an error and throws the embedded
   * exception if exists..
   *
   * @param input The JSON input data to process.
   * @param remotePluginSpi The SPI in charge of carrying out the treatment.
   * @param pluginName The name of the remote plugin.
   * @param logger The logger to use for logging.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   * @since 2.0.0
   */
  static JsonObject executePluginServiceRemotely(
      JsonObject input, AbstractRemotePluginSpi remotePluginSpi, String pluginName, Logger logger)
      throws Exception { // NOSONAR

    if (logger.isDebugEnabled()) {
      logger.debug("Plugin '{}' sends JSON data : {}", pluginName, input);
    }

    String outputJson = remotePluginSpi.executeRemotely(input.toString());

    if (logger.isDebugEnabled()) {
      logger.debug("Plugin '{}' receives JSON data : {}", pluginName, outputJson);
    }

    return getJsonObject(outputJson);
  }

  /**
   * Executes remotely the provided JSON input data of a specific reader service, parses the
   * provided JSON output data, checks if the JSON contains an error and throws the embedded
   * exception if exists.
   *
   * @param input The JSON input data to process.
   * @param remoteReaderSpi The SPI in charge of carrying out the treatment.
   * @param readerName The name of the remote reader.
   * @param pluginName The name of the remote plugin.
   * @param logger The logger to use for logging.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   * @since 2.0.0
   */
  static JsonObject executeReaderServiceRemotely(
      JsonObject input,
      RemoteReaderSpi remoteReaderSpi,
      String readerName,
      String pluginName,
      Logger logger)
      throws Exception { // NOSONAR

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' sends JSON data : {}", readerName, pluginName, input);
    }

    String outputJson = remoteReaderSpi.executeRemotely(input.toString());

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Reader '{}' of plugin '{}' receives JSON data : {}", readerName, pluginName, outputJson);
    }

    return getJsonObject(outputJson);
  }

  /**
   * Parses the provided JSON output data, checks if the JSON contains an error and throws the
   * embedded exception if exists.
   *
   * @param outputJson The JSON to parse.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   * @since 2.0.0
   */
  private static JsonObject getJsonObject(String outputJson) throws Exception { // NOSONAR

    if (outputJson == null || outputJson.isEmpty()) {
      return null;
    }

    JsonObject output = JsonUtil.getParser().fromJson(outputJson, JsonObject.class);
    if (output.has(JsonProperty.ERROR.name())) {
      BodyError body =
          JsonUtil.getParser()
              .fromJson(output.get(JsonProperty.ERROR.name()).getAsString(), BodyError.class);
      throw body.getException();
    }
    return output;
  }

  /**
   * Throws a runtime exception containing the provided exception.
   *
   * @param e The cause.
   * @throws RuntimeException The thrown runtime exception.
   * @since 2.0.0
   */
  static void throwRuntimeException(Exception e) {
    throw new RuntimeException( // NOSONAR
        String.format(
            "The distributed message sender received an unexpected error : %s", e.getMessage()),
        e);
  }

  /**
   * Enumeration of all available common JSON properties.
   *
   * @since 2.0.0
   */
  enum JsonProperty {

    /** @since 2.0.0 */
    CARD_REQUEST,

    /** @since 2.0.0 */
    CARD_SELECTION_REQUESTS,

    /** @since 2.0.0 */
    CARD_SELECTION_SCENARIO,

    /** @since 2.0.0 */
    CHANNEL_CONTROL,

    /** @since 2.0.0 */
    ERROR,

    /** @since 2.0.0 */
    MULTI_SELECTION_PROCESSING,

    /** @since 2.0.0 */
    NOTIFICATION_MODE,

    /** @since 2.0.0 */
    PLUGIN_EVENT,

    /** @since 2.0.0 */
    POLLING_MODE,

    /** @since 2.0.0 */
    READER_EVENT,

    /** @since 2.0.0 */
    READER_GROUP_REFERENCE,

    /** @since 2.0.0 */
    READER_NAME,

    /** @since 2.0.0 */
    RESULT,

    /** @since 2.0.0 */
    SERVICE
  }

  /**
   * Enumeration of the available local services that can be invoked on local plugins from the
   * remote plugin.
   *
   * @since 2.0.0
   */
  enum PluginService {

    /**
     * Refers to {@link Plugin#getReaders()}
     *
     * @since 2.0.0
     */
    GET_READERS,

    /**
     * Refers to {@link PoolPlugin#getReaderGroupReferences()}
     *
     * @since 2.0.0
     */
    GET_READER_GROUP_REFERENCES,

    /**
     * Refers to {@link PoolPlugin#allocateReader(String)}
     *
     * @since 2.0.0
     */
    ALLOCATE_READER,

    /**
     * Refers to {@link PoolPlugin#releaseReader(CardReader)}
     *
     * @since 2.0.0
     */
    RELEASE_READER,

    /**
     * Refers to {@link ObservablePlugin#addObserver(PluginObserverSpi)}
     *
     * @since 2.0.0
     */
    START_READER_DETECTION,

    /**
     * Refers to {@link ObservablePlugin#removeObserver(PluginObserverSpi)}
     *
     * @since 2.0.0
     */
    STOP_READER_DETECTION
  }

  /**
   * Enumeration of the available local services that can be invoked on a local reader from the
   * remote reader.
   *
   * @since 2.0.0
   */
  enum ReaderService {

    /**
     * Refers to {@link ProxyReaderApi#transmitCardRequest(CardRequestSpi, ChannelControl)}
     *
     * @since 2.0.0
     */
    TRANSMIT_CARD_REQUEST,

    /**
     * Refers to {@link AbstractReaderAdapter#transmitCardSelectionRequests(List,
     * MultiSelectionProcessing, ChannelControl)}
     *
     * @since 2.0.0
     */
    TRANSMIT_CARD_SELECTION_REQUESTS,

    /**
     * Refers to {@link
     * ObservableLocalReaderAdapter#scheduleCardSelectionScenario(CardSelectionScenarioAdapter,
     * ObservableCardReader.NotificationMode, ObservableCardReader.DetectionMode)} and {@link
     * ObservableRemoteReaderAdapter#scheduleCardSelectionScenario(CardSelectionScenarioAdapter,
     * ObservableCardReader.NotificationMode, ObservableCardReader.DetectionMode)}
     *
     * @since 2.0.0
     */
    SCHEDULE_CARD_SELECTION_SCENARIO,

    /**
     * Refers to {@link CardReader#isCardPresent()}
     *
     * @since 2.0.0
     */
    IS_CARD_PRESENT,

    /**
     * Refers to {@link CardReader#isContactless()}
     *
     * @since 2.0.0
     */
    IS_CONTACTLESS,

    /**
     * Refers to {@link ObservableCardReader#startCardDetection(ObservableCardReader.DetectionMode)}
     *
     * @since 2.0.0
     */
    START_CARD_DETECTION,

    /**
     * Refers to {@link ObservableCardReader#startCardDetection(ObservableCardReader.DetectionMode)}
     *
     * @since 2.0.0
     */
    STOP_CARD_DETECTION,

    /**
     * Refers to {@link ObservableCardReader#finalizeCardProcessing()}
     *
     * @since 2.0.0
     */
    FINALIZE_CARD_PROCESSING,

    /**
     * Refers to {@link ProxyReaderApi#releaseChannel()}
     *
     * @since 2.0.0
     */
    RELEASE_CHANNEL
  }
}
