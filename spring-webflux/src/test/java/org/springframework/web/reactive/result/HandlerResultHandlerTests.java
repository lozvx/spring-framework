/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;
import static org.springframework.http.MediaType.*;

/**
 * Unit tests for {@link AbstractHandlerResultHandler}.
 * @author Rossen Stoyanchev
 */
public class HandlerResultHandlerTests {

	private TestResultHandler resultHandler;

	private MockServerHttpRequest request;


	@Before
	public void setup() throws Exception {
		this.resultHandler = new TestResultHandler();
		this.request = MockServerHttpRequest.get("/path").build();
	}


	@Test
	public void usesContentTypeResolver() throws Exception {
		TestResultHandler resultHandler = new TestResultHandler(new FixedContentTypeResolver(IMAGE_GIF));
		List<MediaType> mediaTypes = Arrays.asList(IMAGE_JPEG, IMAGE_GIF, IMAGE_PNG);
		MediaType actual = resultHandler.selectMediaType(exchange(), () -> mediaTypes);

		assertEquals(IMAGE_GIF, actual);
	}

	@Test
	public void producibleMediaTypesRequestAttribute() throws Exception {
		ServerWebExchange exchange = exchange();
		exchange.getAttributes().put(
				HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(IMAGE_GIF));

		List<MediaType> mediaTypes = Arrays.asList(IMAGE_JPEG, IMAGE_GIF, IMAGE_PNG);
		MediaType actual = resultHandler.selectMediaType(exchange, () -> mediaTypes);

		assertEquals(IMAGE_GIF, actual);
	}

	@Test  // SPR-9160
	public void sortsByQuality() throws Exception {
		this.request = MockServerHttpRequest.get("/path")
				.header("Accept", "text/plain; q=0.5, application/json")
				.build();

		List<MediaType> mediaTypes = Arrays.asList(TEXT_PLAIN, APPLICATION_JSON_UTF8);
		MediaType actual = this.resultHandler.selectMediaType(exchange(), () -> mediaTypes);

		assertEquals(APPLICATION_JSON_UTF8, actual);
	}

	@Test
	public void charsetFromAcceptHeader() throws Exception {
		MediaType text8859 = MediaType.parseMediaType("text/plain;charset=ISO-8859-1");
		MediaType textUtf8 = MediaType.parseMediaType("text/plain;charset=UTF-8");
		this.request = MockServerHttpRequest.get("/path").accept(text8859).build();
		MediaType actual = this.resultHandler.selectMediaType(exchange(), () -> Collections.singletonList(textUtf8));

		assertEquals(text8859, actual);
	}

	@Test // SPR-12894
	public void noConcreteMediaType() throws Exception {
		List<MediaType> producible = Collections.singletonList(ALL);
		MediaType actual = this.resultHandler.selectMediaType(exchange(), () -> producible);

		assertEquals(APPLICATION_OCTET_STREAM, actual);
	}


	private DefaultServerWebExchange exchange() {
		return new DefaultServerWebExchange(this.request, new MockServerHttpResponse());
	}


	@SuppressWarnings("WeakerAccess")
	private static class TestResultHandler extends AbstractHandlerResultHandler {

		protected TestResultHandler() {
			this(new HeaderContentTypeResolver());
		}

		public TestResultHandler(RequestedContentTypeResolver contentTypeResolver) {
			super(contentTypeResolver);
		}
	}

}
