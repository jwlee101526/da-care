locals {
  name_prefix        = "dacare-${var.environment}"
  dynamic_group_name = "dacare-${var.environment}-instances"
  backup_bucket_name = "dacare-${var.environment}-backups"
}

resource "oci_core_vcn" "this" {
  compartment_id = var.compartment_id
  display_name   = "${local.name_prefix}-vcn"
  cidr_block     = var.environment == "staging" ? "10.20.0.0/16" : "10.30.0.0/16"
  dns_label      = "dacare${var.environment == "staging" ? "stg" : "prd"}"
}

resource "oci_core_internet_gateway" "this" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.this.id
  display_name   = "${local.name_prefix}-igw"
  enabled        = true
}

resource "oci_core_route_table" "public" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.this.id
  display_name   = "${local.name_prefix}-public-rt"
  route_rules {
    network_entity_id = oci_core_internet_gateway.this.id
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
  }
}

resource "oci_core_security_list" "public" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.this.id
  display_name   = "${local.name_prefix}-public-sl"

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

resource "oci_core_subnet" "public" {
  compartment_id             = var.compartment_id
  vcn_id                     = oci_core_vcn.this.id
  display_name               = "${local.name_prefix}-public-subnet"
  cidr_block                 = var.environment == "staging" ? "10.20.1.0/24" : "10.30.1.0/24"
  route_table_id             = oci_core_route_table.public.id
  security_list_ids          = [oci_core_security_list.public.id]
  prohibit_public_ip_on_vnic = true
  dns_label                  = "app"
}

resource "oci_identity_dynamic_group" "instance" {
  compartment_id = var.tenancy_ocid
  name           = local.dynamic_group_name
  description    = "${var.environment} DA-Care VM의 Vault와 백업 접근 권한"
  matching_rule  = "ALL {instance.compartment.id = '${var.compartment_id}', instance.displayName = '${local.name_prefix}-vm'}"
}

resource "oci_identity_policy" "instance" {
  compartment_id = var.tenancy_ocid
  name           = "dacare-${var.environment}-instance-policy"
  description    = "${var.environment} VM 최소 권한"
  statements = [
    "Allow dynamic-group ${oci_identity_dynamic_group.instance.name} to read secret-bundles in compartment id ${var.vault_compartment_id}",
    "Allow dynamic-group ${oci_identity_dynamic_group.instance.name} to manage objects in compartment id ${var.compartment_id} where target.bucket.name='${local.backup_bucket_name}'"
  ]
}

resource "oci_objectstorage_bucket" "backup" {
  compartment_id        = var.compartment_id
  namespace             = data.oci_objectstorage_namespace.current.namespace
  name                  = local.backup_bucket_name
  access_type           = "NoPublicAccess"
  versioning            = "Disabled"
  object_events_enabled = false
}

resource "oci_objectstorage_object_lifecycle_policy" "backup" {
  bucket    = oci_objectstorage_bucket.backup.name
  namespace = data.oci_objectstorage_namespace.current.namespace

  rules {
    action      = "DELETE"
    is_enabled  = true
    name        = "expire-old-backups"
    target      = "objects"
    time_amount = var.backup_retention_days
    time_unit   = "DAYS"
  }
}

resource "oci_core_instance" "app" {
  availability_domain = data.oci_identity_availability_domains.available.availability_domains[0].name
  compartment_id      = var.compartment_id
  display_name        = "${local.name_prefix}-vm"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = 1
    memory_in_gbs = 6
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.public.id
    assign_public_ip = false
    display_name     = "${local.name_prefix}-vnic"
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.ubuntu.images[0].id
  }

  metadata = {
    ssh_authorized_keys = var.admin_ssh_public_key
    user_data = base64encode(templatefile("${path.module}/cloud-init.yaml.tftpl", {
      vault_secret_ocid     = var.vault_secret_ocid
      compose_file          = filebase64("${path.module}/../production/docker-compose.prod.yml")
      caddyfile             = filebase64("${path.module}/../production/Caddyfile")
      deploy_script         = filebase64("${path.module}/../production/deploy.sh")
      backup_script         = filebase64("${path.module}/../production/backup.sh")
      deploy_ssh_public_key = var.deploy_ssh_public_key
    }))
  }
}

data "oci_core_vnic_attachments" "app" {
  compartment_id = var.compartment_id
  instance_id    = oci_core_instance.app.id
}

data "oci_core_private_ips" "app" {
  vnic_id = data.oci_core_vnic_attachments.app.vnic_attachments[0].vnic_id
}

resource "oci_core_public_ip" "app" {
  compartment_id = var.compartment_id
  display_name   = "${local.name_prefix}-ip"
  lifetime       = "RESERVED"
  private_ip_id  = data.oci_core_private_ips.app.private_ips[0].id
}
