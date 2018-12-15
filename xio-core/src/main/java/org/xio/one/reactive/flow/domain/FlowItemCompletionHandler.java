/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.xio.one.reactive.flow.domain;

public interface FlowItemCompletionHandler<R, T> {

  /**
   * Invoked when an operation has completed.
   *
   * @param result     The result of the async operation.
   * @param attachment The object attached to the async operation when it was initiated.
   */
  void completed(R result, T attachment);

  /**
   * Invoked when an operation fails.
   *
   * @param exc        The exception to indicate why the I/O operation failed
   * @param attachment The object attached to the I/O operation when it was initiated.
   */
  void failed(Throwable exc, T attachment);
}
