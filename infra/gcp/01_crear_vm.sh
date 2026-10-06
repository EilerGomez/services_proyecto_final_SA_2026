#!/bin/bash
# =====================================================================
# 01_crear_vm.sh · Crea la VM de producción en Google Cloud (Compute Engine)
# Se ejecuta UNA vez desde su máquina, con gcloud instalado y autenticado:
#   gcloud auth login
#   gcloud config set project <ID_DEL_PROYECTO>
#   bash infra/gcp/01_crear_vm.sh
# =====================================================================
set -euo pipefail

VM=${VM:-proyecto2-vm}
ZONA=${ZONA:-us-central1-a}
REGION=${ZONA%-*}
# e2-standard-2 (2 vCPU, 8 GB): suficiente para el entorno de producción.
TIPO=${TIPO:-e2-standard-2}
DISCO=${DISCO:-30GB}

echo "Habilitando la API de Compute Engine..."
gcloud services enable compute.googleapis.com

echo "Reservando IP externa fija (no cambia aunque la VM se reinicie)..."
gcloud compute addresses describe proyecto2-ip --region="$REGION" >/dev/null 2>&1 \
  || gcloud compute addresses create proyecto2-ip --region="$REGION"
IP=$(gcloud compute addresses describe proyecto2-ip --region="$REGION" --format='value(address)')

echo "Creando la VM $VM ($TIPO, Ubuntu 24.04) con IP $IP..."
gcloud compute instances describe "$VM" --zone="$ZONA" >/dev/null 2>&1 || \
gcloud compute instances create "$VM" \
  --zone="$ZONA" \
  --machine-type="$TIPO" \
  --image-family=ubuntu-2404-lts-amd64 \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size="$DISCO" \
  --boot-disk-type=pd-balanced \
  --address="$IP" \
  --tags=proyecto2

echo "Abriendo solo los puertos públicos (gateway 8080, frontend 80/443)..."
gcloud compute firewall-rules describe proyecto2-publico >/dev/null 2>&1 || \
gcloud compute firewall-rules create proyecto2-publico \
  --network=default \
  --direction=INGRESS \
  --allow=tcp:80,tcp:443,tcp:8080 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=proyecto2

# SSH (22) ya lo permite la regla por defecto "default-allow-ssh".
# PostgreSQL (5432) y RabbitMQ (5672/15672) NO se abren: solo se usan dentro de la VM.

echo
echo "Listo. IP de la VM: $IP"
echo "Entrar:            gcloud compute ssh $VM --zone=$ZONA"
echo "Siguiente paso:    copiar infra/servidor/ a la VM y ejecutar 02_preparar_servidor.sh"
