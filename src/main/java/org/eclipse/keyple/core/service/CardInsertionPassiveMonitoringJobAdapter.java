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

import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.TaskCanceledException;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionBlockingSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detect the card insertion thanks to the method {@link
 * WaitForCardInsertionBlockingSpi#waitForCardPresent()}.
 *
 * <p>This method is invoked in another thread.
 *
 * <p>The job waits indefinitely for the waitForCardPresent method to return unless the {@link
 * #stop()} method is invoked. In this case, the job is aborted.
 *
 * <p>When a card is present, an internal CARD_INSERTED event is fired.
 *
 * <p>All runtime exceptions that may occur during the monitoring process are caught and notified at
 * the application level through the appropriate exception handler.
 *
 * @since 2.0
 */
class CardInsertionPassiveMonitoringJobAdapter extends AbstractMonitoringJob {

  private static final Logger logger =
      LoggerFactory.getLogger(CardInsertionPassiveMonitoringJobAdapter.class);

  private final WaitForCardInsertionBlockingSpi readerSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param reader The reader.
   */
  CardInsertionPassiveMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    super(reader);
    this.readerSpi = (WaitForCardInsertionBlockingSpi) reader.getObservableReaderSpi();
  }

  /** {@inheritDoc} */
  @Override
  Runnable getMonitoringJob(final AbstractObservableStateAdapter state) {
    /*
     * Invoke the method WaitForCardInsertionBlocking#waitForCardPresent() in another thread
     */
    return new Runnable() {
      @Override
      public void run() {
        try {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              readerSpi.waitForCardPresent();
              state.onEvent(ObservableLocalReaderAdapter.InternalEvent.CARD_INSERTED);
              break;
            } catch (ReaderIOException e) {
              // TODO check this
              // just warn as it can be a disconnection of the reader.
              logger.warn(
                  "[{}] waitForCardPresent => Error while processing card insertion event",
                  getReader().getName());
            } catch (TaskCanceledException e) {
              break;
            }
          }
        } catch (RuntimeException e) {
          getReader()
              .getObservationExceptionHandler()
              .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
        }
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  void stop() {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] stopWaitForCard on reader", getReader().getName());
    }
    readerSpi.stopWaitForCard();
  }
}