package com.inn.proxmox_vnc_api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inn.proxmox_vnc_api.entity.LxcConfigResponse;
import com.inn.proxmox_vnc_api.entity.LxcContainer;
import com.inn.proxmox_vnc_api.response.AuthResponse;

@Service
public class NoVncService {

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

	@Value("${proxmox.public-host}")
	private String publicHost;

	private final RestTemplate restTemplate = new RestTemplate();;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public NoVncService() {
		super();
		// TODO Auto-generated constructor stub
	}

	public NoVncService(String username, String password, String proxmoxHost, String proxmoxNodes, int proxmoxPort,
			String publicHost) {
		super();
		this.username = username;
		this.password = password;
		this.proxmoxHost = proxmoxHost;
		this.proxmoxNodes = proxmoxNodes;
		this.proxmoxPort = proxmoxPort;
		this.publicHost = publicHost;
	}

	public Map<String, Object> openVnc(String vmidStr) throws Exception {
		System.err.println("Inside service api of novnc.!!!!");
		if (vmidStr == null || !vmidStr.chars().allMatch(Character::isDigit)) {
			throw new IllegalArgumentException("Invalid ID");
		}
		int vmid = Integer.parseInt(vmidStr);

		ResponseEntity<String> authResponse = authenticate();
		AuthResponse auth = parseAuthResponse(authResponse.getBody());
		System.out.println("Authenticated Response..." + auth);
		String node = findLxcNode(vmid, auth);
		System.out.println("Node : " + node);
		if (node == null) {
			throw new RuntimeException("Resource not found");
		}

		LxcContainer lxc = getLxcDetails(node, vmid, auth);

		System.out.println("LXC object received from getLxcDetails: " + lxc);
		if (lxc != null) {
			System.out.println("Hostname from LXC object: " + lxc.getHostname());
		} else {
			System.out.println("LXC object is null!");
		}

		String containerName = lxc.getHostname();
		if (containerName == null || containerName.isEmpty()) {
			throw new RuntimeException("LXC container not found");
		}


		String proxmoxHost = "https://192.168.192.57/don";
		String vncUrl = String.format("https://%s/?console=lxc&xtermjs=1&vmid=%d&vmname=%s&node=%s&cmd=",
				publicHost, vmid, containerName, node);

		System.err.println("Vnc-URL For LXC := " + vncUrl);

		Map<String, Object> result = new HashMap<>();
		result.put("cookie", auth.getTicket());
		result.put("host", proxmoxHost);
		result.put("vncUrl", vncUrl);
		System.out.println("Result := " + result);
		return result;
	}

	public ResponseEntity<String> authenticate() {
		System.err.println("Inside Authenticate API....");
		String url = String.format("https://%s:%d/api2/json/access/ticket", proxmoxHost, proxmoxPort);
		System.out.println("proxmoxurl = : " + url);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("username", username);
		body.add("password", password);

		System.out.println("Auth Request Body  " + body.get("username"));

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

		System.out.println("Proxmox Authentication Response: " + response);
		return response;
	}

	private AuthResponse parseAuthResponse(String json) throws Exception {
		System.err.println("Inside parseAuthResponse API ...");
		Map<String, Object> map = objectMapper.readValue(json, Map.class);

		System.out.println("Map := " + map);

		List<Entry<String, Object>> collect = map.entrySet().stream().collect(Collectors.toList());
		System.err.println(collect);

		Map<String, Object> data = (Map<String, Object>) map.get("data");

		AuthResponse response = new AuthResponse();
		response.setTicket((String) data.get("ticket"));
		response.setCSRFPreventionToken((String) data.get("CSRFPreventionToken"));
		System.err.println("Authenticated Response := " + response);
		return response;
	}

	private String findLxcNode(int vmid, AuthResponse auth) throws Exception {
		System.err.println("findLxcNode Called ... ");

		String[] nodes = proxmoxNodes.split(",");

		if (nodes.length == 0) {
			throw new IllegalArgumentException("No Proxmox nodes configured in proxmox.nodes property.");
		}

		for (String node : nodes) {
			String trimmedNode = node.trim();

			if (trimmedNode.isEmpty()) {
				continue;
			}

			String url = String.format("https://%s:%d/api2/json/nodes/%s/lxc/%d/config", proxmoxHost, proxmoxPort,
					trimmedNode, vmid);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Cookie", "PVEAuthCookie=" + auth.getTicket());
			headers.add("CSRFPreventionToken", auth.getCSRFPreventionToken());

			System.out.println("DEBUG: Checking node '" + trimmedNode + "' for vmid " + vmid + ". URL: " + url);
			System.out.println("DEBUG: Request Headers for node '" + trimmedNode + "': " + headers);

			try {
				ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
						String.class);
				if (resp.getStatusCodeValue() == 200) {
					System.out.println("DEBUG: Found vmid " + vmid + " on node '" + trimmedNode + "' (Status 200 OK)");
					return trimmedNode;
				} else {
					System.err.println("DEBUG: Node '" + trimmedNode + "' returned non-200 status: "
							+ resp.getStatusCodeValue() + ". Response: " + resp.getBody());
				}
			} catch (HttpClientErrorException e) {
				System.err.println("DEBUG: Error fetching node '" + trimmedNode + "' (HTTP Client Error): HTTP "
						+ e.getStatusCode().value() + " - " + e.getResponseBodyAsString());
			} catch (HttpServerErrorException e) {
				System.err.println("DEBUG: Proxmox Server Error for node '" + trimmedNode + "': HTTP "
						+ e.getStatusCode().value() + " - " + e.getResponseBodyAsString());
			} catch (ResourceAccessException e) {
				System.err.println("DEBUG: Network/Connection Error for node '" + trimmedNode + "': " + e.getMessage());
			} catch (Exception e) {
				System.err.println("DEBUG: Unexpected Error fetching node '" + trimmedNode + "': "
						+ e.getClass().getSimpleName() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}
		System.err.println("DEBUG: findLxcNode returning null (vmid " + vmid
				+ " not found on any configured node, or error occurred for all).");
		return null;
	}

	private LxcContainer getLxcDetails(String node, int vmid, AuthResponse auth) throws Exception {
		System.err.println("getLxcDetails Called...");
		String url = String.format("https://%s:%d/api2/json/nodes/%s/lxc/%d/config", proxmoxHost, proxmoxPort, node,
				vmid);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cookie", "PVEAuthCookie=" + auth.getTicket());
		headers.add("CSRFPreventionToken", auth.getCSRFPreventionToken());

		ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
				String.class);
		System.err.println("Response from getLxcDetails..." + resp);
		LxcConfigResponse lxcConfigResponse = objectMapper.readValue(resp.getBody(), LxcConfigResponse.class);
		System.out.println("" + lxcConfigResponse.getData().getHostname());
		return lxcConfigResponse.getData();
	}
}
