output "public_ip" {
  value       = oci_core_public_ip.production.ip_address
  description = "GitHub DEPLOY_HOST와 DNS A 레코드에 사용할 고정 공인 IP"
}
