#!/bin/bash

installPath=$1
echo ${installPath}

echo "Installing Archlinux VM..."
virt-install \
	--name atlassian \
    --description "Atlassian Tool VM (Jira, Confluence, Stash), based on Archlinux" \
	--virt-type kvm \
	--ram 1024 \
	--vcpus 2 \
	--network network=ovs-net \
	--os-variant virtio26 \
	--os-type linux \
	--disk path="${installPath}",size=30,device=cdrom \
	--boot cdrom,hd,network \
	--vnc \
	--vnclisten=0.0.0.0
