package com.inn.proxmox_vnc_api.entity;

//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;

//@Entity
public class ProxmoxResources {
   // @Id
	private String vmid;
	private String name;
	private String node;
	private String type;

	public ProxmoxResources() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ProxmoxResources(String vmid, String name, String node, String type) {
		super();
		this.vmid = vmid;
		this.name = name;
		this.node = node;
		this.type = type;
	}

	public String getVmid() {
		return vmid;
	}

	public void setVmid(String vmid) {
		this.vmid = vmid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "ProxmoxResources [vmid=" + vmid + ", name=" + name + ", node=" + node + ", type=" + type + "]";
	}

}
