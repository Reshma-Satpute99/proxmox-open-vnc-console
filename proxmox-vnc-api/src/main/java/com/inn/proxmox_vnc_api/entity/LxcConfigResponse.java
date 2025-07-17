package com.inn.proxmox_vnc_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LxcConfigResponse {

	@JsonProperty("data")
	private LxcContainer data;

	public LxcConfigResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LxcConfigResponse(LxcContainer data) {
		super();
		this.data = data;
	}

	public LxcContainer getData() {
		return data;
	}

	public void setData(LxcContainer data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "LxcConfigResponse [data=" + data + "]";
	}

}
