variable "cluster_name" {
  type = string
}

variable "region" {
  type = string
}

output "cluster_name" {
  value = var.cluster_name
}
