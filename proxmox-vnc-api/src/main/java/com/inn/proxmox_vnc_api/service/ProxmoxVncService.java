package com.inn.proxmox_vnc_api.service;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class ProxmoxVncService {

	@Value("${proxmox.username}")
	private String username;

	@Value("${proxmox.password}")
	private String password;

	@Value("${proxmox.host}")
	private String proxmoxHost;

	@Value("${proxmox.nodes}")
	private String proxmoxNodes;

	@Value("${proxmox.port:8006}")
	private int proxmoxPort;

	@Value("${proxmox.public-host:192.168.1.8}")
	private String publicHost;

	private final RestTemplate restTemplate = new RestTemplate();

	public Map<String, Object> openVnc(String vmid) throws Exception {

		System.out.println(" Entering openVnc method...");
		System.out.println("Provided VMID: " + vmid);

		System.out.println("Step 1: Validating VMID...");
		if (vmid == null || !vmid.matches("\\d+")) {
			System.err.println(" Invalid VMID provided.");
			throw new IllegalArgumentException("Invalid ID");
		}
		System.out.println("VMID is valid.");

		// Step 2: Authenticate with Proxmox API
		String authUrl = String.format("https://%s:%d/api2/json/access/ticket", proxmoxHost, proxmoxPort);
		System.out.println(" Authentication URL: " + authUrl);

		HttpHeaders authHeaders = new HttpHeaders();
		authHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> authBody = new LinkedMultiValueMap<>();
		authBody.add("username", username);
		authBody.add("password", password);
		System.out.println(" Sending authentication request...");

		HttpEntity<MultiValueMap<String, String>> authRequest = new HttpEntity<>(authBody, authHeaders);

		ResponseEntity<Map> authResponse = restTemplate.exchange(authUrl, HttpMethod.POST, authRequest, Map.class);

		System.out.println(" Authentication response status: " + authResponse.getStatusCode());
		if (authResponse.getStatusCode() != HttpStatus.OK || authResponse.getBody() == null) {
			System.err.println("ERROR: Authentication failed.");
			throw new Exception("Authentication failed");
		}

		Map<String, Object> data = (Map<String, Object>) authResponse.getBody().get("data");
		String pveAuthCookie = (String) data.get("ticket");
		String csrfToken = (String) data.get("CSRFPreventionToken");

		System.out.println("PVE Auth Cookie: " + pveAuthCookie);
		System.out.println(" CSRF Token: " + csrfToken);

		if (pveAuthCookie == null || csrfToken == null) {
			System.err.println("ERROR: Missing cookie or token from authentication.");
			throw new Exception("Authentication failed: missing cookie/token");
		}

		// Step 3: Searching for VM or LXC container
		String[] nodes = proxmoxNodes.split(",");
		System.out.println("Nodes to check: ");
		for (String node : nodes) {
			System.out.println(" - " + node.trim());
		}

		String resourceType = null;
		String vncUrl = null;
		String selectedNode = null;
		String resourceName = null;
		String vncTicket = null;

		for (String node : nodes) {
			node = node.trim();
			System.out.println(" Checking node: " + node);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Cookie", "PVEAuthCookie=" + pveAuthCookie);
			headers.add("CSRFPreventionToken", csrfToken);

			// Check VM
			String vmUrl = String.format("https://%s:%d/api2/json/nodes/%s/qemu/%s/config", proxmoxHost, proxmoxPort,
					node, vmid);
			System.out.println(" Checking VM config URL: " + vmUrl);

			try {
				ResponseEntity<Map> vmResponse = restTemplate.exchange(vmUrl, HttpMethod.GET, new HttpEntity<>(headers),
						Map.class);
				System.out.println(" VM config response status: " + vmResponse.getStatusCode());

				if (vmResponse.getStatusCode() == HttpStatus.OK && vmResponse.getBody() != null
						&& vmResponse.getBody().get("data") != null) {
					Map<String, Object> vmData = (Map<String, Object>) vmResponse.getBody().get("data");
					System.out.println("DEBUG: VM Data: " + vmData);

					String vmName = (String) vmData.get("name");
					System.out.println("DEBUG: VM Name found: " + vmName);

					if (vmName != null) {
						resourceType = "VM";
						selectedNode = node;
						resourceName = vmName;

						// Step 4: Get VNC ticket for VM
						String vncProxyUrl = String.format("https://%s:%d/api2/json/nodes/%s/qemu/%s/vncproxy",
								proxmoxHost, proxmoxPort, selectedNode, vmid);
						System.out.println(" Fetching VNC proxy URL: " + vncProxyUrl);

						HttpHeaders vncHeaders = new HttpHeaders();
						vncHeaders.add("Cookie", "PVEAuthCookie=" + pveAuthCookie);
						vncHeaders.add("CSRFPreventionToken", csrfToken);
						vncHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

						HttpEntity<MultiValueMap<String, String>> vncRequest = new HttpEntity<>(
								new LinkedMultiValueMap<>(), vncHeaders);

						ResponseEntity<Map> vncResponse = restTemplate.exchange(vncProxyUrl, HttpMethod.POST,
								vncRequest, Map.class);

						System.out.println(" VNC proxy response status: " + vncResponse.getStatusCode());

						if (vncResponse.getStatusCode() == HttpStatus.OK && vncResponse.getBody() != null
								&& vncResponse.getBody().get("data") != null) {
							Map<String, Object> vncData = (Map<String, Object>) vncResponse.getBody().get("data");
							vncTicket = (String) vncData.get("ticket");
							System.out.println(" VNC Ticket received: " + vncTicket);

							// Build noVNC URL
							vncUrl = String.format(
									"http://%s/?console=kvm&novnc=1&vmid=%s&vmname=%s&node=%s&resize=off&vncticket=%s",
									URLEncoder.encode(publicHost, "UTF-8"), URLEncoder.encode(vmid, "UTF-8"),
									URLEncoder.encode(resourceName, "UTF-8"), URLEncoder.encode(selectedNode, "UTF-8"),
									vncTicket);
							System.out.println(" Generated VNC URL: " + vncUrl);
						} else {
							System.err.println(" Failed to get VNC ticket.");
							throw new Exception("Failed to get VNC ticket");
						}
						break;
					}
				}
			} catch (Exception e) {
				System.err.println(" VM check error on node [" + node + "]: " + e.getMessage());
			}

			// Check LXC
			String lxcUrl = String.format("http://%s:%d/api2/json/nodes/%s/lxc/%s/config", proxmoxHost, proxmoxPort,
					node, vmid);
			System.out.println(" Checking LXC config URL: " + lxcUrl);

			try {
				ResponseEntity<Map> lxcResponse = restTemplate.exchange(lxcUrl, HttpMethod.GET,
						new HttpEntity<>(headers), Map.class);

				System.out.println(" LXC config response status: " + lxcResponse.getStatusCode());

				if (lxcResponse.getStatusCode() == HttpStatus.OK && lxcResponse.getBody() != null
						&& lxcResponse.getBody().get("data") != null) {
					Map<String, Object> lxcData = (Map<String, Object>) lxcResponse.getBody().get("data");
					System.out.println(" LXC Data: " + lxcData);

					String containerName = (String) lxcData.get("hostname");
					System.out.println(" Container name found: " + containerName);

					if (containerName != null) {
						resourceType = "LXC";
						selectedNode = node;
						resourceName = containerName;
						vncUrl = String.format("http://%s:8006/?console=lxc&xtermjs=1&vmid=%s&vmname=%s&node=%s",
								publicHost, vmid, resourceName, selectedNode);
						System.out.println(" Generated LXC VNC URL: " + vncUrl);
						break;
					}
				}
			} catch (Exception e) {
				System.err.println(" LXC check error on node [" + node + "]: " + e.getMessage());
			}
		}

		if (resourceType == null) {
			System.err.println(" Resource not found (neither VM nor LXC container matched the VMID).");
			throw new NoSuchElementException("Resource not found");
		}

		// Step 5: Return the response map with raw cookie and ticket
		Map<String, Object> response = new HashMap<>();
		response.put("host", publicHost);
		response.put("vncUrl", vncUrl);
		response.put("type", resourceType);
		response.put("node", selectedNode);
		response.put("name", resourceName);
		response.put("vmid", vmid);
		response.put("cookie", pveAuthCookie);

		if (vncTicket != null)
			response.put("vncticket", vncTicket);

		System.out.println(" Final response built successfully:");
		System.out.println(response);

		return response;
	}
}