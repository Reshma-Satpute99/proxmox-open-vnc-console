package com.inn.proxmox_vnc_api.service;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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

	@Value("${proxmox.public-host:192.168.1.4}")
	private String publicHost;

	private final RestTemplate restTemplate = new RestTemplate();

	public Map<String, Object> openVnc(String vmid) throws Exception {
		
		System.err.println("Inside Service API...");

		System.out.println("Step 1: Validating VMID...");
		if (vmid == null || !vmid.matches("\\d+")) {
			throw new IllegalArgumentException("Invalid ID");
		}

		// Step 2: Authenticate with Proxmox API
		String authUrl = String.format("https://%s:%d/api2/json/access/ticket", proxmoxHost, proxmoxPort);
		HttpHeaders authHeaders = new HttpHeaders();
		authHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> authBody = new LinkedMultiValueMap<>();
		authBody.add("username", username);
		authBody.add("password", password);
		HttpEntity<MultiValueMap<String, String>> authRequest = new HttpEntity<>(authBody, authHeaders);

		ResponseEntity<Map> authResponse = restTemplate.exchange(authUrl, HttpMethod.POST, authRequest, Map.class);

		if (authResponse.getStatusCode() != HttpStatus.OK || authResponse.getBody() == null) {
			throw new Exception("Authentication failed");
		}

		Map<String, Object> data = (Map<String, Object>) authResponse.getBody().get("data");
		String pveAuthCookie = (String) data.get("ticket");
		String csrfToken = (String) data.get("CSRFPreventionToken");

		if (pveAuthCookie == null || csrfToken == null) {
			throw new Exception("Authentication failed: missing cookie/token");
		}

		// Step 3: Search for VM or LXC container
		String[] nodes = proxmoxNodes.split(",");
		String resourceType = null;
		String vncUrl = null;
		String selectedNode = null;
		String resourceName = null;
		String vncTicket = null;

		for (String node : nodes) {
			node = node.trim();
			HttpHeaders headers = new HttpHeaders();
			headers.add("Cookie", "PVEAuthCookie=" + pveAuthCookie);
			headers.add("CSRFPreventionToken", csrfToken);

			// Check VM
			String vmUrl = String.format("https://%s:%d/api2/json/nodes/%s/qemu/%s/config", proxmoxHost, proxmoxPort,
					node, vmid);
			try {
				ResponseEntity<Map> vmResponse = restTemplate.exchange(vmUrl, HttpMethod.GET, new HttpEntity<>(headers),
						Map.class);
				if (vmResponse.getStatusCode() == HttpStatus.OK && vmResponse.getBody() != null
						&& vmResponse.getBody().get("data") != null) {
					Map<String, Object> vmData = (Map<String, Object>) vmResponse.getBody().get("data");
					String vmName = (String) vmData.get("name");

					if (vmName != null) {
						resourceType = "VM";
						selectedNode = node;
						resourceName = vmName;

						// Step 4: Get VNC ticket for VM
						String vncProxyUrl = String.format("https://%s:%d/api2/json/nodes/%s/qemu/%s/vncproxy",
								proxmoxHost, proxmoxPort, selectedNode, vmid);

						HttpHeaders vncHeaders = new HttpHeaders();
						vncHeaders.add("Cookie", "PVEAuthCookie=" + pveAuthCookie);
						vncHeaders.add("CSRFPreventionToken", csrfToken);
						vncHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

						HttpEntity<MultiValueMap<String, String>> vncRequest = new HttpEntity<>(
								new LinkedMultiValueMap<>(), vncHeaders);

						ResponseEntity<Map> vncResponse = restTemplate.exchange(vncProxyUrl, HttpMethod.POST,
								vncRequest, Map.class);

						if (vncResponse.getStatusCode() == HttpStatus.OK && vncResponse.getBody() != null
								&& vncResponse.getBody().get("data") != null) {
							Map<String, Object> vncData = (Map<String, Object>) vncResponse.getBody().get("data");
							vncTicket = (String) vncData.get("ticket");

							// Build noVNC URL — encode only safe parts
							vncUrl = String.format(
									"http://%s/?console=kvm&novnc=1&vmid=%s&vmname=%s&node=%s&resize=off&vncticket=%s",
									URLEncoder.encode(publicHost, "UTF-8"), URLEncoder.encode(vmid, "UTF-8"),
									URLEncoder.encode(resourceName, "UTF-8"), URLEncoder.encode(selectedNode, "UTF-8"),
									vncTicket // no encoding!
							);
						} else {
							throw new Exception("Failed to get VNC ticket");
						}
						break;
					}
				}
			} catch (Exception e) {
				System.out.println("VM check error: " + e.getMessage());
			}

			// Check LXC
			String lxcUrl = String.format("http://%s:%d/api2/json/nodes/%s/lxc/%s/config", proxmoxHost, proxmoxPort,
					node, vmid);
			try {
				ResponseEntity<Map> lxcResponse = restTemplate.exchange(lxcUrl, HttpMethod.GET,
						new HttpEntity<>(headers), Map.class);

				if (lxcResponse.getStatusCode() == HttpStatus.OK && lxcResponse.getBody() != null
						&& lxcResponse.getBody().get("data") != null) {
					Map<String, Object> lxcData = (Map<String, Object>) lxcResponse.getBody().get("data");
					String containerName = (String) lxcData.get("hostname");
					if (containerName != null) {
						resourceType = "LXC";
						selectedNode = node;
						resourceName = containerName;
						vncUrl = String.format("http://%s:8006/?console=lxc&xtermjs=1&vmid=%s&vmname=%s&node=%s",
								publicHost, vmid, resourceName, selectedNode);
						break;
					}
				}
			} catch (Exception e) {
				System.out.println("LXC check error: " + e.getMessage());
			}
		}

		if (resourceType == null) {
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
		// Corrected version — only pass the value, not full "key=value"
		response.put("cookie", pveAuthCookie); // Just the ticket

		if (vncTicket != null)
			response.put("vncticket", vncTicket); // Raw format

		return response;
	}
}

