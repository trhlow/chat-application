module "eks" {
  source       = "../../modules/eks"
  cluster_name = "inchat-prod"
  region       = "ap-southeast-1"
}

module "rds" {
  source     = "../../modules/rds"
  identifier = "inchat-prod"
}

module "redis" {
  source     = "../../modules/redis"
  cluster_id = "inchat-prod"
}
