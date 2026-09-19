variable "environment" {
  type        = string
  description = "staging 또는 production"
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment는 staging 또는 production이어야 합니다."
  }
}

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
variable "admin_ssh_public_key" { type = string }
variable "deploy_ssh_public_key" { type = string }
variable "admin_cidr" {
  type        = string
  description = "SSH를 허용할 단일 관리 CIDR"
}
variable "domain" { type = string }
variable "vault_secret_ocid" {
  type      = string
  sensitive = true
}
variable "vault_compartment_id" {
  type      = string
  sensitive = true
}
variable "backup_retention_days" {
  type    = number
  default = 14
}
