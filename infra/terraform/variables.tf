variable "region" { type = string }
variable "tenancy_ocid" {
  type      = string
  sensitive = true
}
variable "compartment_id" {
  type      = string
  sensitive = true
}
variable "user_ocid" {
  type      = string
  sensitive = true
}
variable "api_key_fingerprint" {
  type      = string
  sensitive = true
}
variable "api_private_key_path" {
  type      = string
  sensitive = true
}
variable "ssh_public_key" { type = string }
variable "admin_cidr" { type = string }
