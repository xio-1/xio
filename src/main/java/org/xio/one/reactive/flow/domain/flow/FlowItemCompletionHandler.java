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

package org.xio.one.reactive.flow.domain.flow;

public interface FlowItemCompletionHandler<R, A> {

  /**
   * Invoked when an operation has completed.
   *
   * @param result     The getFutureResult of the async operation.
   * @param attachment The object attached to the async operation when it was initiated.
   */
  void completed(R result, A attachment);

  /**
   * Invoked when an operation fails.
   *
   * @param exc        The exception to indicate why the I/O operation failed
   * @param attachment The object attached to the I/O operation when it was initiated.
   */
  void failed(Throwable exc, A attachment);
}
