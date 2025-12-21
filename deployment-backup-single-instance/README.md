# Single Instance Deployment (BACKUP)

This folder contains the original single-instance deployment configuration where all services ran on one AWS Lightsail instance.

## Files
- `docker-compose-original.yml` - Original docker-compose with all services
- `.env-original` - Original environment configuration

## Why Moved to Backup
- Single 2GB instance couldn't handle all services efficiently
- Moved to distributed architecture with separate instances per service
- See `deployment-distributed/` folder for new architecture

## Date Backed Up
December 15, 2025
