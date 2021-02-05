package com.genersoft.iot.vmp.gb28181.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.ResponseEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SipSubscribe {

	private final static Logger logger = LoggerFactory.getLogger(SipSubscribe.class);

	private Map<String, SipSubscribe.Event> errorSubscribes = new ConcurrentHashMap<>();

	private Map<String, SipSubscribe.Event> okSubscribes = new ConcurrentHashMap<>();

	public interface Event {
		void response(ResponseEvent event);
	}

	public void addErrorSubscribe(String key, SipSubscribe.Event event) {
		errorSubscribes.put(key, event);
	}

	public void addOkSubscribe(String key, SipSubscribe.Event event) {
		okSubscribes.put(key, event);
	}

	public SipSubscribe.Event getErrorSubscribe(String key) {
		return errorSubscribes.get(key);
	}

	public SipSubscribe.Event getOkSubscribe(String key) {
		return okSubscribes.get(key);
	}

	public int getErrorSubscribesSize() {
		return errorSubscribes.size();
	}

	public int getOkSubscribesSize() {
		return okSubscribes.size();
	}
}
