/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.card.CardApiProperties;
import org.eclipse.keyple.core.card.spi.CardExtensionSpi;
import org.eclipse.keyple.core.common.CommonsApiProperties;
import org.eclipse.keyple.core.common.KeypleCardExtension;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtensionFactory;
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory;
import org.eclipse.keyple.core.plugin.PluginApiProperties;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.*;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SmartCardService}.
 *
 * @since 2.0
 */
public final class SmartCardServiceAdapter implements SmartCardService {

  private static final Logger logger = LoggerFactory.getLogger(SmartCardServiceAdapter.class);

  /** singleton instance of SmartCardServiceAdapter */
  private static final SmartCardServiceAdapter uniqueInstance = new SmartCardServiceAdapter();

  /** the list of readers’ plugins interfaced with the card Proxy Service */
  private final Map<String, Plugin> plugins = new ConcurrentHashMap<String, Plugin>();

  /** Field MONITOR, this is the object we will be synchronizing on ("the monitor") */
  private final Object monitor = new Object();

  /** Private constructor. */
  private SmartCardServiceAdapter() {}

  /**
   * (package-private)<br>
   * Gets the single instance of SmartCardServiceAdapter.
   *
   * @return single instance of SmartCardServiceAdapter
   * @since 2.0
   */
  static SmartCardServiceAdapter getInstance() {
    return uniqueInstance;
  }

  /**
   * (private)<br>
   * Compare versions.
   *
   * @param providedVersion The provided version string.
   * @param localVersion The local version string.
   * @return 0 if providedVersion equals localVersion, &lt; 0 if providedVersion &lt; localVersion,
   *     &gt; 0 if providedVersion &gt; localVersion.
   */
  private int compareVersions(String providedVersion, String localVersion) {
    String[] providedVersions = providedVersion.split("[.]");
    String[] localVersions = localVersion.split("[.]");
    if (providedVersions.length != localVersions.length) {
      throw new IllegalStateException(
          "Inconsistent version numbers: provided = "
              + providedVersion
              + ", local = "
              + localVersion);
    }
    Integer provided = 0;
    int local = 0;
    try {
      for (String v : providedVersions) {
        provided += Integer.parseInt(v);
        provided *= 1000;
      }
      for (String v : localVersions) {
        local += Integer.parseInt(v);
        local *= 1000;
      }
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Bad version numbers: provided = " + providedVersion + ", local = " + localVersion, e);
    }
    return provided.compareTo(local);
  }

  /**
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the plugin and the service.
   *
   * <p>Generates warnings into the log.
   *
   * @param pluginFactorySpi The plugin factory SPI.
   */
  private void checkPluginVersion(PluginFactorySpi pluginFactorySpi) {
    if (compareVersions(pluginFactorySpi.getCommonsApiVersion(), CommonsApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Commons API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          pluginFactorySpi.getPluginName(),
          pluginFactorySpi.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
    }
    if (compareVersions(pluginFactorySpi.getPluginApiVersion(), PluginApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Plugin API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          pluginFactorySpi.getPluginName(),
          pluginFactorySpi.getPluginApiVersion(),
          PluginApiProperties.VERSION);
    }
  }

  /**
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the pool plugin and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param poolPluginFactorySpi The plugin factory SPI.
   */
  private void checkPoolPluginVersion(PoolPluginFactorySpi poolPluginFactorySpi) {
    if (compareVersions(poolPluginFactorySpi.getCommonsApiVersion(), CommonsApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Commons API used by the provided pool plugin ({}:{}) mismatches the version used by the service ({}).",
          poolPluginFactorySpi.getPoolPluginName(),
          poolPluginFactorySpi.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
    }
    if (compareVersions(poolPluginFactorySpi.getPluginApiVersion(), PluginApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Plugin API used by the provided pool plugin ({}:{}) mismatches the version used by the service ({}).",
          poolPluginFactorySpi.getPoolPluginName(),
          poolPluginFactorySpi.getPluginApiVersion(),
          PluginApiProperties.VERSION);
    }
  }

  /**
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the card extension and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param cardExtensionSpi The card extension SPI.
   */
  private void checkCardExtensionVersion(CardExtensionSpi cardExtensionSpi) {
    if (compareVersions(cardExtensionSpi.getCommonsApiVersion(), CommonsApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Commons API used by the provided card extension ({}) mismatches the version used by the service ({}).",
          cardExtensionSpi.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
    }
    if (compareVersions(cardExtensionSpi.getCardApiVersion(), CardApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Card API used by the provided card extension ({}) mismatches the version used by the service ({}).",
          cardExtensionSpi.getCardApiVersion(),
          CardApiProperties.VERSION);
    }
    if (compareVersions(cardExtensionSpi.getServiceApiVersion(), ServiceApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Service API used by the provided card extension ({}) mismatches the version used by the service ({}).",
          cardExtensionSpi.getServiceApiVersion(),
          ServiceApiProperties.VERSION);
    }
  }

  /**
   * Checks if the plugin is already registered.
   *
   * @param pluginName The plugin name.
   * @throws IllegalStateException if the plugin is already registered.
   */
  private void checkPluginRegistration(String pluginName) {
    logger.info("Registering a new Plugin to the platform : {}", pluginName);
    if (isPluginRegistered(pluginName)) {
      throw new IllegalStateException(
          "Plugin has already been registered to the platform : " + pluginName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public Plugin registerPlugin(KeyplePluginExtensionFactory pluginFactory) {

    Assert.getInstance().notNull(pluginFactory, "pluginFactory");

    PluginAdapter<?> plugin;

    synchronized (monitor) {
      if (pluginFactory instanceof PluginFactorySpi) {
        PluginFactorySpi pluginFactorySpi = (PluginFactorySpi) pluginFactory;
        checkPluginRegistration(pluginFactorySpi.getPluginName());
        checkPluginVersion(pluginFactorySpi);
        PluginSpi pluginSpi = pluginFactorySpi.getPlugin();
        if (pluginSpi instanceof ObservablePluginSpi) {
          plugin = new ObservableLocalPluginAdapter((ObservablePluginSpi) pluginSpi);
        } else if (pluginSpi instanceof AutonomousObservablePluginSpi) {
          plugin =
              new AutonomousObservableLocalPluginAdapter((AutonomousObservablePluginSpi) pluginSpi);
        } else {
          plugin = new PluginAdapter<PluginSpi>(pluginSpi);
        }
      } else if (pluginFactory instanceof PoolPluginFactorySpi) {
        PoolPluginFactorySpi poolPluginFactorySpi = (PoolPluginFactorySpi) pluginFactory;
        checkPluginRegistration(poolPluginFactorySpi.getPoolPluginName());
        checkPoolPluginVersion(poolPluginFactorySpi);
        PoolPluginSpi poolPluginSpi = poolPluginFactorySpi.getPoolPlugin();
        plugin = new PoolPluginAdapter<PoolPluginSpi>(poolPluginSpi);
      } else {
        throw new IllegalArgumentException(
            "The provided plugin factory doesn't implement the plugin API properly.");
      }
      try {
        plugin.register();
      } catch (PluginIOException e) {
        throw new KeyplePluginException("Unable to register plugin " + plugin.getName(), e);
      }
      plugins.put(plugin.getName(), plugin);
    }
    return plugin;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void unregisterPlugin(String pluginName) {
    logger.info("Unregistering a plugin from the platform : {}", pluginName);
    synchronized (monitor) {
      Plugin removedPlugin = plugins.remove(pluginName);
      if (removedPlugin != null) {
        ((PluginAdapter<?>) removedPlugin).unregister();
      } else {
        logger.warn("The plugin {} is not registered", pluginName);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public boolean isPluginRegistered(String pluginName) {
    synchronized (monitor) {
      return plugins.containsKey(pluginName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public Map<String, Plugin> getPlugins() {
    synchronized (monitor) {
      return plugins;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public Plugin getPlugin(String pluginName) {
    synchronized (monitor) {
      Plugin plugin = plugins.get(pluginName);
      if (plugin == null) {
        throw new KeyplePluginNotFoundException(pluginName);
      }
      return plugin;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void checkCardExtension(KeypleCardExtension cardExtension) {
    checkCardExtensionVersion((CardExtensionSpi) cardExtension);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public DistributedLocalService registerDistributedLocalService(
      KeypleDistributedLocalServiceExtensionFactory distributedLocalServiceExtensionFactory) {
    // TODO complete
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void unregisterDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void getDistributedLocalService(String distributedLocalServiceName) {
    // TODO complete
  }
}
