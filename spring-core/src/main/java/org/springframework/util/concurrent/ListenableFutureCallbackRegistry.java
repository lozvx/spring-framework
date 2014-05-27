/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.util.concurrent;

import java.util.LinkedList;
import java.util.Queue;

import org.springframework.util.Assert;

/**
 * Registry for {@link ListenableFutureCallback} instances.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ExecutionList}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class ListenableFutureCallbackRegistry<T> {

	private final Queue<SuccessCallback<? super T>> successCallbacks =
			new LinkedList<SuccessCallback<? super T>>();

	private final Queue<FailureCallback> failureCallbacks =
			new LinkedList<FailureCallback>();

	private State state = State.NEW;

	private Object result = null;

	private final Object mutex = new Object();


	/**
	 * Adds the given callback to this registry.
	 * @param callback the callback to add
	 */
	@SuppressWarnings("unchecked")
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");

		synchronized (mutex) {
			switch (state) {
				case NEW:
					successCallbacks.add(callback);
					failureCallbacks.add(callback);
					break;
				case SUCCESS:
					callback.onSuccess((T)result);
					break;
				case FAILURE:
					callback.onFailure((Throwable) result);
					break;
			}
		}
	}

	/**
	 * Adds the given success callback to this registry.
	 * @param callback the success callback to add
	 *
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public void addSuccessCallback(SuccessCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");

		synchronized (mutex) {
			switch (state) {
				case NEW:
					successCallbacks.add(callback);
					break;
				case SUCCESS:
					callback.onSuccess((T)result);
					break;
			}
		}
	}

	/**
	 * Adds the given failure callback to this registry.
	 * @param callback the failure callback to add
	 *
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public void addFailureCallback(FailureCallback callback) {
		Assert.notNull(callback, "'callback' must not be null");

		synchronized (mutex) {
			switch (state) {
				case NEW:
					failureCallbacks.add(callback);
					break;
				case FAILURE:
					callback.onFailure((Throwable) result);
					break;
			}
		}
	}

	/**
	 * Triggers a {@link ListenableFutureCallback#onSuccess(Object)} call on all added
	 * callbacks with the given result
	 * @param result the result to trigger the callbacks with
	 */
	public void success(T result) {
		synchronized (mutex) {
			state = State.SUCCESS;
			this.result = result;

			while (!successCallbacks.isEmpty()) {
				successCallbacks.poll().onSuccess(result);
			}
		}
	}

	/**
	 * Triggers a {@link ListenableFutureCallback#onFailure(Throwable)} call on all added
	 * callbacks with the given {@code Throwable}.
	 * @param t the exception to trigger the callbacks with
	 */
	public void failure(Throwable t) {
		synchronized (mutex) {
			state = State.FAILURE;
			this.result = t;

			while (!failureCallbacks.isEmpty()) {
				failureCallbacks.poll().onFailure(t);
			}
		}
	}

	private enum State {NEW, SUCCESS, FAILURE}

}
