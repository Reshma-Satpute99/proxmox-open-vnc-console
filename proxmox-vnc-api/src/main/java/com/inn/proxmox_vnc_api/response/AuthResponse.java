package com.inn.proxmox_vnc_api.response;

public class AuthResponse {

	private String ticket;
	private String CSRFPreventionToken;

	public AuthResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public AuthResponse(String ticket, String cSRFPreventionToken) {
		super();
		this.ticket = ticket;
		CSRFPreventionToken = cSRFPreventionToken;
	}

	public String getTicket() {
		return ticket;
	}

	public void setTicket(String ticket) {
		this.ticket = ticket;
	}

	public String getCSRFPreventionToken() {
		return CSRFPreventionToken;
	}

	public void setCSRFPreventionToken(String cSRFPreventionToken) {
		CSRFPreventionToken = cSRFPreventionToken;
	}

	@Override
	public String toString() {
		return "AuthResponse [ticket=" + ticket + ", CSRFPreventionToken=" + CSRFPreventionToken + "]";
	}

}
