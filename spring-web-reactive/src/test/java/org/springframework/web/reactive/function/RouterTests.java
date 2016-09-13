/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 */
@SuppressWarnings("unchecked")
public class RouterTests {

	@Test
	public void routeMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> Response.ok().build();

		MockRequest request = MockRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(true);

		RoutingFunction<Void> result = RoutingFunctions.route(requestPredicate, handlerFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void routeNoMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> Response.ok().build();

		MockRequest request = MockRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		RoutingFunction<Void> result = RoutingFunctions.route(requestPredicate, handlerFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertFalse(resultHandlerFunction.isPresent());
	}

	@Test
	public void subrouteMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> Response.ok().build();
		RoutingFunction<Void> routingFunction = request -> Optional.of(handlerFunction);

		MockRequest request = MockRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(true);

		RoutingFunction<Void> result = RoutingFunctions.subroute(requestPredicate, routingFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void subrouteNoMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> Response.ok().build();
		RoutingFunction<Void> routingFunction = request -> Optional.of(handlerFunction);

		MockRequest request = MockRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		RoutingFunction<Void> result = RoutingFunctions.subroute(requestPredicate, routingFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertFalse(resultHandlerFunction.isPresent());
	}

	@Test
	public void toHttpHandler() throws Exception {
		Request request = mock(Request.class);
		Response response = mock(Response.class);
		when(response.writeTo(any(ServerWebExchange.class))).thenReturn(Mono.empty());

		HandlerFunction handlerFunction = mock(HandlerFunction.class);
		when(handlerFunction.handle(any(Request.class))).thenReturn(response);

		RoutingFunction routingFunction = mock(RoutingFunction.class);
		when(routingFunction.route(any(Request.class))).thenReturn(Optional.of(handlerFunction));

		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		Configuration configuration = mock(Configuration.class);
		when(configuration.messageReaders()).thenReturn(
				() -> Collections.<HttpMessageReader<?>>emptyList().stream());
		when(configuration.messageWriters()).thenReturn(
				() -> Collections.<HttpMessageWriter<?>>emptyList().stream());
		when(configuration.viewResolvers()).thenReturn(
				() -> Collections.<ViewResolver>emptyList().stream());

		HttpHandler result = RoutingFunctions.toHttpHandler(routingFunction, configuration);
		assertNotNull(result);

		MockServerHttpRequest httpRequest =
				new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse serverHttpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, serverHttpResponse);
	}

}
