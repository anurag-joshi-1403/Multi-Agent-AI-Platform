package com.project.multi_agent_ai_platform.agent.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Looks up agents by id.
 * <p>
 * Spring injects <em>every</em> bean implementing {@link Agent} into the constructor's
 * {@code List<Agent>}; that is the only wiring needed to register a new agent.
 * Duplicate ids fail at startup rather than silently shadowing each other.
 */
@Component
public class AgentRegistry {

	private final Map<String, Agent> byId;

	public AgentRegistry(List<Agent> agents) {
		Map<String, Agent> map = new LinkedHashMap<>();
		for (Agent agent : agents) {
			Agent previous = map.putIfAbsent(agent.id(), agent);
			if (previous != null) {
				throw new IllegalStateException("Duplicate agent id '" + agent.id() + "': "
						+ previous.getClass().getName() + " and " + agent.getClass().getName());
			}
		}
		// unmodifiableMap keeps registration order (Map.copyOf would not), so listings are stable
		this.byId = Collections.unmodifiableMap(map);
	}

	public Agent get(String id) {
		Agent agent = byId.get(id);
		if (agent == null) {
			throw new UnknownAgentException(id);
		}
		return agent;
	}

	public boolean contains(String id) {
		return byId.containsKey(id);
	}

	public Collection<Agent> all() {
		return byId.values();
	}
}
