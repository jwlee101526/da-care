output "public_ip" {
  value       = oci_core_public_ip.app.ip_address
  description = "DNS A 레코드에 연결할 고정 공인 IP"
}

output "backup_bucket" {
  value = oci_objectstorage_bucket.backup.name
}
