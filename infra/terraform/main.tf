resource "oci_core_vcn" "production" {
  compartment_id = var.compartment_id
  display_name   = "dacare-production-vcn"
  cidr_block     = "10.30.0.0/16"
  dns_label      = "dacareprod"
}

resource "oci_core_internet_gateway" "production" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.production.id
  display_name   = "dacare-production-igw"
  enabled        = true
}

resource "oci_core_route_table" "production" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.production.id
  display_name   = "dacare-production-route-table"

  route_rules {
    network_entity_id = oci_core_internet_gateway.production.id
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
  }
}

resource "oci_core_security_list" "production" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.production.id
  display_name   = "dacare-production-security-list"

  ingress_security_rules {
    protocol = "6"
    source   = var.admin_cidr
    tcp_options {
      min = 22
      max = 22
    }
  }
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 80
      max = 80
    }
  }
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 443
      max = 443
    }
  }
  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }
}

resource "oci_core_subnet" "production" {
  compartment_id    = var.compartment_id
  vcn_id            = oci_core_vcn.production.id
  display_name      = "dacare-production-subnet"
  cidr_block        = "10.30.1.0/24"
  route_table_id    = oci_core_route_table.production.id
  security_list_ids = [oci_core_security_list.production.id]
  dns_label         = "app"
}

resource "oci_core_instance" "production" {
  lifecycle {
    prevent_destroy = true
    ignore_changes  = [source_details[0].source_id]
  }

  availability_domain = data.oci_identity_availability_domains.available.availability_domains[0].name
  compartment_id      = var.compartment_id
  display_name        = "dacare-production"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = 1
    memory_in_gbs = 6
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.production.id
    assign_public_ip = false
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.ubuntu.images[0].id
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data = base64encode(<<-CLOUD_INIT
      #cloud-config
      package_update: true
      packages:
        - docker.io
        - docker-compose-v2
        - curl
      runcmd:
        - systemctl enable --now docker
        - sed -ri 's/^#?PasswordAuthentication .*/PasswordAuthentication no/' /etc/ssh/sshd_config
        - sed -ri 's/^#?PermitRootLogin .*/PermitRootLogin no/' /etc/ssh/sshd_config
        - systemctl reload ssh
    CLOUD_INIT
    )
  }
}

data "oci_core_vnic_attachments" "production" {
  compartment_id = var.compartment_id
  instance_id    = oci_core_instance.production.id
}

data "oci_core_private_ips" "production" {
  vnic_id = data.oci_core_vnic_attachments.production.vnic_attachments[0].vnic_id
}

resource "oci_core_public_ip" "production" {
  lifecycle {
    prevent_destroy = true
  }

  compartment_id = var.compartment_id
  display_name   = "dacare-production-ip"
  lifetime       = "RESERVED"
  private_ip_id  = data.oci_core_private_ips.production.private_ips[0].id
}
