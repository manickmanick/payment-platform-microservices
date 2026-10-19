#!/usr/bin/env bash
set -euo pipefail

: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"
: "${AWS_REGION:?Set AWS_REGION}"
: "${EKS_CLUSTER_NAME:?Set EKS_CLUSTER_NAME}"
: "${VITE_STRIPE_PUBLISHABLE_KEY:=}"

registry="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
services=(user-service catalog-service delivery-service mail-service order-service payment-service api-gateway food-delivery-frontend)
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$registry"

for service in "${services[@]}"; do
  aws ecr describe-repositories --repository-names "food-delivery/$service" --region "$AWS_REGION" >/dev/null 2>&1 || \
    aws ecr create-repository --repository-name "food-delivery/$service" --image-scanning-configuration scanOnPush=true --region "$AWS_REGION" >/dev/null
  tag="$registry/food-delivery/$service:latest"
  if [[ "$service" == order-service ]]; then
    docker build -f order-service/Dockerfile -t "$tag" .
  elif [[ "$service" == food-delivery-frontend ]]; then
    docker build -f food-delivery-frontend/Dockerfile --build-arg VITE_API_BASE_URL=/api/v1 --build-arg VITE_STRIPE_PUBLISHABLE_KEY="$VITE_STRIPE_PUBLISHABLE_KEY" -t "$tag" food-delivery-frontend
  else
    docker build -t "$tag" "$service"
  fi
  docker push "$tag"
done

aws eks update-kubeconfig --name "$EKS_CLUSTER_NAME" --region "$AWS_REGION"
sed "s|YOUR_ECR_URI|$registry/food-delivery|g" deploy/k8s/apps.yaml | kubectl apply -f -
kubectl rollout status deployment/api-gateway -n food-delivery --timeout=5m
kubectl rollout status deployment/food-delivery-frontend -n food-delivery --timeout=5m
kubectl rollout status deployment/user-service -n food-delivery --timeout=5m
kubectl rollout status deployment/catalog-service -n food-delivery --timeout=5m
kubectl rollout status deployment/order-service -n food-delivery --timeout=5m
kubectl rollout status deployment/payment-service -n food-delivery --timeout=5m
kubectl rollout status deployment/delivery-service -n food-delivery --timeout=5m
kubectl rollout status deployment/mail-service -n food-delivery --timeout=5m
