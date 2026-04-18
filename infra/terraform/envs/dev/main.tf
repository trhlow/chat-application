module "eks" {
  source       = "../../modules/eks"
  cluster_name = "inchat-dev"
  region       = "ap-southeast-1"
}

module "rds" {
  source     = "../../modules/rds"
  identifier = "inchat-dev"
}

module "redis" {
  source     = "../../modules/redis"
  cluster_id = "inchat-dev"
}
