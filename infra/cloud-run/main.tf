variable "project_id" { type = string }
variable "region" {
  type    = string
  default = "asia-northeast3"
}
variable "image" { type = string }
variable "revision" { type = string }

resource "google_cloud_run_v2_service" "production" {
  name                 = "dacare"
  location             = var.region
  deletion_protection  = true
  ingress              = "INGRESS_TRAFFIC_ALL"
  invoker_iam_disabled = true

  scaling {
    min_instance_count = 0
    max_instance_count = 1
  }

  template {
    service_account                  = "dacare-runtime@${var.project_id}.iam.gserviceaccount.com"
    timeout                          = "300s"
    max_instance_request_concurrency = 20

    containers {
      image = var.image
      ports { container_port = 8080 }
      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        cpu_idle          = true
        startup_cpu_boost = true
      }
      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      env {
        name  = "APP_REVISION"
        value = var.revision
      }
      env {
        name = "SPRING_APPLICATION_JSON"
        value_source {
          secret_key_ref {
            secret  = "dacare-app-config"
            version = "latest"
          }
        }
      }
      startup_probe {
        initial_delay_seconds = 0
        period_seconds        = 10
        timeout_seconds       = 5
        failure_threshold     = 24
        http_get {
          path = "/actuator/health/readiness"
          port = 8080
        }
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  lifecycle { prevent_destroy = true }
}

output "url" {
  value = google_cloud_run_v2_service.production.uri
}
