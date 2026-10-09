package com.project.multi_agent_ai_platform.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.config.SecurityConfig;

/**
 * For {@code @WebMvcTest} slices: the real {@link SecurityConfig}, with every request running as a
 * signed-in user, so controller tests keep testing their controller. Signing in itself, and the
 * {@code 401} for every route, are covered once in {@code AuthIntegrationTest}.
 */
@TestConfiguration
@Import(SecurityConfig.class)
@EnableConfigurationProperties(PlatformProperties.class)
public class SignedInWebTest {

	@Bean
	MockMvcBuilderCustomizer signedInByDefault() {
		return builder -> builder.defaultRequest(get("/").with(user("tester")));
	}
}
