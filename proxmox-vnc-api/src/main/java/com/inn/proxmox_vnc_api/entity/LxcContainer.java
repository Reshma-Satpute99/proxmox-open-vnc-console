package com.inn.proxmox_vnc_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LxcContainer {

	@JsonProperty("hostname")
	private String hostname;

	public LxcContainer() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LxcContainer(String hostname) {
		super();
		this.hostname = hostname;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public String toString() {
		return "LxcContainer [hostname=" + hostname + "]";
	}

}
